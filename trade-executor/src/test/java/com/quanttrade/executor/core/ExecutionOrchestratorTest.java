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
import com.quanttrade.executor.domain.RiskDecision;
import com.quanttrade.executor.ledger.ExecutionClaim;
import com.quanttrade.executor.ledger.ExecutionLedger;
import com.quanttrade.executor.ledger.InMemoryExecutionLedger;
import com.quanttrade.executor.planner.DefaultOrderPlanner;
import com.quanttrade.executor.planner.OrderPlanner;
import com.quanttrade.executor.risk.DefaultRiskEngine;
import com.quanttrade.executor.risk.RiskEngine;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
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

    @Test
    void riskRejectDoesNotTouchBroker() {
        Signal signal = new Signal(
            "1.0.0",
            "sig-1",
            "acct",
            OffsetDateTime.parse("2026-03-06T15:00:00+08:00"),
            "strategy",
            List.of("510300.SH"),
            0.2,
            List.of(new PortfolioTarget("510300.SH", 0.8, 0.7, "trend")),
            new SignalConstraints(0.2, 0.3, 0.01, 0.08, "DAILY_CLOSE", 100000000.0),
            null,
            "strategy|2026-03-06T15:00:00+08:00|acct"
        );
        FakeBroker broker = new FakeBroker();

        ExecutionOrchestrator orchestrator = new ExecutionOrchestrator(
            accountId -> signal,
            accountId -> new AccountSnapshot(accountId, 1_000_000, 300_000, Map.of(), Set.of(), 0.0, 0.0),
            symbols -> new MarketData(Map.of("510300.SH", 4.0)),
            new DefaultRiskEngine(),
            new DefaultOrderPlanner(),
            broker,
            new InMemoryExecutionLedger()
        );

        ExecutionReport report = orchestrator.runOnce("acct");

        assertTrue(report.skipped());
        assertEquals("risk-rejected", report.reason());
        assertEquals(0, broker.placedBatches);
    }

    @Test
    void riskRejectPersistsTerminalStatusBeforeMarkProcessed() {
        Signal signal = new Signal(
            "1.0.0",
            "sig-1",
            "acct",
            OffsetDateTime.parse("2026-03-06T15:00:00+08:00"),
            "strategy",
            List.of("510300.SH"),
            0.2,
            List.of(new PortfolioTarget("510300.SH", 0.8, 0.7, "trend")),
            new SignalConstraints(0.2, 0.3, 0.01, 0.08, "DAILY_CLOSE", 100000000.0),
            null,
            "strategy|2026-03-06T15:00:00+08:00|acct"
        );
        List<String> events = new ArrayList<>();
        RecordingLedger ledger = new RecordingLedger(events);

        ExecutionOrchestrator orchestrator = new ExecutionOrchestrator(
            accountId -> signal,
            accountId -> new AccountSnapshot(accountId, 1_000_000, 300_000, Map.of(), Set.of(), 0.0, 0.0),
            symbols -> new MarketData(Map.of("510300.SH", 4.0)),
            new DefaultRiskEngine(),
            new DefaultOrderPlanner(),
            new FakeBroker(events),
            ledger
        );

        ExecutionReport report = orchestrator.runOnce("acct");

        assertTrue(report.skipped());
        assertEquals("RISK_REJECTED", ledger.runStatuses.get("run-1"));
        assertTrue(events.indexOf("mark-risk-rejected") < events.indexOf("mark-processed"));
        assertFalse(events.contains("broker-place"));
    }

    @Test
    void savesOrderPlanBeforeBrokerPlacementAndBrokerOrdersAfterPlacement() {
        Signal signal = sampleSignal();
        List<String> events = new ArrayList<>();
        RecordingLedger ledger = new RecordingLedger(events);
        FakeBroker broker = new FakeBroker(events);

        ExecutionOrchestrator orchestrator = new ExecutionOrchestrator(
            accountId -> signal,
            accountId -> new AccountSnapshot(accountId, 1_000_000, 300_000, Map.of(), Set.of(), 0.0, 0.0),
            symbols -> new MarketData(Map.of("510300.SH", 4.0)),
            new DefaultRiskEngine(),
            new DefaultOrderPlanner(),
            broker,
            ledger
        );

        ExecutionReport report = orchestrator.runOnce("acct");

        assertFalse(report.skipped());
        assertTrue(events.indexOf("save-order-plan") < events.indexOf("broker-place"));
        assertTrue(events.indexOf("broker-place") < events.indexOf("save-broker-orders"));
        assertEquals(1, ledger.plannedIntents.size());
    }

    @Test
    void checksumMismatchSkipsBeforeRiskPlannerAndBroker() {
        Signal signal = v2SignalWithChecksum("sha256:0000000000000000000000000000000000000000000000000000000000000000");
        CountingRiskEngine riskEngine = new CountingRiskEngine();
        CountingOrderPlanner orderPlanner = new CountingOrderPlanner();
        FakeBroker broker = new FakeBroker();

        ExecutionOrchestrator orchestrator = new ExecutionOrchestrator(
            accountId -> signal,
            accountId -> new AccountSnapshot(accountId, 1_000_000, 300_000, Map.of(), Set.of(), 0.0, 0.0),
            symbols -> new MarketData(Map.of("510300.SH", 4.0)),
            riskEngine,
            orderPlanner,
            broker,
            new InMemoryExecutionLedger()
        );

        ExecutionReport report = orchestrator.runOnce("acct");

        assertTrue(report.skipped());
        assertEquals("checksum-mismatch", report.reason());
        assertEquals(0, riskEngine.calls);
        assertEquals(0, orderPlanner.calls);
        assertEquals(0, broker.placedBatches);
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

    private Signal v2SignalWithChecksum(String checksum) {
        return new Signal(
            "2.0.0",
            "sig-v2",
            "acct",
            LocalDate.parse("2026-03-06"),
            OffsetDateTime.parse("2026-03-06T15:00:00+08:00"),
            "strategy",
            "v2",
            "DAILY_CLOSE",
            "csv-20260306-v1",
            "PUBLISHED",
            List.of("510300.SH"),
            0.2,
            List.of(new PortfolioTarget("510300.SH", 0.5, 0.7, "trend")),
            new SignalConstraints(0.2, 0.6, 0.01, 0.08, "DAILY_CLOSE", 100000000.0),
            checksum,
            "strategy|v2|acct|2026-03-06|DAILY_CLOSE",
            Map.of("source", "test")
        );
    }

    private static class FakeBroker implements Broker {
        private int placedBatches = 0;
        private final List<String> events;

        private FakeBroker() {
            this.events = null;
        }

        private FakeBroker(List<String> events) {
            this.events = events;
        }

        @Override
        public List<BrokerOrder> placeOrders(List<OrderIntent> intents) {
            placedBatches++;
            if (events != null) {
                events.add("broker-place");
            }
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

    private static class RecordingLedger implements ExecutionLedger {
        private final List<String> events;
        private final List<OrderIntent> plannedIntents = new ArrayList<>();
        private final Map<String, String> runStatuses = new java.util.HashMap<>();

        private RecordingLedger(List<String> events) {
            this.events = events;
        }

        @Override
        public ExecutionClaim claimRun(Signal signal, String traceId) {
            events.add("claim-run");
            return new ExecutionClaim("run-1", true);
        }

        @Override
        public boolean isProcessed(String idempotencyKey) {
            return false;
        }

        @Override
        public void markProcessed(String idempotencyKey) {
            events.add("mark-processed");
        }

        @Override
        public void saveSignal(Signal signal) {
            events.add("save-signal");
        }

        @Override
        public void saveRiskDecision(String signalId, RiskDecision decision) {
            events.add("save-risk");
        }

        @Override
        public void markRiskRejected(String runId, RiskDecision decision) {
            events.add("mark-risk-rejected");
            runStatuses.put(runId, "RISK_REJECTED");
        }

        @Override
        public void saveOrderPlan(String runId, Signal signal, List<OrderIntent> intents) {
            events.add("save-order-plan");
            plannedIntents.addAll(intents);
        }

        @Override
        public void saveBrokerOrders(String runId, List<BrokerOrder> orders) {
            events.add("save-broker-orders");
        }
    }

    private static class CountingRiskEngine implements RiskEngine {
        private int calls = 0;

        @Override
        public RiskDecision evaluate(Signal signal, AccountSnapshot snapshot) {
            calls++;
            return new RiskDecision(true, List.of(), 0.2);
        }
    }

    private static class CountingOrderPlanner implements OrderPlanner {
        private int calls = 0;

        @Override
        public List<OrderIntent> plan(Signal signal, AccountSnapshot snapshot, MarketData marketData) {
            calls++;
            return List.of(new OrderIntent("510300.SH", com.quanttrade.executor.domain.OrderSide.BUY, 100, 4.1));
        }
    }
}
