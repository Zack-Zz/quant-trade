package com.quanttrade.executor.domain;

public record Position(String symbol, int quantity, double avgCost) {
}
