package com.remitly.fx.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;

/**
 * A single provider's quote for a {@code from -> to} pair at a specific send
 * amount, sourced from Wise's V3 comparisons API.
 *
 * @param id              Wise's stable alias ({@code "remitly"}, {@code "wise"}, ...)
 * @param name            display name
 * @param logoUrl         absolute SVG URL Wise hosts on their CDN
 * @param type            {@code "moneyTransferProvider"} or {@code "bank"}
 * @param rate            effective FX rate ({@code 1 from} = {@code rate to})
 * @param fee             flat fee in the source currency
 * @param markup          % above Wise's mid-rate (Wise reports {@code 0})
 * @param receiveAmount   destination-currency amount the recipient gets
 * @param savingsVsBaseline destination-currency delta vs the baseline provider
 * @param deliveryDuration delivery estimate value (may be null)
 * @param deliveryDurationType units for {@code deliveryDuration} (may be null)
 * @param dateCollected   when Wise last collected this rate
 * @param bestDeal        true if this is the highest receive amount
 * @param baseline        true if this is the comparison baseline ("Standard rate")
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ProviderQuote(
        String id,
        String name,
        String logoUrl,
        String type,
        BigDecimal rate,
        BigDecimal fee,
        BigDecimal markup,
        BigDecimal receiveAmount,
        BigDecimal savingsVsBaseline,
        BigDecimal deliveryDuration,
        String deliveryDurationType,
        String dateCollected,
        boolean bestDeal,
        boolean baseline
) {
}
