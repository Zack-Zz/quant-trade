"""Market-data provider interfaces and local CSV implementation."""

from __future__ import annotations

import csv
from datetime import date
from pathlib import Path
from typing import Protocol

from quant_research.models.market import DataStatus, Instrument, MarketBar


class MarketDataProvider(Protocol):
    """Read-only market-data contract used by strategy, backtest, and APIs."""

    def list_instruments(self) -> list[Instrument]:
        """Return all instruments available in the local data cache."""

    def get_bars(self, symbol: str | None = None, start: date | None = None, end: date | None = None) -> list[MarketBar]:
        """Return daily bars filtered by optional symbol and date range."""

    def latest_bars(self, symbols: list[str]) -> dict[str, MarketBar]:
        """Return the latest daily bar for each requested symbol."""

    def status(self) -> DataStatus:
        """Return cache metadata for observability and Web display."""


class CsvMarketDataProvider:
    """Market-data provider backed by local CSV files.

    The provider deliberately keeps the storage format simple for v1 so that
    real exported broker or vendor data can be inspected and replaced by hand.
    """

    def __init__(self, base_dir: Path | str | None = None) -> None:
        """Create a CSV provider using the bundled sample cache by default."""
        package_root = Path(__file__).resolve().parents[3]
        self.base_dir = Path(base_dir) if base_dir is not None else package_root / "data" / "market"
        self._instruments: list[Instrument] | None = None
        self._bars: list[MarketBar] | None = None

    def list_instruments(self) -> list[Instrument]:
        """Return instruments sorted by symbol."""
        if self._instruments is None:
            path = self.base_dir / "instruments.csv"
            with path.open("r", encoding="utf-8", newline="") as handle:
                self._instruments = [
                    Instrument(
                        symbol=row["symbol"],
                        name=row["name"],
                        asset_type=row["asset_type"],
                        exchange=row["exchange"],
                        lot_size=int(row.get("lot_size") or 100),
                    )
                    for row in csv.DictReader(handle)
                ]
        return sorted(self._instruments, key=lambda item: item.symbol)

    def get_bars(self, symbol: str | None = None, start: date | None = None, end: date | None = None) -> list[MarketBar]:
        """Return daily bars filtered by symbol and inclusive date range."""
        bars = self._load_bars()
        filtered = []
        for bar in bars:
            if symbol is not None and bar.symbol != symbol:
                continue
            if start is not None and bar.trading_date < start:
                continue
            if end is not None and bar.trading_date > end:
                continue
            filtered.append(bar)
        return sorted(filtered, key=lambda item: (item.trading_date, item.symbol))

    def latest_bars(self, symbols: list[str]) -> dict[str, MarketBar]:
        """Return the most recent bar for each symbol that has local data."""
        wanted = set(symbols)
        latest: dict[str, MarketBar] = {}
        for bar in self._load_bars():
            if bar.symbol not in wanted:
                continue
            current = latest.get(bar.symbol)
            if current is None or bar.trading_date > current.trading_date:
                latest[bar.symbol] = bar
        return latest

    def status(self) -> DataStatus:
        """Return a compact summary of the local CSV data cache."""
        bars = self._load_bars()
        latest_date = max((bar.trading_date for bar in bars), default=None)
        return DataStatus(
            provider="csv",
            latest_trading_date=latest_date,
            instrument_count=len(self.list_instruments()),
            bar_count=len(bars),
        )

    def _load_bars(self) -> list[MarketBar]:
        """Load and cache all local daily bars from the CSV cache."""
        if self._bars is None:
            path = self.base_dir / "bars.csv"
            with path.open("r", encoding="utf-8", newline="") as handle:
                self._bars = [self._bar_from_row(row) for row in csv.DictReader(handle)]
        return self._bars

    def _bar_from_row(self, row: dict[str, str]) -> MarketBar:
        """Convert one CSV row into a validated market bar."""
        return MarketBar(
            symbol=row["symbol"],
            trading_date=date.fromisoformat(row["date"]),
            open=float(row["open"]),
            high=float(row["high"]),
            low=float(row["low"]),
            close=float(row["close"]),
            volume=int(row["volume"]),
            amount=float(row["amount"]),
            adj_close=float(row["adj_close"]),
            suspended=self._parse_bool(row["suspended"]),
            limit_up=self._parse_bool(row["limit_up"]),
            limit_down=self._parse_bool(row["limit_down"]),
        )

    def _parse_bool(self, value: str) -> bool:
        """Parse CSV boolean values exported by common data tools."""
        return value.strip().lower() in {"1", "true", "yes", "y"}


class BrokerMarketDataProvider(CsvMarketDataProvider):
    """Backward-compatible alias for the current signal service.

    New code should depend on ``MarketDataProvider`` or ``CsvMarketDataProvider``.
    """
