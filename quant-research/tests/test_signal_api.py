from fastapi.testclient import TestClient

from quant_research.serve.main import app


client = TestClient(app)


def test_health() -> None:
    response = client.get("/health")
    assert response.status_code == 200
    assert response.json()["success"] is True


def test_get_signal() -> None:
    response = client.get("/signal", params={"account_id": "acct-main-a"})
    assert response.status_code == 200

    body = response.json()
    assert body["success"] is True
    assert body["data"]["account_id"] == "acct-main-a"
    assert body["data"]["schema_version"] == "1.0.0"


def test_web_api_overview_and_market_bars() -> None:
    """Web endpoints should expose overview and market data."""
    overview = client.get("/api/v1/overview")
    bars = client.get("/api/v1/market-bars", params={"symbol": "510300.SH"})
    market_analysis = client.get("/api/v1/analysis/market")
    stock_analysis = client.get("/api/v1/analysis/stocks")
    data_quality = client.get("/api/v1/data-quality")

    assert overview.status_code == 200
    assert overview.json()["data"]["data_status"]["instrument_count"] == 4
    assert bars.status_code == 200
    assert len(bars.json()["data"]) == 5
    assert market_analysis.json()["data"]["regime"] in {"risk_on", "neutral", "risk_off"}
    assert stock_analysis.json()["data"]["ranked"]
    assert data_quality.json()["data"]["bar_count"] == 20


def test_web_api_runs_backtest_and_paper_signal() -> None:
    """Web endpoints should run backtest and paper signal workflows."""
    backtest = client.post("/api/v1/backtest-runs", json={})
    paper = client.post("/api/v1/paper/accounts/acct-main-a/run-signal")
    signals = client.get("/api/v1/signals")

    assert backtest.status_code == 200
    assert backtest.json()["data"]["metrics"]["trade_count"] > 0
    assert paper.status_code == 200
    assert paper.json()["data"]["equity"] > 0
    assert signals.json()["data"]


def test_signal_v2_generate_and_latest_endpoints() -> None:
    """Signal v2 endpoints should expose stable published signals."""
    generated = client.post("/api/v1/signals/generate", json={"account_id": "acct-v2"})
    latest = client.get("/api/v1/signals/latest", params={"account_id": "acct-v2"})

    assert generated.status_code == 200
    assert generated.json()["data"]["schema_version"] == "2.0.0"
    assert generated.json()["data"]["strategy_version"] == "v2"
    assert "|acct-v2|" in generated.json()["data"]["idempotency_key"]
    assert latest.status_code == 200
    assert latest.json()["data"]["idempotency_key"] == generated.json()["data"]["idempotency_key"]
