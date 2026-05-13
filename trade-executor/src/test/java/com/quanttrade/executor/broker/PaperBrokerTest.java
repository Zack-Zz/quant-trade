package com.quanttrade.executor.broker;

import com.quanttrade.executor.domain.OrderIntent;
import com.quanttrade.executor.domain.OrderSide;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaperBrokerTest {
    @Test
    void placesOrdersAndUpdatesPositions() {
        PaperBroker broker = new PaperBroker(100_000, Map.of());
        List<OrderIntent> intents = List.of(
            new OrderIntent("510300.SH", OrderSide.BUY, 1000, 4.0),
            new OrderIntent("510300.SH", OrderSide.SELL, 200, 3.9)
        );

        List<BrokerOrder> placed = broker.placeOrders(intents);

        assertEquals(2, placed.size());
        assertEquals(OrderStatus.FILLED.name(), placed.get(0).status());
        assertEquals(1000, placed.get(0).filledQuantity());
        assertEquals(800, broker.queryPositions().stream()
            .filter(position -> position.symbol().equals("510300.SH"))
            .findFirst()
            .orElseThrow()
            .quantity());
    }

    @Test
    void cancelMarksOrderCanceled() {
        PaperBroker broker = new PaperBroker();
        broker.failNextOrderAs(OrderStatus.UNKNOWN, "broker timeout");
        BrokerOrder order = broker.placeOrders(List.of(
            new OrderIntent("000333.SZ", OrderSide.BUY, 100, 50.0)
        )).get(0);

        broker.cancel(order.orderId());

        assertTrue(broker.queryOrders().stream().anyMatch(item ->
            item.orderId().equals(order.orderId()) && item.status().equals(OrderStatus.CANCELED.name())
        ));
    }

    @Test
    void rejectsBuyWhenCashIsInsufficient() {
        PaperBroker broker = new PaperBroker(1_000, Map.of());

        BrokerOrder order = broker.placeOrders(List.of(
            new OrderIntent("000333.SZ", OrderSide.BUY, 100, 50.0)
        )).get(0);

        assertEquals(OrderStatus.REJECTED.name(), order.status());
        assertEquals(0, order.filledQuantity());
        assertEquals(1_000, broker.cash());
    }

    @Test
    void supportsInjectedPartialFillWithoutOverMutatingPosition() {
        PaperBroker broker = new PaperBroker(100_000, Map.of());
        broker.failNextOrderAs(OrderStatus.PARTIALLY_FILLED, "partial market liquidity");

        BrokerOrder order = broker.placeOrders(List.of(
            new OrderIntent("510300.SH", OrderSide.BUY, 1_000, 4.0)
        )).get(0);

        assertEquals(OrderStatus.PARTIALLY_FILLED.name(), order.status());
        assertEquals(500, order.filledQuantity());
        assertEquals(500, broker.queryPositions().get(0).quantity());
    }

    @Test
    void unknownStatusDoesNotMutateCashOrPosition() {
        PaperBroker broker = new PaperBroker(100_000, Map.of());
        broker.failNextOrderAs(OrderStatus.UNKNOWN, "upstream status unknown");

        BrokerOrder order = broker.placeOrders(List.of(
            new OrderIntent("510300.SH", OrderSide.BUY, 1_000, 4.0)
        )).get(0);

        assertEquals(OrderStatus.UNKNOWN.name(), order.status());
        assertEquals(0, order.filledQuantity());
        assertEquals(100_000, broker.cash());
        assertTrue(broker.queryPositions().isEmpty());
    }
}
