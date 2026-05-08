package com.remitly.fx.wise;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.http.client.MockClientHttpResponse;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WiseComparisonClientTest {

    private RestClient stub(Map<String, String> bodies, int status) {
        return RestClient.builder()
                .requestFactory((uri, method) -> new StubRequest(uri, bodies, status))
                .build();
    }

    private static final String FIXTURE_BODY = """
            {
              "providerCountry": null,
              "providerType": null,
              "providers": [
                {
                  "id": 104,
                  "alias": "remitly",
                  "name": "Remitly",
                  "logo": "https://example/remitly.svg",
                  "type": "moneyTransferProvider",
                  "partner": false,
                  "quotes": [
                    {
                      "rate": 94.30,
                      "fee": 0.0,
                      "markup": 0.27,
                      "receivedAmount": 94300.0,
                      "sourceCountry": "US",
                      "targetCountry": "IN",
                      "dateCollected": "2026-05-08T03:46:25Z",
                      "deliveryEstimation": {
                        "deliveryDate": null,
                        "duration": null,
                        "durationType": null,
                        "providerGivesEstimate": true
                      }
                    }
                  ]
                },
                {
                  "id": 1,
                  "alias": "wise",
                  "name": "Wise",
                  "logo": "https://example/wise.svg",
                  "type": "moneyTransferProvider",
                  "partner": true,
                  "quotes": [
                    {"rate": 94.20, "fee": 2.0, "markup": 0.0, "receivedAmount": 94011.6,
                     "deliveryEstimation": {"duration": 1, "durationType": "hours"}}
                  ]
                }
              ]
            }
            """;

    @Test
    void fetchComparisonParsesProvidersAndQuotes() {
        WiseComparisonClient client = new WiseComparisonClient("https://example.test",
                stub(Map.of("any", FIXTURE_BODY), 200));

        WiseComparisonResponse response = client.fetchComparison(
                "USD", "INR", new BigDecimal("1000"));

        assertThat(response.providers()).hasSize(2);
        WiseComparisonResponse.Provider remitly = response.providers().get(0);
        assertThat(remitly.alias()).isEqualTo("remitly");
        assertThat(remitly.logo()).contains("remitly.svg");
        assertThat(remitly.quotes()).hasSize(1);
        assertThat(remitly.quotes().get(0).rate()).isEqualByComparingTo("94.30");
    }

    @Test
    void emptyBodyYieldsEmptyProviderList() {
        WiseComparisonClient client = new WiseComparisonClient("https://example.test",
                stub(Map.of("any", "{}"), 200));
        WiseComparisonResponse response = client.fetchComparison(
                "USD", "EUR", new BigDecimal("100"));
        assertThat(response.providers()).isNull();
    }

    @Test
    void serverErrorBubblesUpAsRestClientException() {
        WiseComparisonClient client = new WiseComparisonClient("https://example.test",
                stub(Map.of("any", "ouch"), 500));
        assertThatThrownBy(() -> client.fetchComparison("USD", "INR", new BigDecimal("1000")))
                .isInstanceOf(RestClientException.class);
    }

    private static final class StubRequest
            extends org.springframework.http.client.AbstractClientHttpRequest {
        private final URI uri;
        private final Map<String, String> bodies;
        private final int status;

        StubRequest(URI uri, Map<String, String> bodies, int status) {
            this.uri = uri;
            this.bodies = bodies;
            this.status = status;
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
            String body = bodies.values().iterator().next();
            HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.setContentType(MediaType.APPLICATION_JSON);
            MockClientHttpResponse response = new MockClientHttpResponse(
                    body.getBytes(StandardCharsets.UTF_8), status);
            response.getHeaders().addAll(responseHeaders);
            return response;
        }
    }
}
