package com.quanttrade.executor.broker;

import com.quanttrade.executor.domain.OrderIntent;
import com.quanttrade.executor.domain.OrderSide;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * In-memory paper broker that simulates cash, positions and order lifecycle states.
 */
public class PaperBroker implements Broker {
    private final List<BrokerOrder> orders = new ArrayList<>();
    private final Map<String, Integer> positions = new HashMap<>();
    private double cash;
    private OrderStatus nextForcedStatus;
    private String nextForcedReason;

    public PaperBroker() {
        this(1_000_000, Map.of());
    }

    public PaperBroker(double initialCash, Map<String, Integer> initialPositions) {
        this.cash = initialCash;
        this.positions.putAll(initialPositions);
    }

    /**
     * Forces the next order into a specific terminal or uncertain status for recovery tests.
     */
    public void failNextOrderAs(OrderStatus status, String reason) {
        this.nextForcedStatus = status;
        this.nextForcedReason = reason;
    }

    /**
     * Returns simulated available cash after all known fills.
     */
    public double cash() {
        return cash;
    }

    @Override
    public List<BrokerOrder> placeOrders(List<OrderIntent> intents) {
        List<BrokerOrder> placed = new ArrayList<>();

        for (OrderIntent intent : intents) {
            BrokerOrder order = execute(UUID.randomUUID().toString(), intent);
            orders.add(order);
            placed.add(order);
        }

        return placed;
    }

    @Override
    public void cancel(String orderId) {
        orders.replaceAll(order -> order.orderId().equals(orderId)
            ? new BrokerOrder(
                order.orderId(),
                OrderStatus.CANCELED.name(),
                order.intent(),
                order.filledQuantity(),
                order.averageFillPrice(),
                order.reason()
            )
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

    private BrokerOrder execute(String orderId, OrderIntent intent) {
        if (nextForcedStatus != null) {
            OrderStatus forced = nextForcedStatus;
            String forcedReason = nextForcedReason;
            nextForcedStatus = null;
            nextForcedReason = null;
            return executeForced(orderId, intent, forced, forcedReason);
        }

        if (intent.side() == OrderSide.BUY && tradeValue(intent.quantity(), intent.limitPrice()) > cash) {
            return rejected(orderId, intent, "insufficient cash");
        }
        if (intent.side() == OrderSide.SELL && positions.getOrDefault(intent.symbol(), 0) < intent.quantity()) {
            return rejected(orderId, intent, "insufficient position");
        }

        applyFill(intent, intent.quantity());
        return new BrokerOrder(
            orderId,
            OrderStatus.FILLED.name(),
            intent,
            intent.quantity(),
            intent.limitPrice(),
            null
        );
    }

    private BrokerOrder executeForced(String orderId, OrderIntent intent, OrderStatus status, String reason) {
        if (status == OrderStatus.UNKNOWN) {
            // UNKNOWN means the adapter cannot prove a fill happened, so local cash and positions stay untouched.
            return new BrokerOrder(orderId, status.name(), intent, 0, 0, reason);
        }
        if (status == OrderStatus.REJECTED) {
            return rejected(orderId, intent, reason == null ? "forced rejection" : reason);
        }
        if (status == OrderStatus.PARTIALLY_FILLED) {
            int fillQuantity = Math.max(0, intent.quantity() / 2);
            if (fillQuantity > 0) {
                applyFill(intent, fillQuantity);
            }
            return new BrokerOrder(orderId, status.name(), intent, fillQuantity, intent.limitPrice(), reason);
        }
        if (status == OrderStatus.CANCELED) {
            return new BrokerOrder(orderId, status.name(), intent, 0, 0, reason);
        }
        return execute(orderId, intent);
    }

    private BrokerOrder rejected(String orderId, OrderIntent intent, String reason) {
        return new BrokerOrder(orderId, OrderStatus.REJECTED.name(), intent, 0, 0, reason);
    }

    private void applyFill(OrderIntent intent, int quantity) {
        int current = positions.getOrDefault(intent.symbol(), 0);
        int delta = intent.side() == OrderSide.BUY ? quantity : -quantity;
        int next = Math.max(0, current + delta);
        positions.put(intent.symbol(), next);

        double value = tradeValue(quantity, intent.limitPrice());
        cash += intent.side() == OrderSide.BUY ? -value : value;
    }

    private double tradeValue(int quantity, double price) {
        return quantity * price;
    }
}
