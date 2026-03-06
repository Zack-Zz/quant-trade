package com.quanttrade.executor.broker;

import com.quanttrade.executor.domain.OrderIntent;

public record BrokerOrder(String orderId, String status, OrderIntent intent) {
}
