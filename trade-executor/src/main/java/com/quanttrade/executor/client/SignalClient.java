package com.quanttrade.executor.client;

import com.quanttrade.executor.domain.Signal;

public interface SignalClient {
    Signal fetchLatest(String accountId);
}
