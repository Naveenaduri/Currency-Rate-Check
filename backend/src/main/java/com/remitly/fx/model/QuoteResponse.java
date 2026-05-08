package com.remitly.fx.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record QuoteResponse(
        String from,
        String to,
        BigDecimal sendAmount,
        BigDecimal bestReceiveAmount,
        String baselineName,
        Instant lastRefreshAt,
        String source,
        List<ProviderQuote> providers
) {
}
