package com.quanttrade.executor.broker;

import com.quanttrade.executor.client.HttpSignalClient;
import com.quanttrade.executor.domain.OrderIntent;
import com.quanttrade.executor.domain.OrderSide;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;

class AdapterScaffoldTest {
    @Test
    void qmtBrokerMethodsThrowUntilImplemented() {
        QmtBroker broker = new QmtBroker();

        assertThrows(UnsupportedOperationException.class, () -> broker.placeOrders(List.of()));
        assertThrows(UnsupportedOperationException.class, () -> broker.cancel("o-1"));
        assertThrows(UnsupportedOperationException.class, broker::queryOrders);
        assertThrows(UnsupportedOperationException.class, broker::queryPositions);
    }

    @Test
    void httpSignalClientFailsClosedWhenServiceUnavailable() {
        HttpSignalClient client = new HttpSignalClient("http://127.0.0.1:1");
        assertThrows(IllegalStateException.class, () -> client.fetchLatest("acct"));
    }

    @Test
    void brokerOrderAndPositionCanBeCreated() {
        BrokerOrder order = new BrokerOrder("o-1", "NEW", new OrderIntent("510300.SH", OrderSide.BUY, 100, 4.0));
        BrokerPosition position = new BrokerPosition("510300.SH", 100);

        order.intent();
        position.quantity();
    }
}
