package com.quanttrade.executor.core;

import com.quanttrade.executor.broker.Broker;
import com.quanttrade.executor.broker.BrokerOrder;
import com.quanttrade.executor.broker.BrokerPosition;
import com.quanttrade.executor.client.SignalClient;
import com.quanttrade.executor.domain.AccountSnapshot;
import com.quanttrade.executor.domain.MarketData;
import com.quanttrade.executor.domain.OrderIntent;
import com.quanttrade.executor.domain.PortfolioTarget;
import com.quanttrade.executor.domain.Position;
import com.quanttrade.executor.domain.Signal;
import com.quanttrade.executor.domain.SignalConstraints;
import com.quanttrade.executor.ledger.InMemoryExecutionLedger;
import com.quanttrade.executor.planner.DefaultOrderPlanner;
import com.quanttrade.executor.risk.DefaultRiskEngine;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExecutionOrchestratorTest {
    @Test
    void avoidsDuplicateExecutionByIdempotency() {
        Signal signal = sampleSignal();
        InMemoryExecutionLedger ledger = new InMemoryExecutionLedger();
        FakeBroker broker = new FakeBroker();

        ExecutionOrchestrator orchestrator = new ExecutionOrchestrator(
            accountId -> signal,
            accountId -> new AccountSnapshot(accountId, 1_000_000, 300_000, Map.of(), Set.of(), 0.0, 0.0),
            symbols -> new MarketData(Map.of("510300.SH", 4.0)),
            new DefaultRiskEngine(),
            new DefaultOrderPlanner(),
            broker,
            ledger
        );

        ExecutionReport first = orchestrator.runOnce("acct");
        ExecutionReport second = orchestrator.runOnce("acct");

        assertFalse(first.skipped());
        assertTrue(second.skipped());
        assertEquals(1, broker.placedBatches);
    }

    private Signal sampleSignal() {
        return new Signal(
            "1.0.0",
            "sig-1",
            "acct",
            OffsetDateTime.parse("2026-03-06T15:00:00+08:00"),
            "strategy",
            List.of("510300.SH"),
            0.2,
            List.of(new PortfolioTarget("510300.SH", 0.5, 0.7, "trend")),
            new SignalConstraints(0.2, 0.6, 0.01, 0.08, "DAILY_CLOSE", 100000000.0),
            null,
            "strategy|2026-03-06T15:00:00+08:00|acct"
        );
    }

    private static class FakeBroker implements Broker {
        private int placedBatches = 0;

        @Override
        public List<BrokerOrder> placeOrders(List<OrderIntent> intents) {
            placedBatches++;
            List<BrokerOrder> output = new ArrayList<>();
            for (int index = 0; index < intents.size(); index++) {
                output.add(new BrokerOrder("order-" + index, "FILLED", intents.get(index)));
            }
            return output;
        }

        @Override
        public void cancel(String orderId) {
        }

        @Override
        public List<BrokerOrder> queryOrders() {
            return List.of();
        }

        @Override
        public List<BrokerPosition> queryPositions() {
            return List.of();
        }
    }
}
