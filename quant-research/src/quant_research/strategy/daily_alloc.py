from __future__ import annotations

from dataclasses import dataclass
from datetime import date
from typing import Protocol

from quant_research.models.market import MarketBar


@dataclass(frozen=True)
class StrategyInput:
    """Symbols available to the daily ETF/stock allocation strategy."""

    etf_symbols: list[str]
    stock_symbols: list[str]


@dataclass(frozen=True)
class StrategyContext:
    """Market context used to generate one target-portfolio decision."""

    trading_date: date
    bars: dict[str, MarketBar]
    etf_symbols: list[str]
    stock_symbols: list[str]


@dataclass(frozen=True)
class StrategyDecision:
    """Target weights and explanation produced by a strategy run."""

    strategy_id: str
    trading_date: date
    targets: dict[str, float]
    explanation: dict[str, str | float]


class TargetPortfolioStrategy(Protocol):
    """Strategy interface for target-portfolio signal generation."""

    strategy_id: str

    def generate_decision(self, ctx: StrategyContext) -> StrategyDecision:
        """Generate target weights and explainable diagnostics."""


class DailyEtfStockStrategy:
    """Conservative daily ETF/stock allocation strategy for the MVP."""

    strategy_id = "etf_stock_daily_v1"

    def generate_target_weights(self, ctx: StrategyInput) -> dict[str, float]:
        """Generate static target weights for backward-compatible callers."""
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

    def generate_decision(self, ctx: StrategyContext) -> StrategyDecision:
        """Generate target weights from available, tradable daily bars."""
        etf_symbols = self._tradable_symbols(ctx.etf_symbols, ctx.bars)
        stock_symbols = self._tradable_symbols(ctx.stock_symbols, ctx.bars)
        targets = self.generate_target_weights(StrategyInput(etf_symbols=etf_symbols, stock_symbols=stock_symbols))

        return StrategyDecision(
            strategy_id=self.strategy_id,
            trading_date=ctx.trading_date,
            targets=targets,
            explanation={
                "etf_count": float(len(etf_symbols)),
                "stock_count": float(len(stock_symbols)),
                "target_count": float(len(targets)),
                "style": "daily-balance-etf-stock",
            },
        )

    def _tradable_symbols(self, symbols: list[str], bars: dict[str, MarketBar]) -> list[str]:
        """Filter symbols that have a tradable daily bar for the run date."""
        tradable: list[str] = []
        for symbol in symbols:
            bar = bars.get(symbol)
            if bar is None:
                continue
            # Suspended and limit-locked symbols are excluded because the paper
            # matcher and Java executor should not assume they can be filled.
            if bar.suspended or bar.limit_up or bar.limit_down:
                continue
            tradable.append(symbol)
        return tradable
