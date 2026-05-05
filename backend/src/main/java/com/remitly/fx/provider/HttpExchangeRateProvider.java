package com.remitly.fx.provider;

import com.remitly.fx.model.CurrencyPair;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Generic HTTP-based provider integration. Targets a Frankfurter-compatible
 * endpoint (https://api.frankfurter.app) which exposes
 * {@code GET /latest?from=USD} → {@code { "base": "USD", "rates": { "EUR": 0.92, ... } }}.
 *
 * <p>This class is the "real" integration template. It is registered as a
 * Spring bean only when the matching {@code fx.providers.*.enabled} property
 * is set, so that the default boot doesn't depend on internet access.</p>
 */
public class HttpExchangeRateProvider implements ExchangeRateProvider {

    private final String name;
    private final String baseUrl;
    private final List<String> currencies;
    private final RestClient http;

    public HttpExchangeRateProvider(
            String name, String baseUrl, List<String> currencies, RestClient http) {
        this.name = name;
        this.baseUrl = baseUrl;
        this.currencies = List.copyOf(currencies);
        this.http = http;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Map<CurrencyPair, BigDecimal> fetchRates() {
        Map<CurrencyPair, BigDecimal> result = new HashMap<>();
        Set<String> known = Set.copyOf(currencies);
        for (String base : currencies) {
            FrankfurterResponse response = http.get()
                    .uri(baseUrl + "/latest?from={base}", base)
                    .retrieve()
                    .body(FrankfurterResponse.class);
            if (response == null || response.rates() == null) continue;
            for (Map.Entry<String, BigDecimal> entry : response.rates().entrySet()) {
                String to = entry.getKey();
                if (!known.contains(to)) continue;
                result.put(new CurrencyPair(base, to), entry.getValue());
            }
        }
        return result;
    }

    public record FrankfurterResponse(String base, Map<String, BigDecimal> rates) {
    }
}
