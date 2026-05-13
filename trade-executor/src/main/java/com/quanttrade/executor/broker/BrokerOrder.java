package com.quanttrade.executor.broker;

import com.quanttrade.executor.domain.OrderIntent;

/**
 * Immutable broker order view returned by broker adapters.
 */
public record BrokerOrder(
    String orderId,
    String status,
    OrderIntent intent,
    int filledQuantity,
    double averageFillPrice,
    String reason
) {
    public BrokerOrder(String orderId, String status, OrderIntent intent) {
        this(
            orderId,
            status,
            intent,
            "FILLED".equals(status) ? intent.quantity() : 0,
            "FILLED".equals(status) ? intent.limitPrice() : 0,
            null
        );
    }
}
