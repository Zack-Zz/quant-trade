package com.quanttrade.executor.broker;

import com.quanttrade.executor.domain.OrderIntent;

import java.util.List;

public class QmtBroker implements Broker {
    @Override
    public List<BrokerOrder> placeOrders(List<OrderIntent> intents) {
        throw new UnsupportedOperationException("QMT adapter is a scaffold. Implement QMT API bridge in week 7-8 phase.");
    }

    @Override
    public void cancel(String orderId) {
        throw new UnsupportedOperationException("QMT adapter is a scaffold.");
    }

    @Override
    public List<BrokerOrder> queryOrders() {
        throw new UnsupportedOperationException("QMT adapter is a scaffold.");
    }

    @Override
    public List<BrokerPosition> queryPositions() {
        throw new UnsupportedOperationException("QMT adapter is a scaffold.");
    }
}
