package com.quanttrade.executor.risk;

import com.quanttrade.executor.domain.AccountSnapshot;
import com.quanttrade.executor.domain.PortfolioTarget;
import com.quanttrade.executor.domain.Position;
import com.quanttrade.executor.domain.Signal;
import com.quanttrade.executor.domain.SignalConstraints;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultRiskEngineTest {
    private final DefaultRiskEngine riskEngine = new DefaultRiskEngine();

    @Test
    void rejectsWhenDailyLossTriggered() {
        Signal signal = sampleSignal(0.6);
        AccountSnapshot snapshot = new AccountSnapshot("acct", 1_000_000, 100_000, Map.of(), Set.of(), -0.02, 0.01);

        var decision = riskEngine.evaluate(signal, snapshot);
        assertFalse(decision.approved());
    }

    @Test
    void approvesWhenWithinRiskLimits() {
        Signal signal = sampleSignal(0.6);
        AccountSnapshot snapshot = new AccountSnapshot("acct", 1_000_000, 100_000, Map.of(), Set.of(), -0.001, 0.02);

        var decision = riskEngine.evaluate(signal, snapshot);
        assertTrue(decision.approved());
    }

    private Signal sampleSignal(double singleLimit) {
        return new Signal(
            "1.0.0",
            "sig-1",
            "acct",
            OffsetDateTime.parse("2026-03-06T15:00:00+08:00"),
            "strategy",
            List.of("510300.SH"),
            0.2,
            List.of(new PortfolioTarget("510300.SH", 0.5, 0.7, "trend")),
            new SignalConstraints(0.2, singleLimit, 0.01, 0.08, "DAILY_CLOSE", 100000000.0),
            null,
            "strategy|2026-03-06T15:00:00+08:00|acct"
        );
    }
}
