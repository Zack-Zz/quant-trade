package com.quanttrade.executor.planner;

import com.quanttrade.executor.domain.AccountSnapshot;
import com.quanttrade.executor.domain.MarketData;
import com.quanttrade.executor.domain.OrderIntent;
import com.quanttrade.executor.domain.OrderSide;
import com.quanttrade.executor.domain.PortfolioTarget;
import com.quanttrade.executor.domain.Position;
import com.quanttrade.executor.domain.Signal;
import com.quanttrade.executor.domain.SignalConstraints;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultOrderPlannerTest {
    private final DefaultOrderPlanner planner = new DefaultOrderPlanner();

    @Test
    void createsLotSizedBuyOrders() {
        Signal signal = sampleSignal();
        AccountSnapshot snapshot = new AccountSnapshot(
            "acct",
            1_000_000,
            300_000,
            Map.of("510300.SH", new Position("510300.SH", 1_000, 3.0)),
            Set.of(),
            0,
            0
        );
        MarketData marketData = new MarketData(Map.of("510300.SH", 4.0, "000333.SZ", 20.0));

        List<OrderIntent> intents = planner.plan(signal, snapshot, marketData);

        assertEquals(2, intents.size());
        assertTrue(intents.stream().allMatch(intent -> intent.quantity() % 100 == 0));
    }

    @Test
    void blocksSameDaySellForT1() {
        Signal signal = sampleSignal();
        AccountSnapshot snapshot = new AccountSnapshot(
            "acct",
            1_000_000,
            300_000,
            Map.of("510300.SH", new Position("510300.SH", 80_000, 4.0), "000333.SZ", new Position("000333.SZ", 2_000, 20.0)),
            Set.of("510300.SH"),
            0,
            0
        );
        MarketData marketData = new MarketData(Map.of("510300.SH", 4.0, "000333.SZ", 20.0));

        List<OrderIntent> intents = planner.plan(signal, snapshot, marketData);

        assertTrue(intents.stream().noneMatch(intent -> intent.symbol().equals("510300.SH") && intent.side() == OrderSide.SELL));
    }

    @Test
    void placesSellOrdersBeforeBuysSoCashIsReleasedFirst() {
        Signal signal = sampleSignal();
        AccountSnapshot snapshot = new AccountSnapshot(
            "acct",
            1_000_000,
            50_000,
            Map.of("510300.SH", new Position("510300.SH", 100_000, 3.0)),
            Set.of(),
            0,
            0
        );
        MarketData marketData = new MarketData(Map.of("510300.SH", 4.0, "000333.SZ", 20.0));

        List<OrderIntent> intents = planner.plan(signal, snapshot, marketData);

        assertEquals(OrderSide.SELL, intents.get(0).side());
        assertEquals(OrderSide.BUY, intents.get(1).side());
    }

    @Test
    void scalesBuysToAvailableCashAfterExpectedSells() {
        Signal signal = sampleSignal();
        AccountSnapshot snapshot = new AccountSnapshot(
            "acct",
            1_000_000,
            10_000,
            Map.of("510300.SH", new Position("510300.SH", 90_000, 3.0)),
            Set.of(),
            0,
            0
        );
        MarketData marketData = new MarketData(Map.of("510300.SH", 4.0, "000333.SZ", 20.0));

        List<OrderIntent> intents = planner.plan(signal, snapshot, marketData);

        double buyNotional = intents.stream()
            .filter(intent -> intent.side() == OrderSide.BUY)
            .mapToDouble(intent -> intent.quantity() * intent.limitPrice())
            .sum();
        double sellNotional = intents.stream()
            .filter(intent -> intent.side() == OrderSide.SELL)
            .mapToDouble(intent -> intent.quantity() * intent.limitPrice())
            .sum();
        assertTrue(buyNotional <= 10_000 + sellNotional);
    }

    private Signal sampleSignal() {
        return new Signal(
            "1.0.0",
            "sig-1",
            "acct",
            OffsetDateTime.parse("2026-03-06T15:00:00+08:00"),
            "strategy",
            List.of("510300.SH", "000333.SZ"),
            0.12,
            List.of(
                new PortfolioTarget("510300.SH", 0.20, 0.7, "trend"),
                new PortfolioTarget("000333.SZ", 0.30, 0.6, "quality")
            ),
            new SignalConstraints(0.18, 0.4, 0.01, 0.08, "DAILY_CLOSE", 100000000.0),
            null,
            "strategy|2026-03-06T15:00:00+08:00|acct"
        );
    }
}
