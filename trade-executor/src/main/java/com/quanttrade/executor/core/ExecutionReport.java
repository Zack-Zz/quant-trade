package com.quanttrade.executor.core;

import com.quanttrade.executor.domain.RiskDecision;

import java.time.OffsetDateTime;

/**
 * Summary returned after one executor run.
 *
 * @author quant-trade maintainers
 * @since 0.2.0
 */
public record ExecutionReport(
    boolean skipped,
    String reason,
    int orderCount,
    RiskDecision riskDecision,
    String runId,
    String traceId,
    String signalId,
    String idempotencyKey,
    String status,
    int plannedOrderCount,
    int submittedOrderCount,
    int filledOrderCount,
    int rejectedOrderCount,
    String reconcileStatus,
    OffsetDateTime startedAt,
    OffsetDateTime finishedAt
) {
    public ExecutionReport(boolean skipped, String reason, int orderCount, RiskDecision riskDecision) {
        this(
            skipped,
            reason,
            orderCount,
            riskDecision,
            null,
            null,
            null,
            null,
            reason,
            orderCount,
            orderCount,
            0,
            0,
            null,
            null,
            null
        );
    }
}
