from quant_research.data.market_provider import CsvMarketDataProvider
from quant_research.services.research_service import ResearchService
from quant_research.storage.sqlite_repository import ResearchRepository


def test_research_service_runs_strategy_backtest_and_paper(tmp_path) -> None:
    """Research service should coordinate the Python business workflow."""
    service = ResearchService(
        market_data=CsvMarketDataProvider(),
        repository=ResearchRepository(tmp_path / "research.sqlite3"),
    )

    strategy_run = service.run_strategy()
    backtest = service.run_backtest()
    account = service.run_signal_for_paper_account("acct-main-a")
    overview = service.overview()

    assert strategy_run.targets
    assert backtest["metrics"]["trade_count"] > 0
    assert account.equity > 0
    assert overview["data_status"]["provider"] == "csv"
