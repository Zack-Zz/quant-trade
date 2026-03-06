from __future__ import annotations

from dataclasses import dataclass


@dataclass(frozen=True)
class StrategyInput:
    etf_symbols: list[str]
    stock_symbols: list[str]


class DailyEtfStockStrategy:
    strategy_id = "etf_stock_daily_v1"

    def generate_target_weights(self, ctx: StrategyInput) -> dict[str, float]:
        if not ctx.etf_symbols and not ctx.stock_symbols:
            return {}

        weights: dict[str, float] = {}
        etf_budget = 0.62
        stock_budget = 0.26

        if ctx.etf_symbols:
            etf_each = etf_budget / len(ctx.etf_symbols)
            for symbol in ctx.etf_symbols:
                weights[symbol] = round(etf_each, 4)

        if ctx.stock_symbols:
            stock_each = stock_budget / len(ctx.stock_symbols)
            for symbol in ctx.stock_symbols:
                weights[symbol] = round(stock_each, 4)

        return weights
