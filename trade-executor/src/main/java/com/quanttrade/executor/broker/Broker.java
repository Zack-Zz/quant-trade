package com.quanttrade.executor.broker;

import com.quanttrade.executor.domain.OrderIntent;

import java.util.List;

public interface Broker {
    List<BrokerOrder> placeOrders(List<OrderIntent> intents);

    void cancel(String orderId);

    List<BrokerOrder> queryOrders();

    List<BrokerPosition> queryPositions();
}
