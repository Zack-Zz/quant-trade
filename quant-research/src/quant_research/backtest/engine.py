from __future__ import annotations

from dataclasses import dataclass


@dataclass(frozen=True)
class BacktestMetrics:
    total_return_pct: float
    max_drawdown_pct: float
    annual_turnover_pct: float


class BacktestEngine:
    """Minimal deterministic backtest shell for pipeline integration."""

    def run_smoke(self) -> BacktestMetrics:
        return BacktestMetrics(
            total_return_pct=0.087,
            max_drawdown_pct=0.045,
            annual_turnover_pct=0.63,
        )
