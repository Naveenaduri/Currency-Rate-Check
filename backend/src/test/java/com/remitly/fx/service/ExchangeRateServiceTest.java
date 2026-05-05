package com.remitly.fx.service;

import com.remitly.fx.model.CurrencyPair;
import com.remitly.fx.model.RateComparison;
import com.remitly.fx.model.RateQuote;
import com.remitly.fx.provider.ExchangeRateProvider;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExchangeRateServiceTest {

    private static class FakeProvider implements ExchangeRateProvider {
        private final String name;
        private final Map<CurrencyPair, BigDecimal> rates;
        FakeProvider(String name, Map<CurrencyPair, BigDecimal> rates) {
            this.name = name;
            this.rates = rates;
        }
        @Override public String getName() { return name; }
        @Override public Map<CurrencyPair, BigDecimal> fetchRates() {
            return new HashMap<>(rates);
        }
    }

    private ExchangeRateService serviceWith(ExchangeRateProvider... providers) {
        RateAggregator aggregator = new RateAggregator(List.of(providers));
        aggregator.init();
        return new ExchangeRateService(aggregator);
    }

    @Test
    void getBestRateDelegatesToAggregator() {
        ExchangeRateService service = serviceWith(
                new FakeProvider("A", Map.of(new CurrencyPair("USD", "EUR"), new BigDecimal("0.91"))),
                new FakeProvider("B", Map.of(new CurrencyPair("USD", "EUR"), new BigDecimal("0.93")))
        );

        RateQuote best = service.getBestRate("USD", "EUR");

        assertThat(best.provider()).isEqualTo("B");
        assertThat(best.rate()).isEqualByComparingTo("0.93");
    }

    @Test
    void getBestRateThrows404WhenMissing() {
        ExchangeRateService service = serviceWith(
                new FakeProvider("A", Map.of(new CurrencyPair("USD", "EUR"), new BigDecimal("0.91")))
        );

        assertThatThrownBy(() -> service.getBestRate("USD", "JPY"))
                .isInstanceOf(RateNotFoundException.class)
                .hasMessageContaining("USD -> JPY");
    }

    @Test
    void compareReturnsAllProviderQuotes() {
        ExchangeRateService service = serviceWith(
                new FakeProvider("A", Map.of(new CurrencyPair("USD", "EUR"), new BigDecimal("0.90"))),
                new FakeProvider("B", Map.of(new CurrencyPair("USD", "EUR"), new BigDecimal("0.93")))
        );

        RateComparison comparison = service.compare("USD", "EUR");

        assertThat(comparison.quotes()).hasSize(2);
        assertThat(comparison.best().provider()).isEqualTo("B");
    }

    @Test
    void compareThrowsWhenNoQuotes() {
        ExchangeRateService service = serviceWith(
                new FakeProvider("A", Map.of())
        );

        assertThatThrownBy(() -> service.compare("USD", "EUR"))
                .isInstanceOf(RateNotFoundException.class);
    }

    @Test
    void listProvidersAndCurrenciesAndBestRatesAndRefresh() {
        ExchangeRateService service = serviceWith(
                new FakeProvider("A", Map.of(
                        new CurrencyPair("USD", "EUR"), new BigDecimal("0.91"),
                        new CurrencyPair("USD", "GBP"), new BigDecimal("0.79")
                )),
                new FakeProvider("B", Map.of(
                        new CurrencyPair("USD", "EUR"), new BigDecimal("0.93")
                ))
        );

        assertThat(service.listProviders()).containsExactly("A", "B");
        assertThat(service.listCurrencies()).containsExactly("EUR", "GBP", "USD");
        assertThat(service.listBestRates()).extracting("from", "to", "provider")
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple("USD", "EUR", "B"),
                        org.assertj.core.groups.Tuple.tuple("USD", "GBP", "A"));

        service.refreshNow();
        assertThat(service.listBestRates()).hasSize(2);
    }
}
