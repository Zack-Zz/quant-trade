package com.quanttrade.executor.broker;

import com.quanttrade.executor.domain.OrderIntent;
import com.quanttrade.executor.domain.OrderSide;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaperBrokerTest {
    @Test
    void placesOrdersAndUpdatesPositions() {
        PaperBroker broker = new PaperBroker();
        List<OrderIntent> intents = List.of(
            new OrderIntent("510300.SH", OrderSide.BUY, 1000, 4.0),
            new OrderIntent("510300.SH", OrderSide.SELL, 200, 3.9)
        );

        List<BrokerOrder> placed = broker.placeOrders(intents);

        assertEquals(2, placed.size());
        assertEquals("FILLED", placed.get(0).status());
        assertEquals(800, broker.queryPositions().stream()
            .filter(position -> position.symbol().equals("510300.SH"))
            .findFirst()
            .orElseThrow()
            .quantity());
    }

    @Test
    void cancelMarksOrderCanceled() {
        PaperBroker broker = new PaperBroker();
        BrokerOrder order = broker.placeOrders(List.of(
            new OrderIntent("000333.SZ", OrderSide.BUY, 100, 50.0)
        )).get(0);

        broker.cancel(order.orderId());

        assertTrue(broker.queryOrders().stream().anyMatch(item ->
            item.orderId().equals(order.orderId()) && item.status().equals("CANCELED")
        ));
    }
}
