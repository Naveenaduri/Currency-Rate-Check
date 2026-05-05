package com.remitly.fx.provider;

import com.remitly.fx.model.CurrencyPair;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

class SimulatedProviderTest {

    private static final Map<CurrencyPair, BigDecimal> BASE = Map.of(
            new CurrencyPair("USD", "EUR"), new BigDecimal("0.92"),
            new CurrencyPair("EUR", "USD"), new BigDecimal("1.087")
    );

    @Test
    void exposesProviderName() {
        SimulatedProvider provider = new SimulatedProvider("Acme", BASE, 0.0, new Random(1));
        assertThat(provider.getName()).isEqualTo("Acme");
    }

    @Test
    void fetchRatesReturnsAllPairs() {
        SimulatedProvider provider = new SimulatedProvider("Acme", BASE, 0.0, new Random(42));

        Map<CurrencyPair, BigDecimal> snapshot = provider.fetchRates();

        assertThat(snapshot).hasSize(BASE.size());
        assertThat(snapshot.keySet()).isEqualTo(BASE.keySet());
        snapshot.values().forEach(v -> assertThat(v.signum()).isPositive());
    }

    @Test
    void positiveSpreadInflatesRates() {
        SimulatedProvider zero = new SimulatedProvider("Z", BASE, 0.0, new Random(0));
        SimulatedProvider plus = new SimulatedProvider("P", BASE, 0.05, new Random(0));

        BigDecimal zeroRate = zero.fetchRates().get(new CurrencyPair("USD", "EUR"));
        BigDecimal plusRate = plus.fetchRates().get(new CurrencyPair("USD", "EUR"));

        assertThat(plusRate).isGreaterThan(zeroRate);
    }

    @Test
    void successiveCallsProduceVaryingRates() {
        SimulatedProvider provider = new SimulatedProvider("Acme", BASE, 0.0, new Random(7));

        BigDecimal first = provider.fetchRates().get(new CurrencyPair("USD", "EUR"));
        BigDecimal second = provider.fetchRates().get(new CurrencyPair("USD", "EUR"));

        assertThat(first).isNotEqualByComparingTo(second);
    }
}
