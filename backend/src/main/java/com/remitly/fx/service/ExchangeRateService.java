package com.remitly.fx.service;

import com.remitly.fx.model.RateComparison;
import com.remitly.fx.model.RateQuote;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

/**
 * Application-facing API. Thin delegate over {@link RateAggregator} that
 * translates "missing pair" into the {@link RateNotFoundException} the web
 * layer maps to a 404.
 */
@Service
public class ExchangeRateService {

    private final RateAggregator aggregator;

    public ExchangeRateService(RateAggregator aggregator) {
        this.aggregator = aggregator;
    }

    public Set<String> listCurrencies() {
        return aggregator.listCurrencies();
    }

    public List<String> listProviders() {
        return aggregator.providerNames();
    }

    public RateQuote getBestRate(String from, String to) {
        return aggregator.bestRate(from, to)
                .orElseThrow(() -> new RateNotFoundException(
                        "No provider quotes a direct rate for " + from + " -> " + to));
    }

    public RateComparison compare(String from, String to) {
        RateComparison comparison = aggregator.compare(from, to);
        if (comparison.quotes().isEmpty()) {
            throw new RateNotFoundException(
                    "No provider quotes a direct rate for " + from + " -> " + to);
        }
        return comparison;
    }

    public List<RateQuote> listBestRates() {
        return aggregator.allBestRates();
    }

    public void refreshNow() {
        aggregator.refresh();
    }
}
