package com.remitly.fx.model;

import java.util.Objects;

public record CurrencyPair(String from, String to) {
    public CurrencyPair {
        Objects.requireNonNull(from, "from must not be null");
        Objects.requireNonNull(to, "to must not be null");
        from = from.trim().toUpperCase();
        to = to.trim().toUpperCase();
        if (from.isEmpty() || to.isEmpty()) {
            throw new IllegalArgumentException("Currency codes must be non-empty");
        }
    }
}
