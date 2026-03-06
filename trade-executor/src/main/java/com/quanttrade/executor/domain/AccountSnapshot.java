package com.quanttrade.executor.domain;

import java.util.Map;
import java.util.Set;

public record AccountSnapshot(
    String accountId,
    double totalEquity,
    double availableCash,
    Map<String, Position> positions,
    Set<String> todayBoughtSymbols,
    double dailyPnlPct,
    double drawdownPct
) {
}
