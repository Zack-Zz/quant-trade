package com.quanttrade.executor.domain;

import java.util.List;

public record RiskDecision(boolean approved, List<String> messages, double maxTurnoverPct) {
}
