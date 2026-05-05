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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class RateAggregatorTest {

    private static class FakeProvider implements ExchangeRateProvider {
        private final String name;
        private Map<CurrencyPair, BigDecimal> rates;
        private boolean fail = false;
        private int callCount = 0;

        FakeProvider(String name, Map<CurrencyPair, BigDecimal> rates) {
            this.name = name;
            this.rates = new HashMap<>(rates);
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public Map<CurrencyPair, BigDecimal> fetchRates() {
            callCount++;
            if (fail) throw new RuntimeException("upstream down");
            return new HashMap<>(rates);
        }
    }

    private RateAggregator aggregatorWith(ExchangeRateProvider... providers) {
        RateAggregator aggregator = new RateAggregator(List.of(providers));
        aggregator.init();
        return aggregator;
    }

    @Test
    void initPollsEveryProviderAndStampsRefreshTime() {
        FakeProvider a = new FakeProvider("A",
                Map.of(new CurrencyPair("USD", "EUR"), new BigDecimal("0.92")));
        FakeProvider b = new FakeProvider("B",
                Map.of(new CurrencyPair("USD", "EUR"), new BigDecimal("0.93")));

        RateAggregator aggregator = aggregatorWith(a, b);

        assertThat(aggregator.providerNames()).containsExactly("A", "B");
        assertThat(a.callCount).isEqualTo(1);
        assertThat(b.callCount).isEqualTo(1);
        assertThat(aggregator.lastRefreshAt()).isNotNull();
    }

    @Test
    void bestRatePicksHighest() {
        FakeProvider a = new FakeProvider("A",
                Map.of(new CurrencyPair("USD", "EUR"), new BigDecimal("0.90")));
        FakeProvider b = new FakeProvider("B",
                Map.of(new CurrencyPair("USD", "EUR"), new BigDecimal("0.93")));
        FakeProvider c = new FakeProvider("C",
                Map.of(new CurrencyPair("USD", "EUR"), new BigDecimal("0.91")));

        RateAggregator aggregator = aggregatorWith(a, b, c);

        Optional<RateQuote> best = aggregator.bestRate("usd", "eur");

        assertThat(best).isPresent();
        assertThat(best.get().provider()).isEqualTo("B");
        assertThat(best.get().rate()).isEqualByComparingTo("0.93");
    }

    @Test
    void bestRateEmptyWhenNoProviderQuotesPair() {
        FakeProvider only = new FakeProvider("Only",
                Map.of(new CurrencyPair("USD", "EUR"), new BigDecimal("0.92")));

        RateAggregator aggregator = aggregatorWith(only);

        assertThat(aggregator.bestRate("USD", "JPY")).isEmpty();
    }

    @Test
    void compareSortsQuotesDescByRateAndExposesBest() {
        FakeProvider a = new FakeProvider("A",
                Map.of(new CurrencyPair("USD", "EUR"), new BigDecimal("0.90")));
        FakeProvider b = new FakeProvider("B",
                Map.of(new CurrencyPair("USD", "EUR"), new BigDecimal("0.93")));
        FakeProvider c = new FakeProvider("C", Map.of());

        RateAggregator aggregator = aggregatorWith(a, b, c);

        RateComparison comp = aggregator.compare("USD", "EUR");

        assertThat(comp.from()).isEqualTo("USD");
        assertThat(comp.quotes()).extracting("provider").containsExactly("B", "A");
        assertThat(comp.best().provider()).isEqualTo("B");
    }

    @Test
    void allBestRatesUnionsPairsAcrossProviders() {
        FakeProvider a = new FakeProvider("A", Map.of(
                new CurrencyPair("USD", "EUR"), new BigDecimal("0.92"),
                new CurrencyPair("USD", "GBP"), new BigDecimal("0.79")
        ));
        FakeProvider b = new FakeProvider("B", Map.of(
                new CurrencyPair("USD", "EUR"), new BigDecimal("0.93"),
                new CurrencyPair("USD", "JPY"), new BigDecimal("156.0")
        ));

        RateAggregator aggregator = aggregatorWith(a, b);

        List<RateQuote> all = aggregator.allBestRates();

        assertThat(all).hasSize(3);
        assertThat(all)
                .filteredOn(q -> q.from().equals("USD") && q.to().equals("EUR"))
                .first()
                .extracting(RateQuote::provider)
                .isEqualTo("B");
    }

    @Test
    void listCurrenciesUnionsAcrossProviders() {
        FakeProvider a = new FakeProvider("A", Map.of(
                new CurrencyPair("USD", "EUR"), new BigDecimal("0.92")
        ));
        FakeProvider b = new FakeProvider("B", Map.of(
                new CurrencyPair("GBP", "JPY"), new BigDecimal("198.5")
        ));

        RateAggregator aggregator = aggregatorWith(a, b);

        assertThat(aggregator.listCurrencies())
                .containsExactly("EUR", "GBP", "JPY", "USD");
    }

    @Test
    void providerFailureKeepsLastSnapshot() {
        FakeProvider a = new FakeProvider("A",
                Map.of(new CurrencyPair("USD", "EUR"), new BigDecimal("0.92")));

        RateAggregator aggregator = aggregatorWith(a);
        assertThat(aggregator.bestRate("USD", "EUR")).isPresent();

        a.fail = true;
        aggregator.refresh();

        assertThat(aggregator.bestRate("USD", "EUR"))
                .isPresent()
                .get()
                .extracting(RateQuote::rate)
                .isEqualTo(new BigDecimal("0.92"));
    }

    @Test
    void snapshotByProviderReturnsSortedQuotesPerProvider() {
        FakeProvider a = new FakeProvider("A", Map.of(
                new CurrencyPair("USD", "EUR"), new BigDecimal("0.92"),
                new CurrencyPair("USD", "GBP"), new BigDecimal("0.79")
        ));

        RateAggregator aggregator = aggregatorWith(a);

        Map<String, List<RateQuote>> byProvider = aggregator.snapshotByProvider();

        assertThat(byProvider).containsOnlyKeys("A");
        assertThat(byProvider.get("A")).extracting(RateQuote::to).containsExactly("EUR", "GBP");
    }

    @Test
    void clearCacheForTestEmptiesEverything() {
        FakeProvider a = new FakeProvider("A",
                Map.of(new CurrencyPair("USD", "EUR"), new BigDecimal("0.92")));
        RateAggregator aggregator = aggregatorWith(a);

        aggregator.clearCacheForTest();

        assertThat(aggregator.allBestRates()).isEmpty();
        assertThat(aggregator.lastRefreshAt()).isNull();
        assertThat(aggregator.snapshotEntries().count()).isZero();
    }
}
