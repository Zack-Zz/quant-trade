package com.quanttrade.executor.ledger;

import com.quanttrade.executor.broker.BrokerOrder;
import com.quanttrade.executor.domain.RiskDecision;
import com.quanttrade.executor.domain.Signal;

import java.util.List;

public interface ExecutionLedger {
    boolean isProcessed(String idempotencyKey);

    void markProcessed(String idempotencyKey);

    void saveSignal(Signal signal);

    void saveRiskDecision(String signalId, RiskDecision decision);

    void saveOrderPlan(String signalId, List<BrokerOrder> orders);
}
