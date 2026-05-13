"""Application service for Web-facing research workflows."""

from __future__ import annotations

from datetime import UTC, date, datetime
from uuid import uuid4

from quant_research.backtest.engine import BacktestEngine, BacktestRequest
from quant_research.analysis.market import MarketAnalyzer
from quant_research.analysis.stock import StockAnalyzer
from quant_research.data.market_provider import CsvMarketDataProvider, MarketDataProvider
from quant_research.models.trading import PaperAccount, StrategyRun
from quant_research.paper.simulator import DailyBarSimulator, SimulatorState
from quant_research.services.signal_service import SignalService
from quant_research.storage.sqlite_repository import ResearchRepository
from quant_research.strategy.daily_alloc import DailyEtfStockStrategy, StrategyContext


class ResearchService:
    """Coordinate research, backtest, paper trading, and signal workflows."""

    def __init__(
        self,
        market_data: MarketDataProvider | None = None,
        repository: ResearchRepository | None = None,
    ) -> None:
        """Create an application service with local defaults for v1."""
        self.market_data = market_data or CsvMarketDataProvider()
        self.repository = repository or ResearchRepository()
        self.strategy = DailyEtfStockStrategy()
        self.signal_service = SignalService(self.market_data)
        self.market_analyzer = MarketAnalyzer(self.market_data)
        self.stock_analyzer = StockAnalyzer(self.market_data)

    def overview(self) -> dict:
        """Return chart-ready system overview data for the Web home page."""
        status = self.market_data.status()
        self.repository.record_data_status(status.model_dump(mode="json"))
        latest_backtest = next(iter(self.repository.list_backtest_runs(limit=1)), None)
        latest_signal = next(iter(self.repository.list_signals(limit=1)), None)
        paper_account = self.get_paper_account("acct-main-a").model_dump(mode="json")
        return {
            "data_status": status.model_dump(mode="json"),
            "latest_backtest": latest_backtest,
            "latest_signal": latest_signal,
            "paper_account": paper_account,
        }

    def run_strategy(self, trading_date: date | None = None) -> StrategyRun:
        """Run the default strategy for a trading date and persist the decision."""
        status = self.market_data.status()
        selected_date = trading_date or status.latest_trading_date
        if selected_date is None:
            raise ValueError("no market data is available for strategy run")

        bars = {bar.symbol: bar for bar in self.market_data.get_bars(start=selected_date, end=selected_date)}
        instruments = self.market_data.list_instruments()
        ctx = StrategyContext(
            trading_date=selected_date,
            bars=bars,
            etf_symbols=[item.symbol for item in instruments if item.asset_type == "ETF"],
            stock_symbols=[item.symbol for item in instruments if item.asset_type == "STOCK"],
        )
        decision = self.strategy.generate_decision(ctx)
        run = StrategyRun(
            run_id=f"sr-{uuid4().hex[:12]}",
            strategy_id=decision.strategy_id,
            trading_date=decision.trading_date,
            targets=decision.targets,
            explanation=decision.explanation,
            created_at=datetime.now(UTC).replace(microsecond=0),
        )
        self.repository.save_strategy_run(run.run_id, run.model_dump(mode="json"))
        return run

    def list_strategy_runs(self) -> list[dict]:
        """Return recent strategy runs for Web tables."""
        return self.repository.list_strategy_runs()

    def market_analysis(self) -> dict:
        """Return market regime, breadth, liquidity, and volatility analysis."""
        return self.market_analyzer.analyze()

    def stock_analysis(self) -> dict:
        """Return ranked stock candidates with filter explanations."""
        return self.stock_analyzer.analyze()

    def data_quality(self) -> dict:
        """Return local market-data quality diagnostics."""
        return self.market_analyzer.data_quality()

    def get_strategy_run(self, run_id: str) -> dict | None:
        """Return one strategy run by id."""
        return self.repository.get_strategy_run(run_id)

    def run_backtest(self, request: BacktestRequest | None = None) -> dict:
        """Run and persist a daily-bar backtest."""
        status = self.market_data.status()
        end_date = status.latest_trading_date
        start_date = min((bar.trading_date for bar in self.market_data.get_bars()), default=end_date)
        if start_date is None or end_date is None:
            raise ValueError("no market data is available for backtest")

        actual_request = request or BacktestRequest(start_date=start_date, end_date=end_date)
        result = BacktestEngine(self.market_data, self.strategy).run(actual_request)
        payload = result.model_dump(mode="json")
        self.repository.save_backtest_run(result.run_id, payload)
        return payload

    def get_backtest_run(self, run_id: str) -> dict | None:
        """Return one backtest run by id."""
        return self.repository.get_backtest_run(run_id)

    def get_paper_account(self, account_id: str) -> PaperAccount:
        """Return current paper account, creating a default account if needed."""
        existing = self.repository.get_paper_account(account_id)
        if existing is not None:
            return PaperAccount.model_validate(existing)
        account = self._empty_paper_account(account_id)
        self.repository.save_paper_account(account_id, account.model_dump(mode="json"))
        return account

    def run_signal_for_paper_account(self, account_id: str) -> PaperAccount:
        """Generate a signal and apply it to the paper account."""
        account = self.get_paper_account(account_id)
        signal = self.signal_service.create_latest_signal(account_id)
        latest_bars = self.market_data.latest_bars(signal.universe)
        trading_date = max(bar.trading_date for bar in latest_bars.values())
        targets = {target.symbol: target.target_pct for target in signal.portfolio.targets}

        state = SimulatorState(
            cash=account.cash,
            positions={position.symbol: position.quantity for position in account.positions},
            orders=list(account.orders),
            fills=list(account.fills),
            equity_curve=list(account.equity_curve),
        )
        simulator = DailyBarSimulator()
        simulator.apply_targets(state, trading_date, latest_bars, targets)

        updated = PaperAccount(
            account_id=account_id,
            cash=round(state.cash, 2),
            equity=round(simulator.mark_to_market(state, latest_bars), 2),
            positions=simulator.position_snapshots(state, latest_bars),
            orders=state.orders,
            fills=state.fills,
            equity_curve=state.equity_curve,
            updated_at=datetime.now(UTC).replace(microsecond=0),
        )
        self.repository.save_paper_account(account_id, updated.model_dump(mode="json"))
        self.repository.save_signal(signal.signal_id, signal.model_dump(mode="json"))
        return updated

    def create_signal(self, account_id: str, schema_version: str = "2.0.0") -> dict:
        """Create and persist a target-portfolio signal."""
        signal = self.signal_service.create_latest_signal(account_id, schema_version=schema_version)
        payload = signal.model_dump(mode="json")
        self.repository.save_signal(signal.signal_id, payload)
        return payload

    def latest_signal(self, account_id: str) -> dict | None:
        """Return the latest published signal for one account."""
        for signal in self.repository.list_signals(limit=100):
            if signal.get("account_id") != account_id:
                continue
            if signal.get("schema_version") == "2.0.0" and signal.get("status") != "PUBLISHED":
                continue
            return signal
        return None

    def list_signals(self) -> list[dict]:
        """Return recent generated signals."""
        return self.repository.list_signals()

    def get_signal(self, signal_id: str) -> dict | None:
        """Return one generated signal by id."""
        return self.repository.get_signal(signal_id)

    def _empty_paper_account(self, account_id: str) -> PaperAccount:
        """Create the default local paper account used by the Web dashboard."""
        return PaperAccount(
            account_id=account_id,
            cash=1_000_000.0,
            equity=1_000_000.0,
            positions=[],
            orders=[],
            fills=[],
            equity_curve=[],
            updated_at=datetime.now(UTC).replace(microsecond=0),
        )
