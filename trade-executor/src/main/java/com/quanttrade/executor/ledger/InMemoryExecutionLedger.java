package com.quanttrade.executor.ledger;

import com.quanttrade.executor.broker.BrokerOrder;
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
    private final Map<String, List<BrokerOrder>> orderPlans = new HashMap<>();

    @Override
    public boolean isProcessed(String idempotencyKey) {
        return processedKeys.contains(idempotencyKey);
    }

    @Override
    public void markProcessed(String idempotencyKey) {
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
    public void saveOrderPlan(String signalId, List<BrokerOrder> orders) {
        orderPlans.put(signalId, orders);
    }
}
