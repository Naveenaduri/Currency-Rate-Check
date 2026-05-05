package com.remitly.fx.provider;

import com.remitly.fx.model.CurrencyPair;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * In-process simulation of a remittance provider. Each call to
 * {@link #fetchRates()} returns rates derived from a shared base, multiplied
 * by a provider-specific {@code spread} (negative spread = better for the
 * customer = higher destination amount per unit of source currency) plus a
 * small random jitter. This makes "polling" visible: each poll cycle yields
 * slightly different numbers, and different providers have different
 * baselines.
 *
 * <p>Used in place of real HTTP calls when no provider credentials/URL are
 * configured. See {@link HttpExchangeRateProvider} for the real-API flavour.</p>
 */
public class SimulatedProvider implements ExchangeRateProvider {

    private static final int SCALE = 6;
    private static final double JITTER = 0.002;

    private final String name;
    private final Map<CurrencyPair, BigDecimal> baseRates;
    private final double spread;
    private final Random random;

    public SimulatedProvider(
            String name,
            Map<CurrencyPair, BigDecimal> baseRates,
            double spread,
            Random random) {
        this.name = name;
        this.baseRates = Map.copyOf(baseRates);
        this.spread = spread;
        this.random = random;
    }

    public SimulatedProvider(String name, Map<CurrencyPair, BigDecimal> baseRates, double spread) {
        this(name, baseRates, spread, new Random());
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Map<CurrencyPair, BigDecimal> fetchRates() {
        Map<CurrencyPair, BigDecimal> snapshot = new HashMap<>();
        for (Map.Entry<CurrencyPair, BigDecimal> entry : baseRates.entrySet()) {
            double jitter = (random.nextDouble() - 0.5) * JITTER;
            double factor = 1.0 + spread + jitter;
            BigDecimal rate = entry.getValue()
                    .multiply(BigDecimal.valueOf(factor))
                    .setScale(SCALE, RoundingMode.HALF_UP);
            if (rate.signum() > 0) {
                snapshot.put(entry.getKey(), rate);
            }
        }
        return snapshot;
    }
}
