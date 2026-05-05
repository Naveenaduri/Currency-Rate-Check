package com.remitly.fx.model;

import java.math.BigDecimal;

/**
 * The best quote for a given currency pair, including which provider supplied it.
 */
public record RateQuote(String from, String to, BigDecimal rate, String provider) {
}
