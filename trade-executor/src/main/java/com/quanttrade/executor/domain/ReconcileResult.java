package com.quanttrade.executor.domain;

import java.time.LocalDate;

public record ReconcileResult(LocalDate tradingDate, boolean matched, String message) {
}
