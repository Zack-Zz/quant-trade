package com.quanttrade.executor.core;

import com.quanttrade.executor.domain.RiskDecision;

public record ExecutionReport(boolean skipped, String reason, int orderCount, RiskDecision riskDecision) {
}
