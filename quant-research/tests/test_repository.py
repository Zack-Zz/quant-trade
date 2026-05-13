from quant_research.storage.sqlite_repository import ResearchRepository


def test_repository_persists_json_payloads(tmp_path) -> None:
    """Repository should round-trip run, signal, and account payloads."""
    repo = ResearchRepository(tmp_path / "research.sqlite3")

    repo.save_strategy_run("sr-1", {"run_id": "sr-1", "value": 1})
    repo.save_backtest_run("bt-1", {"run_id": "bt-1", "value": 2})
    repo.save_paper_account("acct", {"account_id": "acct", "cash": 100})
    repo.save_signal("sig-1", {"signal_id": "sig-1"})

    assert repo.get_strategy_run("sr-1")["value"] == 1
    assert repo.get_backtest_run("bt-1")["value"] == 2
    assert repo.get_paper_account("acct")["cash"] == 100
    assert repo.list_signals()[0]["signal_id"] == "sig-1"


def test_repository_records_data_status(tmp_path) -> None:
    """Data status snapshots should be readable by the overview service."""
    repo = ResearchRepository(tmp_path / "research.sqlite3")

    repo.record_data_status(
        {
            "provider": "csv",
            "latest_trading_date": "2026-03-06",
            "instrument_count": 4,
            "bar_count": 20,
        }
    )

    status = repo.latest_data_status()
    assert status is not None
    assert status["bar_count"] == 20
