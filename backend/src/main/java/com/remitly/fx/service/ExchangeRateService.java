package com.remitly.fx.service;

import com.remitly.fx.model.QuoteResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Application-facing facade over {@link RateAggregator}. Owns the small bits
 * of static config the UI needs (the supported currency picker list).
 */
@Service
public class ExchangeRateService {

    private final RateAggregator aggregator;
    private final List<String> supportedCurrencies;

    public ExchangeRateService(
            RateAggregator aggregator,
            @Value("${fx.currencies.supported:USD,EUR,GBP,JPY,INR,MXN,PHP,AUD,CAD,SGD,CHF}")
            String[] supportedCurrencies) {
        this.aggregator = aggregator;
        this.supportedCurrencies = Arrays.stream(supportedCurrencies)
                .map(String::trim)
                .map(String::toUpperCase)
                .distinct()
                .toList();
    }

    public Set<String> listCurrencies() {
        return new TreeSet<>(supportedCurrencies);
    }

    public Set<String> listProviders() {
        return aggregator.knownProviderNames();
    }

    public QuoteResponse quote(String from, String to, BigDecimal sendAmount) {
        return aggregator.quote(from, to, sendAmount);
    }

    public void refresh(String from, String to) {
        aggregator.invalidate(from, to);
    }
}
