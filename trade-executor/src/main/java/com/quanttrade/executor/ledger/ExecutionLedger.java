package com.quanttrade.executor.ledger;

import com.quanttrade.executor.broker.BrokerOrder;
import com.quanttrade.executor.domain.OrderIntent;
import com.quanttrade.executor.domain.ReconcileResult;
import com.quanttrade.executor.domain.RiskDecision;
import com.quanttrade.executor.domain.Signal;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Durable boundary for execution audit, idempotency, and recovery.
 *
 * @author quant-trade maintainers
 * @since 0.2.0
 */
public interface ExecutionLedger {
    default String beginRun(Signal signal, String traceId) {
        return "run-" + UUID.randomUUID();
    }

    default ExecutionClaim claimRun(Signal signal, String traceId) {
        if (isProcessed(signal.idempotencyKey())) {
            return new ExecutionClaim(null, false);
        }
        return new ExecutionClaim(beginRun(signal, traceId), true);
    }

    boolean isProcessed(String idempotencyKey);

    void markProcessed(String idempotencyKey);

    void saveSignal(Signal signal);

    void saveRiskDecision(String signalId, RiskDecision decision);

    default void markRiskRejected(String runId, RiskDecision decision) {
    }

    void saveOrderPlan(String runId, Signal signal, List<OrderIntent> intents);

    default void saveBrokerOrders(String runId, List<BrokerOrder> orders) {
    }

    default void saveReconcileResult(String runId, ReconcileResult result) {
    }

    default List<String> findRecoverableRuns() {
        return Collections.emptyList();
    }
}
