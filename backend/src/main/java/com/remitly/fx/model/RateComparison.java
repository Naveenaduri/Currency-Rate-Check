package com.remitly.fx.model;

import java.util.List;

/**
 * All available quotes for a currency pair, with the best one called out.
 */
public record RateComparison(String from, String to, Quote best, List<Quote> quotes) {
}
