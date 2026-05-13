package com.quanttrade.executor.ledger;

import com.quanttrade.executor.broker.BrokerOrder;
import com.quanttrade.executor.domain.OrderIntent;
import com.quanttrade.executor.domain.ReconcileResult;
import com.quanttrade.executor.domain.RiskDecision;
import com.quanttrade.executor.domain.Signal;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class InMemoryExecutionLedger implements ExecutionLedger {
    private final Set<String> processedKeys = new HashSet<>();
    private final Map<String, Signal> signals = new HashMap<>();
    private final Map<String, RiskDecision> riskDecisions = new HashMap<>();
    private final Map<String, List<OrderIntent>> orderPlans = new HashMap<>();
    private final Map<String, List<BrokerOrder>> ordersByRun = new HashMap<>();
    private final Map<String, ReconcileResult> reconcileResults = new HashMap<>();
    private final Map<String, String> runToSignal = new HashMap<>();
    private final Map<String, String> runByIdempotencyKey = new HashMap<>();
    private final Map<String, String> runStatuses = new HashMap<>();

    @Override
    public synchronized String beginRun(Signal signal, String traceId) {
        String runId = "run-" + (runToSignal.size() + 1);
        runToSignal.put(runId, signal.signalId());
        runByIdempotencyKey.put(signal.idempotencyKey(), runId);
        runStatuses.put(runId, "STARTED");
        return runId;
    }

    @Override
    public synchronized ExecutionClaim claimRun(Signal signal, String traceId) {
        String existingRunId = runByIdempotencyKey.get(signal.idempotencyKey());
        if (existingRunId != null || processedKeys.contains(signal.idempotencyKey())) {
            return new ExecutionClaim(existingRunId, false);
        }
        return new ExecutionClaim(beginRun(signal, traceId), true);
    }

    @Override
    public synchronized boolean isProcessed(String idempotencyKey) {
        return processedKeys.contains(idempotencyKey);
    }

    @Override
    public synchronized void markProcessed(String idempotencyKey) {
        processedKeys.add(idempotencyKey);
    }

    @Override
    public void saveSignal(Signal signal) {
        signals.put(signal.signalId(), signal);
    }

    @Override
    public void saveRiskDecision(String signalId, RiskDecision decision) {
        riskDecisions.put(signalId, decision);
    }

    @Override
    public void markRiskRejected(String runId, RiskDecision decision) {
        runStatuses.put(runId, "RISK_REJECTED");
    }

    @Override
    public void saveOrderPlan(String runId, Signal signal, List<OrderIntent> intents) {
        orderPlans.put(runId, List.copyOf(intents));
    }

    @Override
    public void saveBrokerOrders(String runId, List<BrokerOrder> orders) {
        ordersByRun.put(runId, List.copyOf(orders));
        runStatuses.put(runId, orders.isEmpty() ? "PLANNED" : "SUBMITTED");
    }

    @Override
    public void saveReconcileResult(String runId, ReconcileResult result) {
        reconcileResults.put(runId, result);
        runStatuses.put(runId, result.matched() ? "COMPLETED" : "RECONCILE_MISMATCH");
    }

    public String runStatus(String runId) {
        return runStatuses.get(runId);
    }
}
