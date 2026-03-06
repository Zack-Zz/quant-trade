package com.quanttrade.executor.planner;

import com.quanttrade.executor.domain.AccountSnapshot;
import com.quanttrade.executor.domain.MarketData;
import com.quanttrade.executor.domain.OrderIntent;
import com.quanttrade.executor.domain.Signal;

import java.util.List;

public interface OrderPlanner {
    List<OrderIntent> plan(Signal signal, AccountSnapshot snapshot, MarketData marketData);
}
