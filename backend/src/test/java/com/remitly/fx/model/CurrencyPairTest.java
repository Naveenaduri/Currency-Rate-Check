package com.remitly.fx.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CurrencyPairTest {

    @Test
    void normalizesCodesToUpperCase() {
        CurrencyPair pair = new CurrencyPair("usd", " eur ");
        assertThat(pair.from()).isEqualTo("USD");
        assertThat(pair.to()).isEqualTo("EUR");
    }

    @Test
    void rejectsNullCodes() {
        assertThatThrownBy(() -> new CurrencyPair(null, "USD"))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new CurrencyPair("USD", null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejectsBlankCodes() {
        assertThatThrownBy(() -> new CurrencyPair(" ", "USD"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CurrencyPair("USD", ""))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void equalityIsBasedOnNormalizedCodes() {
        assertThat(new CurrencyPair("usd", "eur"))
                .isEqualTo(new CurrencyPair("USD", "EUR"));
    }
}
