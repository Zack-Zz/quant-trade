package com.quanttrade.executor.reconcile;

import com.quanttrade.executor.broker.Broker;
import com.quanttrade.executor.broker.BrokerOrder;
import com.quanttrade.executor.broker.BrokerPosition;
import com.quanttrade.executor.domain.OrderIntent;
import com.quanttrade.executor.domain.OrderSide;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultReconcileServiceTest {
    @Test
    void runReturnsMatchedResult() {
        Broker broker = new Broker() {
            @Override
            public List<BrokerOrder> placeOrders(List<OrderIntent> intents) {
                return List.of();
            }

            @Override
            public void cancel(String orderId) {
            }

            @Override
            public List<BrokerOrder> queryOrders() {
                return List.of(new BrokerOrder("1", "FILLED", new OrderIntent("510300.SH", OrderSide.BUY, 100, 4.0)));
            }

            @Override
            public List<BrokerPosition> queryPositions() {
                return List.of();
            }
        };

        DefaultReconcileService reconcileService = new DefaultReconcileService(broker);
        var result = reconcileService.run(LocalDate.of(2026, 3, 6));

        assertTrue(result.matched());
    }
}
