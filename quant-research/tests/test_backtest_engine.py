from datetime import date

from quant_research.backtest.engine import BacktestEngine, BacktestRequest
from quant_research.data.market_provider import CsvMarketDataProvider


def test_backtest_engine_runs_daily_bar_simulation() -> None:
    """Backtest should create chart-ready metrics, equity, orders, and fills."""
    engine = BacktestEngine(CsvMarketDataProvider())

    result = engine.run(
        BacktestRequest(
            start_date=date(2026, 3, 2),
            end_date=date(2026, 3, 6),
            initial_cash=1_000_000,
        )
    )

    assert result.metrics.trade_count > 0
    assert result.equity_curve[-1].equity > 0
    assert result.positions
    assert result.fills[0].commission >= 5.0


def test_backtest_smoke_returns_metrics() -> None:
    """Compatibility smoke call should return non-empty metrics."""
    metrics = BacktestEngine(CsvMarketDataProvider()).run_smoke()

    assert metrics.trade_count > 0
    assert metrics.annual_turnover_pct > 0
