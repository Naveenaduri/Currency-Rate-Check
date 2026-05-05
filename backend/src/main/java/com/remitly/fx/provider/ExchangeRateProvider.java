package com.remitly.fx.provider;

import com.remitly.fx.model.CurrencyPair;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Integration point for an external remittance / FX provider.
 *
 * <p>Implementations are expected to be safe to call from the
 * {@code RateAggregator} polling thread. They MUST NOT cache rates internally
 * across calls in a way that hides errors — return what the upstream service
 * returned for this fetch, or throw to signal a poll failure.</p>
 */
public interface ExchangeRateProvider {

    /**
     * Stable display name (e.g. "Remitly", "Wise"). Used as the key in the
     * aggregator's per-provider cache and shown to users.
     */
    String getName();

    /**
     * Fetch a snapshot of all rates this provider currently quotes.
     * Throwing here causes the aggregator to keep the previously-cached
     * snapshot for this provider.
     */
    Map<CurrencyPair, BigDecimal> fetchRates();
}
