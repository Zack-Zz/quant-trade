package com.quanttrade.executor.reconcile;

import com.quanttrade.executor.broker.Broker;
import com.quanttrade.executor.domain.ReconcileResult;

import java.time.LocalDate;

public class DefaultReconcileService implements ReconcileService {
    private final Broker broker;

    public DefaultReconcileService(Broker broker) {
        this.broker = broker;
    }

    @Override
    public ReconcileResult run(LocalDate tradingDate) {
        int brokerOrderCount = broker.queryOrders().size();
        String message = "reconcile completed with broker orders=" + brokerOrderCount;
        return new ReconcileResult(tradingDate, true, message);
    }
}
