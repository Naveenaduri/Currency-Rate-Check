package com.remitly.fx.config;

import com.remitly.fx.model.CurrencyPair;
import com.remitly.fx.provider.ExchangeRateProvider;
import com.remitly.fx.provider.HttpExchangeRateProvider;
import com.remitly.fx.provider.SimulatedProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Wires the set of {@link ExchangeRateProvider} beans that
 * {@code RateAggregator} polls. By default we register three simulated
 * providers so the app runs without external dependencies. The Frankfurter
 * HTTP provider only activates when {@code fx.providers.frankfurter.enabled=true}.
 */
@Configuration
public class ProviderConfig {

    /**
     * Shared base rates the simulated providers vary. These are mid-market
     * approximations; each provider applies its own spread on top, so the
     * customer-facing rate differs by provider.
     */
    public static Map<CurrencyPair, BigDecimal> defaultBaseRates() {
        Map<CurrencyPair, BigDecimal> rates = new HashMap<>();
        rates.put(new CurrencyPair("USD", "EUR"), new BigDecimal("0.920000"));
        rates.put(new CurrencyPair("EUR", "USD"), new BigDecimal("1.087000"));
        rates.put(new CurrencyPair("USD", "GBP"), new BigDecimal("0.790000"));
        rates.put(new CurrencyPair("GBP", "USD"), new BigDecimal("1.266000"));
        rates.put(new CurrencyPair("USD", "JPY"), new BigDecimal("156.000000"));
        rates.put(new CurrencyPair("JPY", "USD"), new BigDecimal("0.006400"));
        rates.put(new CurrencyPair("USD", "INR"), new BigDecimal("83.500000"));
        rates.put(new CurrencyPair("INR", "USD"), new BigDecimal("0.012000"));
        rates.put(new CurrencyPair("EUR", "GBP"), new BigDecimal("0.860000"));
        rates.put(new CurrencyPair("GBP", "EUR"), new BigDecimal("1.163000"));
        return rates;
    }

    @Bean
    public ExchangeRateProvider remitlyProvider() {
        return new SimulatedProvider("Remitly", defaultBaseRates(), 0.000);
    }

    @Bean
    public ExchangeRateProvider wiseProvider() {
        return new SimulatedProvider("Wise", defaultBaseRates(), -0.0015);
    }

    @Bean
    public ExchangeRateProvider westernUnionProvider() {
        return new SimulatedProvider("Western Union", defaultBaseRates(), -0.0050);
    }

    @Bean
    @ConditionalOnProperty(name = "fx.providers.frankfurter.enabled", havingValue = "true")
    public ExchangeRateProvider frankfurterProvider(
            @Value("${fx.providers.frankfurter.base-url:https://api.frankfurter.app}") String baseUrl,
            @Value("${fx.providers.frankfurter.currencies:USD,EUR,GBP,JPY,INR}") List<String> currencies) {
        return new HttpExchangeRateProvider(
                "Frankfurter", baseUrl, currencies, RestClient.create());
    }
}
