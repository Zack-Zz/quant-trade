package com.quanttrade.executor.core;

import com.quanttrade.executor.domain.AccountSnapshot;

public interface SnapshotProvider {
    AccountSnapshot current(String accountId);
}
