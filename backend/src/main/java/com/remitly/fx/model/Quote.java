package com.remitly.fx.model;

import java.math.BigDecimal;

public record Quote(String provider, BigDecimal rate) {
}
