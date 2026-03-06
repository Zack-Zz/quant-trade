package com.quanttrade.executor.domain;

public record OrderIntent(String symbol, OrderSide side, int quantity, double limitPrice) {
}
