package com.quanttrade.executor.risk;

import com.quanttrade.executor.domain.AccountSnapshot;
import com.quanttrade.executor.domain.RiskDecision;
import com.quanttrade.executor.domain.Signal;

public interface RiskEngine {
    RiskDecision evaluate(Signal signal, AccountSnapshot snapshot);
}
