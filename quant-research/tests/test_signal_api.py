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

    assert overview.status_code == 200
    assert overview.json()["data"]["data_status"]["instrument_count"] == 4
    assert bars.status_code == 200
    assert len(bars.json()["data"]) == 5


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
