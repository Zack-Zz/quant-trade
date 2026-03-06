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
}
