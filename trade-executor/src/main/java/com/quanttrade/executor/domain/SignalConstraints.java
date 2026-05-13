package com.quanttrade.executor.domain;

import java.util.List;

public record SignalConstraints(
    double maxTurnoverPct,
    double maxSinglePositionPct,
    double maxDailyLossPct,
    double maxDrawdownPct,
    String rebalance,
    Double minAvgDailyAmount,
    List<String> excludeTags
) {
    public SignalConstraints(
        double maxTurnoverPct,
        double maxSinglePositionPct,
        double maxDailyLossPct,
        double maxDrawdownPct,
        String rebalance,
        Double minAvgDailyAmount
    ) {
        this(maxTurnoverPct, maxSinglePositionPct, maxDailyLossPct, maxDrawdownPct, rebalance, minAvgDailyAmount, List.of());
    }
}
