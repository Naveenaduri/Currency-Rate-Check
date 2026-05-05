package com.remitly.fx.service;

import com.remitly.fx.model.CurrencyPair;
import com.remitly.fx.model.Quote;
import com.remitly.fx.model.RateComparison;
import com.remitly.fx.model.RateQuote;
import com.remitly.fx.provider.ExchangeRateProvider;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Polls every registered {@link ExchangeRateProvider} on a schedule, caches
 * the latest snapshot per provider, and answers comparison queries.
 *
 * <p>"Best" for a pair {@code from -> to} is defined as the largest rate
 * (i.e. customer receives the most {@code to} per unit of {@code from}).
 * If a provider's poll fails, its previously cached snapshot stays in place
 * so the system degrades gracefully.</p>
 */
@Service
public class RateAggregator {

    private static final Logger log = LoggerFactory.getLogger(RateAggregator.class);

    private final List<ExchangeRateProvider> providers;
    private final Map<String, Map<CurrencyPair, BigDecimal>> ratesByProvider =
            new ConcurrentHashMap<>();
    private volatile Instant lastRefreshAt;

    public RateAggregator(List<ExchangeRateProvider> providers) {
        this.providers = List.copyOf(providers);
    }

    @PostConstruct
    public void init() {
        refresh();
    }

    @Scheduled(fixedRateString = "${fx.poll.interval-ms:30000}")
    public void refresh() {
        for (ExchangeRateProvider provider : providers) {
            try {
                Map<CurrencyPair, BigDecimal> snapshot = provider.fetchRates();
                ratesByProvider.put(provider.getName(), Map.copyOf(snapshot));
            } catch (RuntimeException ex) {
                log.warn("Provider {} poll failed: {}", provider.getName(), ex.getMessage());
            }
        }
        lastRefreshAt = Instant.now();
    }

    public List<String> providerNames() {
        return providers.stream().map(ExchangeRateProvider::getName).toList();
    }

    public Instant lastRefreshAt() {
        return lastRefreshAt;
    }

    public Set<String> listCurrencies() {
        Set<String> result = new TreeSet<>();
        for (Map<CurrencyPair, BigDecimal> snapshot : ratesByProvider.values()) {
            for (CurrencyPair pair : snapshot.keySet()) {
                result.add(pair.from());
                result.add(pair.to());
            }
        }
        return result;
    }

    public Optional<RateQuote> bestRate(String from, String to) {
        CurrencyPair pair = new CurrencyPair(from, to);
        return ratesByProvider.entrySet().stream()
                .filter(entry -> entry.getValue().containsKey(pair))
                .map(entry -> new RateQuote(
                        pair.from(), pair.to(), entry.getValue().get(pair), entry.getKey()))
                .max(Comparator.comparing(RateQuote::rate));
    }

    public RateComparison compare(String from, String to) {
        CurrencyPair pair = new CurrencyPair(from, to);
        List<Quote> quotes = ratesByProvider.entrySet().stream()
                .filter(entry -> entry.getValue().containsKey(pair))
                .map(entry -> new Quote(entry.getKey(), entry.getValue().get(pair)))
                .sorted(Comparator.comparing(Quote::rate).reversed())
                .toList();
        Quote best = quotes.isEmpty() ? null : quotes.get(0);
        return new RateComparison(pair.from(), pair.to(), best, quotes);
    }

    public List<RateQuote> allBestRates() {
        Set<CurrencyPair> allPairs = new HashSet<>();
        for (Map<CurrencyPair, BigDecimal> snapshot : ratesByProvider.values()) {
            allPairs.addAll(snapshot.keySet());
        }
        return allPairs.stream()
                .flatMap(pair -> bestRate(pair.from(), pair.to()).stream())
                .sorted(Comparator.comparing(RateQuote::from).thenComparing(RateQuote::to))
                .toList();
    }

    public Map<String, List<RateQuote>> snapshotByProvider() {
        return ratesByProvider.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().entrySet().stream()
                                .map(p -> new RateQuote(
                                        p.getKey().from(),
                                        p.getKey().to(),
                                        p.getValue(),
                                        e.getKey()))
                                .sorted(Comparator
                                        .comparing(RateQuote::from)
                                        .thenComparing(RateQuote::to))
                                .toList()));
    }

    /** For tests: drop all cached data. */
    void clearCacheForTest() {
        ratesByProvider.clear();
        lastRefreshAt = null;
    }

    /** For tests / inspection: stream over the cached snapshots. */
    Stream<Map.Entry<String, Map<CurrencyPair, BigDecimal>>> snapshotEntries() {
        return ratesByProvider.entrySet().stream();
    }
}
