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
