package com.remitly.fx.service;

import com.remitly.fx.model.QuoteResponse;
import com.remitly.fx.wise.WiseComparisonClient;
import com.remitly.fx.wise.WiseComparisonResponse;
import com.remitly.fx.wise.WiseComparisonResponse.DeliveryEstimation;
import com.remitly.fx.wise.WiseComparisonResponse.Provider;
import com.remitly.fx.wise.WiseComparisonResponse.Quote;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class ExchangeRateServiceTest {

    private static class StubClient extends WiseComparisonClient {
        WiseComparisonResponse next;
        final AtomicInteger calls = new AtomicInteger();
        StubClient() { super("https://example", RestClient.create()); }
        @Override
        public WiseComparisonResponse fetchComparison(String src, String dst, BigDecimal amt) {
            calls.incrementAndGet();
            return next;
        }
    }

    private static Provider provider(String alias, String name, String type) {
        Quote q = new Quote(
                new BigDecimal("94.30"),
                new BigDecimal("2"),
                BigDecimal.ZERO,
                new BigDecimal("94300"),
                Instant.parse("2026-05-08T00:00:00Z"),
                "US", "IN",
                new DeliveryEstimation(null, null, null, true));
        return new Provider(1, alias, name, "https://logos/" + alias + ".svg",
                type, false, List.of(q));
    }

    private ExchangeRateService serviceWith(StubClient client) {
        RateAggregator aggregator = new RateAggregator(client, new BigDecimal("1000"), 300);
        return new ExchangeRateService(aggregator,
                new String[]{"USD", "EUR", "GBP", "INR"});
    }

    @Test
    void listCurrenciesReturnsConfiguredSet() {
        ExchangeRateService service = serviceWith(new StubClient());
        assertThat(service.listCurrencies()).containsExactly("EUR", "GBP", "INR", "USD");
    }

    @Test
    void quoteDelegatesToAggregator() {
        StubClient client = new StubClient();
        client.next = new WiseComparisonResponse(List.of(
                provider("wise", "Wise", "moneyTransferProvider")));
        ExchangeRateService service = serviceWith(client);

        QuoteResponse response = service.quote("USD", "INR", new BigDecimal("1000"));

        assertThat(response.providers()).hasSize(1);
        assertThat(response.providers().get(0).id()).isEqualTo("wise");
    }

    @Test
    void refreshInvalidatesCache() {
        StubClient client = new StubClient();
        client.next = new WiseComparisonResponse(List.of(
                provider("wise", "Wise", "moneyTransferProvider")));
        ExchangeRateService service = serviceWith(client);

        service.quote("USD", "INR", new BigDecimal("1000"));
        service.refresh("USD", "INR");
        service.quote("USD", "INR", new BigDecimal("1000"));

        assertThat(client.calls.get()).isEqualTo(2);
    }

    @Test
    void listProvidersDerivesFromAggregator() {
        StubClient client = new StubClient();
        client.next = new WiseComparisonResponse(List.of(
                provider("wise", "Wise", "moneyTransferProvider"),
                provider("remitly", "Remitly", "moneyTransferProvider")));
        ExchangeRateService service = serviceWith(client);

        assertThat(service.listProviders()).isEmpty();
        service.quote("USD", "INR", new BigDecimal("1000"));
        assertThat(service.listProviders()).contains("Wise", "Remitly");
    }
}
