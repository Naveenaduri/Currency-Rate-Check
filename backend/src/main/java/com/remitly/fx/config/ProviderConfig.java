package com.remitly.fx.config;

import com.remitly.fx.wise.WiseComparisonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

/**
 * Wires the {@link WiseComparisonClient} the {@link com.remitly.fx.service.RateAggregator}
 * uses for live multi-provider quotes.
 */
@Configuration
public class ProviderConfig {

    @Bean
    public WiseComparisonClient wiseComparisonClient(
            @Value("${fx.wise.base-url:https://api.wise.com}") String baseUrl,
            @Value("${fx.wise.connect-timeout-ms:5000}") int connectTimeout,
            @Value("${fx.wise.read-timeout-ms:8000}") int readTimeout) {
        HttpClient jdkClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(connectTimeout))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(jdkClient);
        factory.setReadTimeout(Duration.ofMillis(readTimeout));
        RestClient http = RestClient.builder()
                .requestFactory(factory)
                .defaultHeader("User-Agent", "fx-backend/1.0 (+https://github.com/remitly-fx)")
                .defaultHeader("Accept", "application/json")
                .defaultHeader("Accept-Encoding", "identity")
                .build();
        return new WiseComparisonClient(baseUrl, http);
    }
}
