package com.quanttrade.executor.core;

import com.quanttrade.executor.broker.Broker;
import com.quanttrade.executor.broker.BrokerOrder;
import com.quanttrade.executor.client.SignalClient;
import com.quanttrade.executor.domain.AccountSnapshot;
import com.quanttrade.executor.domain.MarketData;
import com.quanttrade.executor.domain.OrderIntent;
import com.quanttrade.executor.domain.RiskDecision;
import com.quanttrade.executor.domain.Signal;
import com.quanttrade.executor.ledger.ExecutionLedger;
import com.quanttrade.executor.planner.OrderPlanner;
import com.quanttrade.executor.risk.RiskEngine;

import java.util.List;

public class ExecutionOrchestrator {
    private final SignalClient signalClient;
    private final SnapshotProvider snapshotProvider;
    private final MarketDataProvider marketDataProvider;
    private final RiskEngine riskEngine;
    private final OrderPlanner orderPlanner;
    private final Broker broker;
    private final ExecutionLedger ledger;

    public ExecutionOrchestrator(
        SignalClient signalClient,
        SnapshotProvider snapshotProvider,
        MarketDataProvider marketDataProvider,
        RiskEngine riskEngine,
        OrderPlanner orderPlanner,
        Broker broker,
        ExecutionLedger ledger
    ) {
        this.signalClient = signalClient;
        this.snapshotProvider = snapshotProvider;
        this.marketDataProvider = marketDataProvider;
        this.riskEngine = riskEngine;
        this.orderPlanner = orderPlanner;
        this.broker = broker;
        this.ledger = ledger;
    }

    public ExecutionReport runOnce(String accountId) {
        Signal signal = signalClient.fetchLatest(accountId);
        if (ledger.isProcessed(signal.idempotencyKey())) {
            return new ExecutionReport(true, "idempotency-hit", 0, null);
        }

        ledger.saveSignal(signal);
        AccountSnapshot snapshot = snapshotProvider.current(accountId);
        RiskDecision riskDecision = riskEngine.evaluate(signal, snapshot);
        ledger.saveRiskDecision(signal.signalId(), riskDecision);

        if (!riskDecision.approved()) {
            ledger.markProcessed(signal.idempotencyKey());
            return new ExecutionReport(true, "risk-rejected", 0, riskDecision);
        }

        MarketData marketData = marketDataProvider.latest(signal.universe());
        List<OrderIntent> intents = orderPlanner.plan(signal, snapshot, marketData);
        List<BrokerOrder> placed = broker.placeOrders(intents);
        ledger.saveOrderPlan(signal.signalId(), placed);
        ledger.markProcessed(signal.idempotencyKey());

        return new ExecutionReport(false, "executed", placed.size(), riskDecision);
    }
}
