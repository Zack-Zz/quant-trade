from datetime import date

from quant_research.data.market_provider import CsvMarketDataProvider


def test_csv_market_provider_reads_instruments_and_bars() -> None:
    """CSV provider should expose bundled local market data."""
    provider = CsvMarketDataProvider()

    instruments = provider.list_instruments()
    bars = provider.get_bars(symbol="510300.SH", start=date(2026, 3, 2), end=date(2026, 3, 6))
    status = provider.status()

    assert len(instruments) == 4
    assert len(bars) == 5
    assert status.latest_trading_date == date(2026, 3, 6)


def test_latest_bars_returns_one_bar_per_symbol() -> None:
    """Latest bars should select the newest date for each requested symbol."""
    provider = CsvMarketDataProvider()

    latest = provider.latest_bars(["510300.SH", "000333.SZ"])

    assert latest["510300.SH"].close == 4.10
    assert latest["000333.SZ"].trading_date == date(2026, 3, 6)
