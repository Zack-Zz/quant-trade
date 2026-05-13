package com.quanttrade.executor.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

public record Signal(
    @JsonProperty("schema_version")
    String schemaVersion,
    @JsonProperty("signal_id")
    String signalId,
    @JsonProperty("account_id")
    String accountId,
    @JsonProperty("trading_date")
    LocalDate tradingDate,
    @JsonProperty("as_of")
    OffsetDateTime asOf,
    @JsonProperty("strategy_id")
    String strategyId,
    @JsonProperty("strategy_version")
    String strategyVersion,
    @JsonProperty("rebalance_cycle")
    String rebalanceCycle,
    @JsonProperty("data_version")
    String dataVersion,
    String status,
    List<String> universe,
    @JsonProperty("cash_target_pct")
    double cashTargetPct,
    List<PortfolioTarget> targets,
    SignalConstraints constraints,
    String checksum,
    @JsonProperty("idempotency_key")
    String idempotencyKey,
    Map<String, Object> metadata
) {
    public Signal(
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
        this(
            schemaVersion,
            signalId,
            accountId,
            null,
            asOf,
            strategyId,
            null,
            null,
            null,
            null,
            universe,
            cashTargetPct,
            targets,
            constraints,
            checksum,
            idempotencyKey,
            Map.of()
        );
    }
}
