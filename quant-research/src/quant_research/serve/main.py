from __future__ import annotations

from fastapi import FastAPI, Query

from quant_research.models.signal import ApiEnvelope
from quant_research.services.signal_service import SignalService

app = FastAPI(title="quant-research-signal-service", version="0.1.0")
signal_service = SignalService()


@app.get("/health", response_model=ApiEnvelope)
def health() -> ApiEnvelope:
    return ApiEnvelope(success=True, data={"status": "ok"})


@app.get("/version", response_model=ApiEnvelope)
def version() -> ApiEnvelope:
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
    latest = signal_service.create_latest_signal(account_id=account_id)
    return ApiEnvelope(success=True, data=latest.model_dump(mode="json"), meta={"source": "local"})


@app.get("/explain", response_model=ApiEnvelope)
def explain(signal_id: str = Query(min_length=8, max_length=128)) -> ApiEnvelope:
    details = signal_service.explain(signal_id=signal_id)
    return ApiEnvelope(success=True, data=details)
