package com.quanttrade.executor.planner;

import com.quanttrade.executor.domain.AccountSnapshot;
import com.quanttrade.executor.domain.MarketData;
import com.quanttrade.executor.domain.OrderIntent;
import com.quanttrade.executor.domain.OrderSide;
import com.quanttrade.executor.domain.PortfolioTarget;
import com.quanttrade.executor.domain.Position;
import com.quanttrade.executor.domain.Signal;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class DefaultOrderPlanner implements OrderPlanner {
    private static final int LOT_SIZE = 100;

    @Override
    public List<OrderIntent> plan(Signal signal, AccountSnapshot snapshot, MarketData marketData) {
        List<RawIntent> raw = new ArrayList<>();

        for (PortfolioTarget target : signal.targets()) {
            Double price = marketData.prices().get(target.symbol());
            if (price == null || price <= 0) {
                continue;
            }

            Position currentPosition = snapshot.positions().get(target.symbol());
            int currentQty = currentPosition == null ? 0 : currentPosition.quantity();
            double currentValue = currentQty * price;
            double desiredValue = snapshot.totalEquity() * target.targetPct();
            double deltaValue = desiredValue - currentValue;

            if (Math.abs(deltaValue) < price * LOT_SIZE) {
                continue;
            }

            OrderSide side = deltaValue >= 0 ? OrderSide.BUY : OrderSide.SELL;
            if (side == OrderSide.SELL && snapshot.todayBoughtSymbols().contains(target.symbol())) {
                continue;
            }

            int shares = toLot(Math.abs(deltaValue) / price);
            if (shares <= 0) {
                continue;
            }

            raw.add(new RawIntent(target.symbol(), side, shares, price, Math.abs(deltaValue)));
        }

        List<OrderIntent> intents = applyTurnoverLimit(raw, snapshot.totalEquity(), signal.constraints().maxTurnoverPct());
        return applyCashProtection(sortSellBeforeBuy(intents), snapshot.availableCash());
    }

    private List<OrderIntent> applyTurnoverLimit(List<RawIntent> raw, double equity, double maxTurnoverPct) {
        List<OrderIntent> intents = new ArrayList<>();
        double totalTurnover = raw.stream().mapToDouble(RawIntent::tradeValue).sum();
        double allowedTurnover = equity * maxTurnoverPct;
        double scale = totalTurnover > 0 ? Math.min(1.0, allowedTurnover / totalTurnover) : 1.0;

        for (RawIntent intent : raw) {
            int scaledShares = toLot(intent.shares() * scale);
            if (scaledShares <= 0) {
                continue;
            }

            double limitPrice = intent.side() == OrderSide.BUY
                ? intent.price() * 1.002
                : intent.price() * 0.998;

            intents.add(new OrderIntent(intent.symbol(), intent.side(), scaledShares, limitPrice));
        }
        return intents;
    }

    private List<OrderIntent> sortSellBeforeBuy(List<OrderIntent> intents) {
        return intents.stream()
            .sorted(Comparator.comparingInt(intent -> intent.side() == OrderSide.SELL ? 0 : 1))
            .toList();
    }

    private List<OrderIntent> applyCashProtection(List<OrderIntent> intents, double availableCash) {
        double cashBudget = availableCash;
        List<OrderIntent> protectedIntents = new ArrayList<>();

        for (OrderIntent intent : intents) {
            if (intent.side() == OrderSide.SELL) {
                protectedIntents.add(intent);
                // Planner assumes sell proceeds are available before buys; broker still enforces real cash.
                cashBudget += intent.quantity() * intent.limitPrice();
                continue;
            }

            int affordableShares = toLot(cashBudget / intent.limitPrice());
            int quantity = Math.min(intent.quantity(), affordableShares);
            if (quantity <= 0) {
                continue;
            }
            protectedIntents.add(new OrderIntent(intent.symbol(), intent.side(), quantity, intent.limitPrice()));
            cashBudget -= quantity * intent.limitPrice();
        }

        return protectedIntents;
    }

    private int toLot(double rawShares) {
        return (int) (Math.floor(rawShares / LOT_SIZE) * LOT_SIZE);
    }

    private record RawIntent(String symbol, OrderSide side, int shares, double price, double tradeValue) {
    }
}
