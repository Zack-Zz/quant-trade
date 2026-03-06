package com.quanttrade.executor.domain;

import java.util.Map;

public record MarketData(Map<String, Double> prices) {
}
