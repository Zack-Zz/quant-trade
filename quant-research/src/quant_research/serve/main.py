from __future__ import annotations

from datetime import date
from typing import Any

from fastapi import FastAPI, Query
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field

from quant_research.models.signal import ApiEnvelope
from quant_research.services.research_service import ResearchService
from quant_research.services.signal_service import SignalService

app = FastAPI(title="quant-research-signal-service", version="0.1.0")
app.add_middleware(
    CORSMiddleware,
    allow_origins=["http://localhost:5173", "http://127.0.0.1:5173"],
    allow_credentials=False,
    allow_methods=["*"],
    allow_headers=["*"],
)

research_service = ResearchService()
signal_service = SignalService(research_service.market_data)


class BacktestRunRequest(BaseModel):
    """Optional API request body for launching a backtest run."""

    start_date: date | None = None
    end_date: date | None = None
    initial_cash: float = Field(default=1_000_000.0, gt=0)


class StrategyRunRequest(BaseModel):
    """Optional API request body for running the default strategy."""

    trading_date: date | None = None


@app.get("/health", response_model=ApiEnvelope)
def health() -> ApiEnvelope:
    """Return service health status."""
    return ApiEnvelope(success=True, data={"status": "ok"})


@app.get("/version", response_model=ApiEnvelope)
def version() -> ApiEnvelope:
    """Return service and contract versions."""
    return ApiEnvelope(
        success=True,
        data={
            "service_version": "0.1.0",
            "strategy_version": "etf_stock_daily_v1",
            "schema_version": "1.0.0",
            "data_version": "broker-daily",
        },
    )


@app.get("/signal", response_model=ApiEnvelope)
def signal(account_id: str = Query(min_length=1, max_length=64)) -> ApiEnvelope:
    """Return the latest target-portfolio signal for one account."""
    latest = research_service.create_signal(account_id=account_id)
    return ApiEnvelope(success=True, data=latest, meta={"source": "local"})


@app.get("/explain", response_model=ApiEnvelope)
def explain(signal_id: str = Query(min_length=8, max_length=128)) -> ApiEnvelope:
    """Return a lightweight explanation for a signal id."""
    details = signal_service.explain(signal_id=signal_id)
    return ApiEnvelope(success=True, data=details)


@app.get("/api/v1/overview", response_model=ApiEnvelope)
def api_overview() -> ApiEnvelope:
    """Return system overview data for the Web dashboard."""
    return ApiEnvelope(success=True, data=research_service.overview())


@app.get("/api/v1/instruments", response_model=ApiEnvelope)
def api_instruments() -> ApiEnvelope:
    """Return available tradable instruments."""
    data = [item.model_dump(mode="json") for item in research_service.market_data.list_instruments()]
    return ApiEnvelope(success=True, data=data)


@app.get("/api/v1/market-bars", response_model=ApiEnvelope)
def api_market_bars(
    symbol: str | None = Query(default=None, pattern=r"^[0-9]{6}\.(SH|SZ)$"),
    start: date | None = None,
    end: date | None = None,
) -> ApiEnvelope:
    """Return daily market bars for charts and tables."""
    bars = research_service.market_data.get_bars(symbol=symbol, start=start, end=end)
    data = [bar.model_dump(mode="json") for bar in bars]
    return ApiEnvelope(success=True, data=data, meta={"total": len(data)})


@app.get("/api/v1/strategies", response_model=ApiEnvelope)
def api_strategies() -> ApiEnvelope:
    """Return available strategy definitions."""
    data: list[dict[str, Any]] = [
        {
            "strategy_id": "etf_stock_daily_v1",
            "name": "ETF + Stock Daily Allocation",
            "description": "Conservative target-weight strategy using local daily bars.",
        }
    ]
    return ApiEnvelope(success=True, data=data)


@app.post("/api/v1/strategy-runs", response_model=ApiEnvelope)
def api_create_strategy_run(request: StrategyRunRequest | None = None) -> ApiEnvelope:
    """Run the default strategy and persist its decision."""
    run = research_service.run_strategy(trading_date=None if request is None else request.trading_date)
    return ApiEnvelope(success=True, data=run.model_dump(mode="json"))


@app.get("/api/v1/strategy-runs/{run_id}", response_model=ApiEnvelope)
def api_get_strategy_run(run_id: str) -> ApiEnvelope:
    """Return one persisted strategy run."""
    run = research_service.get_strategy_run(run_id)
    if run is None:
        return ApiEnvelope(success=False, error="strategy run not found")
    return ApiEnvelope(success=True, data=run)


@app.post("/api/v1/backtest-runs", response_model=ApiEnvelope)
def api_create_backtest_run(request: BacktestRunRequest | None = None) -> ApiEnvelope:
    """Run a local backtest and persist the result."""
    if request is None or request.start_date is None or request.end_date is None:
        data = research_service.run_backtest()
    else:
        from quant_research.backtest.engine import BacktestRequest

        data = research_service.run_backtest(
            BacktestRequest(
                start_date=request.start_date,
                end_date=request.end_date,
                initial_cash=request.initial_cash,
            )
        )
    return ApiEnvelope(success=True, data=data)


@app.get("/api/v1/backtest-runs/{run_id}", response_model=ApiEnvelope)
def api_get_backtest_run(run_id: str) -> ApiEnvelope:
    """Return one persisted backtest run."""
    run = research_service.get_backtest_run(run_id)
    if run is None:
        return ApiEnvelope(success=False, error="backtest run not found")
    return ApiEnvelope(success=True, data=run)


@app.get("/api/v1/paper/accounts/{account_id}", response_model=ApiEnvelope)
def api_get_paper_account(account_id: str) -> ApiEnvelope:
    """Return a paper account snapshot."""
    account = research_service.get_paper_account(account_id)
    return ApiEnvelope(success=True, data=account.model_dump(mode="json"))


@app.post("/api/v1/paper/accounts/{account_id}/run-signal", response_model=ApiEnvelope)
def api_run_signal_for_paper_account(account_id: str) -> ApiEnvelope:
    """Generate a signal and apply it to the paper account."""
    account = research_service.run_signal_for_paper_account(account_id)
    return ApiEnvelope(success=True, data=account.model_dump(mode="json"))


@app.get("/api/v1/signals", response_model=ApiEnvelope)
def api_list_signals() -> ApiEnvelope:
    """Return recent generated signals."""
    return ApiEnvelope(success=True, data=research_service.list_signals())


@app.get("/api/v1/signals/{signal_id}", response_model=ApiEnvelope)
def api_get_signal(signal_id: str) -> ApiEnvelope:
    """Return one generated signal."""
    signal_payload = research_service.get_signal(signal_id)
    if signal_payload is None:
        return ApiEnvelope(success=False, error="signal not found")
    return ApiEnvelope(success=True, data=signal_payload)
