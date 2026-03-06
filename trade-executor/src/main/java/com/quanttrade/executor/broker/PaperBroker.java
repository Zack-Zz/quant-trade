package com.quanttrade.executor.broker;

import com.quanttrade.executor.domain.OrderIntent;
import com.quanttrade.executor.domain.OrderSide;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PaperBroker implements Broker {
    private final List<BrokerOrder> orders = new ArrayList<>();
    private final Map<String, Integer> positions = new HashMap<>();

    @Override
    public List<BrokerOrder> placeOrders(List<OrderIntent> intents) {
        List<BrokerOrder> placed = new ArrayList<>();

        for (OrderIntent intent : intents) {
            int current = positions.getOrDefault(intent.symbol(), 0);
            int delta = intent.side() == OrderSide.BUY ? intent.quantity() : -intent.quantity();
            int next = Math.max(0, current + delta);
            positions.put(intent.symbol(), next);

            BrokerOrder order = new BrokerOrder(UUID.randomUUID().toString(), "FILLED", intent);
            orders.add(order);
            placed.add(order);
        }

        return placed;
    }

    @Override
    public void cancel(String orderId) {
        orders.replaceAll(order -> order.orderId().equals(orderId)
            ? new BrokerOrder(order.orderId(), "CANCELED", order.intent())
            : order
        );
    }

    @Override
    public List<BrokerOrder> queryOrders() {
        return List.copyOf(orders);
    }

    @Override
    public List<BrokerPosition> queryPositions() {
        return positions.entrySet().stream()
            .map(entry -> new BrokerPosition(entry.getKey(), entry.getValue()))
            .toList();
    }
}
