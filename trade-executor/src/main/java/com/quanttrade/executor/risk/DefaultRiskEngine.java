package com.quanttrade.executor.risk;

import com.quanttrade.executor.domain.AccountSnapshot;
import com.quanttrade.executor.domain.PortfolioTarget;
import com.quanttrade.executor.domain.RiskDecision;
import com.quanttrade.executor.domain.Signal;

import java.util.ArrayList;
import java.util.List;

public class DefaultRiskEngine implements RiskEngine {
    @Override
    public RiskDecision evaluate(Signal signal, AccountSnapshot snapshot) {
        List<String> messages = new ArrayList<>();

        validateSignalContract(signal, messages);

        for (PortfolioTarget target : signal.targets()) {
            if (target.targetPct() > signal.constraints().maxSinglePositionPct()) {
                messages.add("target exceeds max single position: " + target.symbol());
            }
        }

        if (snapshot.dailyPnlPct() <= -signal.constraints().maxDailyLossPct()) {
            messages.add("daily loss circuit breaker triggered");
        }

        if (snapshot.drawdownPct() >= signal.constraints().maxDrawdownPct()) {
            messages.add("max drawdown circuit breaker triggered");
        }

        boolean approved = messages.isEmpty();
        return new RiskDecision(approved, messages, signal.constraints().maxTurnoverPct());
    }

    private void validateSignalContract(Signal signal, List<String> messages) {
        if ("2.0.0".equals(signal.schemaVersion())) {
            if (!"PUBLISHED".equals(signal.status())) {
                messages.add("signal v2 is not published");
            }
            if (signal.tradingDate() == null || isBlank(signal.strategyVersion()) || isBlank(signal.rebalanceCycle())) {
                messages.add("signal v2 missing stable identity fields");
                return;
            }
            String expectedKey = String.join(
                "|",
                signal.strategyId(),
                signal.strategyVersion(),
                signal.accountId(),
                signal.tradingDate().toString(),
                signal.rebalanceCycle()
            );
            if (!expectedKey.equals(signal.idempotencyKey())) {
                messages.add("signal v2 idempotency key mismatch");
            }
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
