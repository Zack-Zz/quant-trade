package com.quanttrade.executor.core;

import com.quanttrade.executor.domain.MarketData;

import java.util.List;

public interface MarketDataProvider {
    MarketData latest(List<String> symbols);
}
