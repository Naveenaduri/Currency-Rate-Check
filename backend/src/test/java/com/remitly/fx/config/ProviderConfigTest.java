package com.remitly.fx.config;

import com.remitly.fx.model.CurrencyPair;
import com.remitly.fx.provider.ExchangeRateProvider;
import com.remitly.fx.provider.HttpExchangeRateProvider;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ProviderConfigTest {

    @Test
    void simulatedProvidersExist() {
        ProviderConfig config = new ProviderConfig();

        List<ExchangeRateProvider> providers = List.of(
                config.remitlyProvider(),
                config.wiseProvider(),
                config.westernUnionProvider());

        assertThat(providers).extracting(ExchangeRateProvider::getName)
                .containsExactly("Remitly", "Wise", "Western Union");
        providers.forEach(p -> assertThat(p.fetchRates()).isNotEmpty());
    }

    @Test
    void defaultBaseRatesIncludeMajorPairs() {
        Map<CurrencyPair, BigDecimal> base = ProviderConfig.defaultBaseRates();
        assertThat(base).containsKeys(
                new CurrencyPair("USD", "EUR"),
                new CurrencyPair("EUR", "USD"),
                new CurrencyPair("USD", "GBP"));
    }

    @Test
    void frankfurterProviderUsesProvidedConfig() {
        ProviderConfig config = new ProviderConfig();

        ExchangeRateProvider frankfurter = config.frankfurterProvider(
                "https://example.test", List.of("USD", "EUR"));

        assertThat(frankfurter).isInstanceOf(HttpExchangeRateProvider.class);
        assertThat(frankfurter.getName()).isEqualTo("Frankfurter");
    }
}
