package com.quanttrade.executor.reconcile;

import com.quanttrade.executor.domain.ReconcileResult;

import java.time.LocalDate;

public interface ReconcileService {
    ReconcileResult run(LocalDate tradingDate);
}
