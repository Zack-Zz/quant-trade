package com.quanttrade.executor.core;

import com.quanttrade.executor.broker.Broker;
import com.quanttrade.executor.broker.BrokerOrder;
import com.quanttrade.executor.client.SignalClient;
import com.quanttrade.executor.domain.AccountSnapshot;
import com.quanttrade.executor.domain.MarketData;
import com.quanttrade.executor.domain.OrderIntent;
import com.quanttrade.executor.domain.ReconcileResult;
import com.quanttrade.executor.domain.RiskDecision;
import com.quanttrade.executor.domain.Signal;
import com.quanttrade.executor.domain.SignalChecksum;
import com.quanttrade.executor.ledger.ExecutionClaim;
import com.quanttrade.executor.ledger.ExecutionLedger;
import com.quanttrade.executor.planner.OrderPlanner;
import com.quanttrade.executor.reconcile.ReconcileService;
import com.quanttrade.executor.risk.RiskEngine;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public class ExecutionOrchestrator {
    private final SignalClient signalClient;
    private final SnapshotProvider snapshotProvider;
    private final MarketDataProvider marketDataProvider;
    private final RiskEngine riskEngine;
    private final OrderPlanner orderPlanner;
    private final Broker broker;
    private final ExecutionLedger ledger;
    private final ReconcileService reconcileService;

    public ExecutionOrchestrator(
        SignalClient signalClient,
        SnapshotProvider snapshotProvider,
        MarketDataProvider marketDataProvider,
        RiskEngine riskEngine,
        OrderPlanner orderPlanner,
        Broker broker,
        ExecutionLedger ledger
    ) {
        this(
            signalClient,
            snapshotProvider,
            marketDataProvider,
            riskEngine,
            orderPlanner,
            broker,
            ledger,
            tradingDate -> new ReconcileResult(tradingDate, true, "reconcile skipped")
        );
    }

    public ExecutionOrchestrator(
        SignalClient signalClient,
        SnapshotProvider snapshotProvider,
        MarketDataProvider marketDataProvider,
        RiskEngine riskEngine,
        OrderPlanner orderPlanner,
        Broker broker,
        ExecutionLedger ledger,
        ReconcileService reconcileService
    ) {
        this.signalClient = signalClient;
        this.snapshotProvider = snapshotProvider;
        this.marketDataProvider = marketDataProvider;
        this.riskEngine = riskEngine;
        this.orderPlanner = orderPlanner;
        this.broker = broker;
        this.ledger = ledger;
        this.reconcileService = reconcileService;
    }

    public ExecutionReport runOnce(String accountId) {
        OffsetDateTime startedAt = OffsetDateTime.now();
        String traceId = "trace-" + UUID.randomUUID();
        Signal signal = signalClient.fetchLatest(accountId);

        if ("2.0.0".equals(signal.schemaVersion()) && !SignalChecksum.matches(signal)) {
            return report(true, "checksum-mismatch", signal, null, null, traceId, startedAt, 0, 0, 0, 0, null);
        }

        ExecutionClaim claim = ledger.claimRun(signal, traceId);
        if (!claim.claimed()) {
            return report(true, "idempotency-hit", signal, null, claim.runId(), traceId, startedAt, 0, 0, 0, 0, null);
        }

        String runId = claim.runId();
        ledger.saveSignal(signal);
        AccountSnapshot snapshot = snapshotProvider.current(accountId);
        RiskDecision riskDecision = riskEngine.evaluate(signal, snapshot);
        ledger.saveRiskDecision(signal.signalId(), riskDecision);

        if (!riskDecision.approved()) {
            ledger.markRiskRejected(runId, riskDecision);
            ledger.markProcessed(signal.idempotencyKey());
            return report(true, "risk-rejected", signal, riskDecision, runId, traceId, startedAt, 0, 0, 0, 0, null);
        }

        MarketData marketData = marketDataProvider.latest(signal.universe());
        List<OrderIntent> intents = orderPlanner.plan(signal, snapshot, marketData);
        ledger.saveOrderPlan(runId, signal, intents);
        List<BrokerOrder> placed = broker.placeOrders(intents);
        ledger.saveBrokerOrders(runId, placed);
        LocalDate tradingDate = signal.tradingDate() == null ? LocalDate.now() : signal.tradingDate();
        ReconcileResult reconcileResult = reconcileService.run(tradingDate);
        ledger.saveReconcileResult(runId, reconcileResult);
        ledger.markProcessed(signal.idempotencyKey());

        return report(
            false,
            "executed",
            signal,
            riskDecision,
            runId,
            traceId,
            startedAt,
            intents.size(),
            placed.size(),
            (int) placed.stream().filter(order -> "FILLED".equals(order.status())).count(),
            (int) placed.stream().filter(order -> "REJECTED".equals(order.status())).count(),
            reconcileResult.matched() ? "MATCHED" : "MISMATCHED"
        );
    }

    private ExecutionReport report(
        boolean skipped,
        String reason,
        Signal signal,
        RiskDecision riskDecision,
        String runId,
        String traceId,
        OffsetDateTime startedAt,
        int plannedOrderCount,
        int submittedOrderCount,
        int filledOrderCount,
        int rejectedOrderCount,
        String reconcileStatus
    ) {
        return new ExecutionReport(
            skipped,
            reason,
            submittedOrderCount,
            riskDecision,
            runId,
            traceId,
            signal.signalId(),
            signal.idempotencyKey(),
            reason,
            plannedOrderCount,
            submittedOrderCount,
            filledOrderCount,
            rejectedOrderCount,
            reconcileStatus,
            startedAt,
            OffsetDateTime.now()
        );
    }
}
