package com.quanttrade.executor.domain;

public record SignalConstraints(
    double maxTurnoverPct,
    double maxSinglePositionPct,
    double maxDailyLossPct,
    double maxDrawdownPct,
    String rebalance,
    Double minAvgDailyAmount
) {
}
