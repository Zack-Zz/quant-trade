package com.quanttrade.executor.domain;

import java.time.OffsetDateTime;
import java.util.List;

public record Signal(
    String schemaVersion,
    String signalId,
    String accountId,
    OffsetDateTime asOf,
    String strategyId,
    List<String> universe,
    double cashTargetPct,
    List<PortfolioTarget> targets,
    SignalConstraints constraints,
    String checksum,
    String idempotencyKey
) {
}
