package com.remitly.fx.provider;

import com.remitly.fx.model.CurrencyPair;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.http.client.MockClientHttpResponse;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class HttpExchangeRateProviderTest {

    /**
     * Stubs the request factory so RestClient never opens a real socket.
     * Each call to {@code createRequest} pops the next queued JSON body and
     * returns it via {@link MockClientHttpResponse}.
     */
    private RestClient stubClient(Map<String, String> responsesByPathAndQuery) {
        return RestClient.builder()
                .requestFactory((uri, httpMethod) -> {
                    String body = responsesByPathAndQuery.getOrDefault(
                            uri.getRawQuery(), "{}");
                    return new StubRequest(uri, body);
                })
                .build();
    }

    @Test
    void exposesProviderName() {
        HttpExchangeRateProvider provider = new HttpExchangeRateProvider(
                "Frankfurter", "https://example.test", List.of("USD"),
                RestClient.create());
        assertThat(provider.getName()).isEqualTo("Frankfurter");
    }

    @Test
    void parsesFrankfurterResponsesAndDropsUnknownTargetCurrencies() {
        Map<String, String> bodies = new LinkedHashMap<>();
        bodies.put("from=USD",
                "{\"base\":\"USD\",\"rates\":{\"EUR\":0.92,\"GBP\":0.79,\"XYZ\":1.0}}");
        bodies.put("from=EUR",
                "{\"base\":\"EUR\",\"rates\":{\"USD\":1.087,\"GBP\":0.86}}");

        HttpExchangeRateProvider provider = new HttpExchangeRateProvider(
                "Frankfurter",
                "https://example.test",
                List.of("USD", "EUR", "GBP"),
                stubClient(bodies));

        Map<CurrencyPair, BigDecimal> snapshot = provider.fetchRates();

        assertThat(snapshot).containsKeys(
                new CurrencyPair("USD", "EUR"),
                new CurrencyPair("USD", "GBP"),
                new CurrencyPair("EUR", "USD"),
                new CurrencyPair("EUR", "GBP"));
        assertThat(snapshot).doesNotContainKey(new CurrencyPair("USD", "XYZ"));
        assertThat(snapshot.get(new CurrencyPair("USD", "EUR")))
                .isEqualByComparingTo("0.92");
    }

    @Test
    void handlesEmptyOrMissingRatesPayload() {
        Map<String, String> bodies = new HashMap<>();
        bodies.put("from=USD", "{\"base\":\"USD\"}");

        HttpExchangeRateProvider provider = new HttpExchangeRateProvider(
                "Frankfurter",
                "https://example.test",
                List.of("USD"),
                stubClient(bodies));

        assertThat(provider.fetchRates()).isEmpty();
    }

    private static final class StubRequest
            extends org.springframework.http.client.AbstractClientHttpRequest {
        private final URI uri;
        private final String body;

        StubRequest(URI uri, String body) {
            this.uri = uri;
            this.body = body;
        }

        @Override
        public org.springframework.http.HttpMethod getMethod() {
            return org.springframework.http.HttpMethod.GET;
        }

        @Override
        public URI getURI() {
            return uri;
        }

        @Override
        protected java.io.OutputStream getBodyInternal(HttpHeaders headers) {
            return java.io.OutputStream.nullOutputStream();
        }

        @Override
        protected org.springframework.http.client.ClientHttpResponse executeInternal(
                HttpHeaders headers) throws IOException {
            HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.setContentType(MediaType.APPLICATION_JSON);
            MockClientHttpResponse response = new MockClientHttpResponse(
                    body.getBytes(StandardCharsets.UTF_8), 200);
            response.getHeaders().addAll(responseHeaders);
            return response;
        }
    }
}
