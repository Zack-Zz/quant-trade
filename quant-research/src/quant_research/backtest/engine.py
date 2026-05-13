"""Daily-frequency backtest engine for target-portfolio strategies."""

from __future__ import annotations

from datetime import UTC, date, datetime
from uuid import uuid4

from pydantic import BaseModel, Field

from quant_research.data.market_provider import CsvMarketDataProvider, MarketDataProvider
from quant_research.models.market import MarketBar
from quant_research.models.trading import BacktestMetrics, BacktestRun
from quant_research.paper.simulator import DailyBarSimulator, SimulatorState
from quant_research.strategy.daily_alloc import DailyEtfStockStrategy, StrategyContext, TargetPortfolioStrategy


class BacktestRequest(BaseModel):
    """Request used to run a local daily-bar backtest."""

    strategy_id: str = "etf_stock_daily_v1"
    symbols: list[str] = Field(default_factory=lambda: ["510300.SH", "159915.SZ", "600519.SH", "000333.SZ"])
    start_date: date
    end_date: date
    initial_cash: float = Field(default=1_000_000.0, gt=0)


class BacktestEngine:
    """Run deterministic daily backtests against the market-data provider."""

    def __init__(
        self,
        market_data: MarketDataProvider | None = None,
        strategy: TargetPortfolioStrategy | None = None,
        simulator: DailyBarSimulator | None = None,
    ) -> None:
        """Create a backtest engine with injectable data and strategy dependencies."""
        self.market_data = market_data or CsvMarketDataProvider()
        self.strategy = strategy or DailyEtfStockStrategy()
        self.simulator = simulator or DailyBarSimulator()

    def run(self, request: BacktestRequest) -> BacktestRun:
        """Run a backtest and return metrics plus chart-ready details."""
        bars = self.market_data.get_bars(start=request.start_date, end=request.end_date)
        bars_by_date = self._group_bars_by_date(bars)
        state = SimulatorState(cash=request.initial_cash)

        for trading_date in sorted(bars_by_date):
            day_bars = bars_by_date[trading_date]
            ctx = StrategyContext(
                trading_date=trading_date,
                bars=day_bars,
                etf_symbols=[symbol for symbol in request.symbols if symbol in {"510300.SH", "159915.SZ"}],
                stock_symbols=[symbol for symbol in request.symbols if symbol not in {"510300.SH", "159915.SZ"}],
            )
            decision = self.strategy.generate_decision(ctx)
            self.simulator.apply_targets(state, trading_date, day_bars, decision.targets)

        latest_bars = bars_by_date[max(bars_by_date)] if bars_by_date else {}
        metrics = self._calculate_metrics(state, request.initial_cash)
        return BacktestRun(
            run_id=f"bt-{uuid4().hex[:12]}",
            strategy_id=request.strategy_id,
            start_date=request.start_date,
            end_date=request.end_date,
            initial_cash=request.initial_cash,
            metrics=metrics,
            equity_curve=state.equity_curve,
            orders=state.orders,
            fills=state.fills,
            positions=self.simulator.position_snapshots(state, latest_bars),
            created_at=datetime.now(UTC).replace(microsecond=0),
        )

    def run_smoke(self) -> BacktestMetrics:
        """Run a small bundled-data smoke backtest for compatibility."""
        status = self.market_data.status()
        end_date = status.latest_trading_date or date.today()
        start_date = min((bar.trading_date for bar in self.market_data.get_bars()), default=end_date)
        return self.run(BacktestRequest(start_date=start_date, end_date=end_date)).metrics

    def _group_bars_by_date(self, bars: list[MarketBar]) -> dict[date, dict[str, MarketBar]]:
        """Group market bars by trading date and symbol for fast simulation lookup."""
        grouped: dict[date, dict[str, MarketBar]] = {}
        for bar in bars:
            grouped.setdefault(bar.trading_date, {})[bar.symbol] = bar
        return grouped

    def _calculate_metrics(self, state: SimulatorState, initial_cash: float) -> BacktestMetrics:
        """Calculate headline metrics from a finished simulator state."""
        if not state.equity_curve:
            return BacktestMetrics(total_return_pct=0.0, max_drawdown_pct=0.0, annual_turnover_pct=0.0, trade_count=0)

        final_equity = state.equity_curve[-1].equity
        total_return = (final_equity - initial_cash) / initial_cash
        max_drawdown = max((point.drawdown_pct for point in state.equity_curve), default=0.0)
        turnover_value = sum(fill.quantity * fill.price for fill in state.fills)
        annual_turnover = turnover_value / initial_cash
        return BacktestMetrics(
            total_return_pct=round(total_return, 6),
            max_drawdown_pct=round(max_drawdown, 6),
            annual_turnover_pct=round(annual_turnover, 6),
            trade_count=len(state.fills),
        )
