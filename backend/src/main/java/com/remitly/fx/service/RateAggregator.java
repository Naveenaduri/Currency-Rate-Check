package com.remitly.fx.service;

import com.remitly.fx.model.CurrencyPair;
import com.remitly.fx.model.ProviderQuote;
import com.remitly.fx.model.QuoteResponse;
import com.remitly.fx.wise.WiseComparisonClient;
import com.remitly.fx.wise.WiseComparisonResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * TTL cache around {@link WiseComparisonClient} that builds the comparison
 * payload the UI consumes.
 *
 * <p>For every {@code (from, to)} corridor we hit Wise once with a fixed
 * sample {@code sendAmount} (default 1000), keep the per-provider rate + fee
 * in memory, and recompute receive amounts client-side when callers ask for
 * a specific amount. Cache entries refresh after {@code fx.cache.ttl} (default
 * 5 minutes) — well below Wise's data-collection cadence so rates stay
 * fresh.</p>
 */
@Service
public class RateAggregator {

    private static final Logger log = LoggerFactory.getLogger(RateAggregator.class);
    private static final int MONEY_SCALE = 2;
    private static final int RATE_SCALE = 6;

    /** Provider aliases that should be used as the comparison baseline (worst case). */
    private static final Set<String> BASELINE_TYPES = Set.of("bank");

    private final WiseComparisonClient wise;
    private final BigDecimal sampleAmount;
    private final Duration ttl;
    private final Map<CurrencyPair, CachedComparison> cache = new ConcurrentHashMap<>();

    public RateAggregator(
            WiseComparisonClient wise,
            @Value("${fx.cache.sample-amount:1000}") BigDecimal sampleAmount,
            @Value("${fx.cache.ttl-seconds:300}") long ttlSeconds) {
        this.wise = wise;
        this.sampleAmount = sampleAmount;
        this.ttl = Duration.ofSeconds(ttlSeconds);
    }

    /** Force the next quote to bypass the cache. */
    public void invalidate(String from, String to) {
        cache.remove(new CurrencyPair(from, to));
    }

    /** Names of every provider seen across cached corridors. */
    public Set<String> knownProviderNames() {
        Set<String> names = new LinkedHashSet<>();
        for (CachedComparison cached : cache.values()) {
            for (WiseComparisonResponse.Provider p : cached.response().providers()) {
                if (p.name() != null) names.add(p.name());
            }
        }
        return names;
    }

    public QuoteResponse quote(String from, String to, BigDecimal sendAmount) {
        if (sendAmount == null || sendAmount.signum() <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
        CurrencyPair pair = new CurrencyPair(from, to);

        CachedComparison cached = cache.compute(pair, (key, current) -> {
            if (current != null && !current.isStale(ttl)) {
                return current;
            }
            try {
                WiseComparisonResponse fresh = wise.fetchComparison(
                        key.from(), key.to(), sampleAmount);
                return new CachedComparison(fresh, Instant.now());
            } catch (RestClientException ex) {
                Throwable root = ex;
                while (root.getCause() != null && root.getCause() != root) root = root.getCause();
                log.warn("Wise fetch failed for {} -> {}: {} (root: {})",
                        key.from(), key.to(), ex.getMessage(), root.toString());
                if (current != null) return current; // serve stale on transient failures
                throw new RateNotFoundException(
                        "Wise has no comparison data for " + key.from() + " -> " + key.to());
            }
        });

        List<ProviderQuote> quotes = buildQuotes(cached, sendAmount);
        if (quotes.isEmpty()) {
            throw new RateNotFoundException(
                    "Wise has no comparison data for " + pair.from() + " -> " + pair.to());
        }

        BigDecimal bestReceive = quotes.get(0).receiveAmount();
        String baselineName = quotes.stream()
                .filter(ProviderQuote::baseline)
                .map(ProviderQuote::name)
                .findFirst()
                .orElse("Standard rate");

        return new QuoteResponse(
                pair.from(),
                pair.to(),
                sendAmount.setScale(MONEY_SCALE, RoundingMode.HALF_UP),
                bestReceive,
                baselineName,
                cached.fetchedAt(),
                "Wise V3 Comparisons API",
                quotes);
    }

    private List<ProviderQuote> buildQuotes(CachedComparison cached, BigDecimal sendAmount) {
        record Row(WiseComparisonResponse.Provider provider,
                   WiseComparisonResponse.Quote quote,
                   BigDecimal receive) {}

        List<Row> rows = new ArrayList<>();
        for (WiseComparisonResponse.Provider provider : cached.response().providers()) {
            if (provider.quotes() == null || provider.quotes().isEmpty()) continue;
            WiseComparisonResponse.Quote q = provider.quotes().get(0);
            if (q.rate() == null) continue;
            BigDecimal fee = q.fee() == null ? BigDecimal.ZERO : q.fee();
            // receivedAmount = (sendAmount - fee) * rate matches Wise's reporting
            BigDecimal receive = sendAmount
                    .subtract(fee)
                    .multiply(q.rate())
                    .setScale(MONEY_SCALE, RoundingMode.HALF_UP);
            if (receive.signum() <= 0) continue;
            rows.add(new Row(provider, q, receive));
        }
        if (rows.isEmpty()) return List.of();

        rows.sort(Comparator.comparing(Row::receive).reversed());
        BigDecimal bestReceive = rows.get(0).receive();

        // Pick the lowest-receive bank as the "Standard rate" baseline; fall back to the
        // worst-received provider if none of the providers are banks.
        BigDecimal baselineReceive = rows.stream()
                .filter(r -> r.provider().type() != null
                        && BASELINE_TYPES.contains(r.provider().type()))
                .map(Row::receive)
                .min(Comparator.naturalOrder())
                .orElseGet(() -> rows.get(rows.size() - 1).receive());
        Row baselineRow = rows.stream()
                .filter(r -> r.receive().compareTo(baselineReceive) == 0)
                .findFirst()
                .orElse(rows.get(rows.size() - 1));

        List<ProviderQuote> result = new ArrayList<>(rows.size());
        for (Row row : rows) {
            BigDecimal savings = row.receive().subtract(baselineReceive)
                    .setScale(MONEY_SCALE, RoundingMode.HALF_UP);
            WiseComparisonResponse.DeliveryEstimation delivery = row.quote().deliveryEstimation();
            result.add(new ProviderQuote(
                    row.provider().alias(),
                    row.provider().name(),
                    row.provider().logo(),
                    row.provider().type(),
                    row.quote().rate().setScale(RATE_SCALE, RoundingMode.HALF_UP),
                    row.quote().fee() == null
                            ? BigDecimal.ZERO.setScale(MONEY_SCALE)
                            : row.quote().fee().setScale(MONEY_SCALE, RoundingMode.HALF_UP),
                    row.quote().markup(),
                    row.receive(),
                    savings,
                    delivery == null ? null : delivery.durationAsBigDecimal(),
                    delivery == null ? null : delivery.durationType(),
                    row.quote().dateCollected() == null
                            ? null
                            : row.quote().dateCollected().toString(),
                    row.receive().compareTo(bestReceive) == 0,
                    row == baselineRow));
        }
        return result;
    }

    private record CachedComparison(WiseComparisonResponse response, Instant fetchedAt) {
        boolean isStale(Duration ttl) {
            return Instant.now().isAfter(fetchedAt.plus(ttl));
        }
    }
}
