package com.remitly.fx.wise;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.util.List;

/**
 * Thin client over Wise's public V3 Comparisons API
 * ({@code https://api.wise.com/v3/comparisons/}).
 *
 * <p>Wise publishes this endpoint as part of their "transparent comparison"
 * initiative — competitors' rates and fees scraped + curated by Wise — and
 * it's open to anyone, no auth or API key required.</p>
 */
public class WiseComparisonClient {

    private static final Logger log = LoggerFactory.getLogger(WiseComparisonClient.class);

    private final String baseUrl;
    private final RestClient http;

    public WiseComparisonClient(String baseUrl, RestClient http) {
        this.baseUrl = baseUrl;
        this.http = http;
    }

    /**
     * Fetch the full provider comparison for a given pair + send amount.
     *
     * @return the parsed response, never {@code null}; {@code providers} may be empty
     *         if Wise has no data for the corridor
     * @throws RestClientException on transport / parse errors
     */
    public WiseComparisonResponse fetchComparison(
            String sourceCurrency, String targetCurrency, BigDecimal sendAmount) {
        String url = baseUrl
                + "/v3/comparisons/?sourceCurrency={src}&targetCurrency={dst}&sendAmount={amt}";
        log.debug("Fetching Wise comparison: {} -> {} amount={}",
                sourceCurrency, targetCurrency, sendAmount);
        WiseComparisonResponse response = http.get()
                .uri(url, sourceCurrency, targetCurrency, sendAmount)
                .retrieve()
                .body(WiseComparisonResponse.class);
        if (response == null) {
            return new WiseComparisonResponse(List.of());
        }
        return response;
    }
}
