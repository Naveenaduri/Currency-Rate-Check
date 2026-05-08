package com.remitly.fx.wise;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Subset of the Wise V3 Comparisons response we actually consume.
 *
 * <p>Endpoint:
 * {@code GET https://api.wise.com/v3/comparisons/?sourceCurrency=...&targetCurrency=...&sendAmount=...}.
 * Public, no auth required.</p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record WiseComparisonResponse(List<Provider> providers) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Provider(
            Integer id,
            String alias,
            String name,
            String logo,
            String type,
            Boolean partner,
            List<Quote> quotes
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Quote(
            BigDecimal rate,
            BigDecimal fee,
            BigDecimal markup,
            BigDecimal receivedAmount,
            Instant dateCollected,
            String sourceCountry,
            String targetCountry,
            DeliveryEstimation deliveryEstimation
    ) {
    }

    /**
     * Wise's delivery-estimation block has inconsistent typing — {@code deliveryDate}
     * and {@code duration} are sometimes scalars and sometimes nested objects.
     * Treat them as opaque {@code Object} and only consume the fields we actually
     * render ({@link #duration()} as a {@link Number} when it is one).
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record DeliveryEstimation(
            Object deliveryDate,
            Object duration,
            String durationType,
            Boolean providerGivesEstimate
    ) {
        public BigDecimal durationAsBigDecimal() {
            if (duration instanceof Number n) {
                return new BigDecimal(n.toString());
            }
            return null;
        }
    }
}
