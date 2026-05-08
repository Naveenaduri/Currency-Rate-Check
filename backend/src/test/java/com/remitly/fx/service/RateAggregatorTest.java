package com.remitly.fx.service;

import com.remitly.fx.model.ProviderQuote;
import com.remitly.fx.model.QuoteResponse;
import com.remitly.fx.wise.WiseComparisonClient;
import com.remitly.fx.wise.WiseComparisonResponse;
import com.remitly.fx.wise.WiseComparisonResponse.DeliveryEstimation;
import com.remitly.fx.wise.WiseComparisonResponse.Provider;
import com.remitly.fx.wise.WiseComparisonResponse.Quote;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RateAggregatorTest {

    private static class StubClient extends WiseComparisonClient {
        WiseComparisonResponse next;
        RuntimeException toThrow;
        final AtomicInteger calls = new AtomicInteger();
        StubClient() { super("https://example", RestClient.create()); }
        @Override
        public WiseComparisonResponse fetchComparison(String src, String dst, BigDecimal amt) {
            calls.incrementAndGet();
            if (toThrow != null) throw toThrow;
            return next;
        }
    }

    private static Provider provider(String alias, String name, String type,
                                     String rate, String fee) {
        Quote q = new Quote(
                new BigDecimal(rate),
                new BigDecimal(fee),
                BigDecimal.ZERO,
                new BigDecimal(rate).multiply(new BigDecimal("1000")),
                Instant.parse("2026-05-08T00:00:00Z"),
                "US", "IN",
                new DeliveryEstimation(null, null, null, true));
        return new Provider(1, alias, name, "https://logos/" + alias + ".svg",
                type, false, List.of(q));
    }

    private RateAggregator aggregatorWith(StubClient client) {
        return new RateAggregator(client, new BigDecimal("1000"), 300);
    }

    @Test
    void quoteRecomputesReceiveAmountFromCachedRate() {
        StubClient client = new StubClient();
        client.next = new WiseComparisonResponse(List.of(
                provider("remitly", "Remitly", "moneyTransferProvider", "94.30", "0"),
                provider("wise", "Wise", "moneyTransferProvider", "94.50", "2"),
                provider("chase", "Chase", "bank", "92.00", "10")
        ));
        RateAggregator aggregator = aggregatorWith(client);

        QuoteResponse response = aggregator.quote("USD", "INR", new BigDecimal("500"));

        assertThat(response.providers()).hasSize(3);
        ProviderQuote best = response.providers().get(0);
        // At a $500 send, Remitly (rate 94.30, fee 0) edges out Wise (94.50, fee 2)
        // because the flat fee dominates the rate gap on small amounts:
        //   Remitly = (500 - 0) * 94.30 = 47150.00
        //   Wise    = (500 - 2) * 94.50 = 47061.00
        assertThat(best.id()).isEqualTo("remitly");
        assertThat(best.bestDeal()).isTrue();
        assertThat(best.receiveAmount()).isEqualByComparingTo("47150.00");
        assertThat(response.baselineName()).isEqualTo("Chase");
        assertThat(response.source()).contains("Wise");
    }

    @Test
    void cacheReusesPriorResponseWithinTtl() {
        StubClient client = new StubClient();
        client.next = new WiseComparisonResponse(List.of(
                provider("remitly", "Remitly", "moneyTransferProvider", "94.30", "0")
        ));
        RateAggregator aggregator = aggregatorWith(client);

        aggregator.quote("USD", "INR", new BigDecimal("1000"));
        aggregator.quote("USD", "INR", new BigDecimal("2000"));
        aggregator.quote("USD", "INR", new BigDecimal("500"));

        assertThat(client.calls.get()).isEqualTo(1);
    }

    @Test
    void invalidateForcesRefetch() {
        StubClient client = new StubClient();
        client.next = new WiseComparisonResponse(List.of(
                provider("remitly", "Remitly", "moneyTransferProvider", "94.30", "0")
        ));
        RateAggregator aggregator = aggregatorWith(client);

        aggregator.quote("USD", "INR", new BigDecimal("1000"));
        aggregator.invalidate("USD", "INR");
        aggregator.quote("USD", "INR", new BigDecimal("1000"));

        assertThat(client.calls.get()).isEqualTo(2);
    }

    @Test
    void rejectsNonPositiveAmount() {
        RateAggregator aggregator = aggregatorWith(new StubClient());

        assertThatThrownBy(() -> aggregator.quote("USD", "INR", BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> aggregator.quote("USD", "INR", null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void wiseFailureWithNoCacheThrowsRateNotFound() {
        StubClient client = new StubClient();
        client.toThrow = new RestClientException("network down");
        RateAggregator aggregator = aggregatorWith(client);

        assertThatThrownBy(() -> aggregator.quote("USD", "INR", new BigDecimal("100")))
                .isInstanceOf(RateNotFoundException.class);
    }

    @Test
    void wiseFailureWithCacheServesStale() {
        StubClient client = new StubClient();
        client.next = new WiseComparisonResponse(List.of(
                provider("remitly", "Remitly", "moneyTransferProvider", "94.30", "0")
        ));
        RateAggregator aggregator = aggregatorWith(client);
        aggregator.quote("USD", "INR", new BigDecimal("1000"));

        client.toThrow = new RestClientException("network down");
        aggregator.invalidate("USD", "INR");
        // The next call will try to refetch (and fail), but compute() returns the
        // current value when fresh; here we have no current after invalidate, so a
        // second invocation that finds a stale entry is what we want.
        // Re-prime cache, then go stale-by-zero-ttl path indirectly with a short TTL.
        RateAggregator shortTtl = new RateAggregator(client, new BigDecimal("1000"), 0);
        client.toThrow = null;
        shortTtl.quote("USD", "INR", new BigDecimal("1000"));
        client.toThrow = new RestClientException("down");
        QuoteResponse stale = shortTtl.quote("USD", "INR", new BigDecimal("1000"));

        assertThat(stale.providers()).isNotEmpty();
    }

    @Test
    void emptyResponseTriggers404() {
        StubClient client = new StubClient();
        client.next = new WiseComparisonResponse(List.of());
        RateAggregator aggregator = aggregatorWith(client);

        assertThatThrownBy(() -> aggregator.quote("USD", "XYZ", new BigDecimal("100")))
                .isInstanceOf(RateNotFoundException.class);
    }

    @Test
    void knownProviderNamesAccumulatesAcrossPairs() {
        StubClient client = new StubClient();
        client.next = new WiseComparisonResponse(List.of(
                provider("remitly", "Remitly", "moneyTransferProvider", "94.30", "0"),
                provider("wise", "Wise", "moneyTransferProvider", "94.50", "2")
        ));
        RateAggregator aggregator = aggregatorWith(client);
        aggregator.quote("USD", "INR", new BigDecimal("1000"));

        assertThat(aggregator.knownProviderNames()).containsExactlyInAnyOrder("Remitly", "Wise");
    }

    @Test
    void baselineFallsBackToWorstReceiveWhenNoBank() {
        StubClient client = new StubClient();
        client.next = new WiseComparisonResponse(List.of(
                provider("wise", "Wise", "moneyTransferProvider", "94.50", "2"),
                provider("remitly", "Remitly", "moneyTransferProvider", "94.30", "0")
        ));
        RateAggregator aggregator = aggregatorWith(client);

        QuoteResponse response = aggregator.quote("USD", "INR", new BigDecimal("1000"));

        // Without a bank in the result, the worst-received provider becomes the baseline.
        assertThat(response.baselineName()).isEqualTo("Remitly");
    }
}
