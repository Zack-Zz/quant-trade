package com.quanttrade.executor.domain;

public record PortfolioTarget(String symbol, double targetPct, Double confidence, String reason) {
}
