"""Market data models shared by research, backtest, paper trading, and APIs."""

from __future__ import annotations

from datetime import date

from pydantic import BaseModel, Field


class Instrument(BaseModel):
    """Tradable instrument metadata used to interpret market bars and orders."""

    symbol: str = Field(pattern=r"^[0-9]{6}\.(SH|SZ)$")
    name: str
    asset_type: str
    exchange: str = Field(pattern=r"^(SH|SZ)$")
    lot_size: int = Field(default=100, gt=0)


class MarketBar(BaseModel):
    """Daily OHLCV bar with A-share tradability flags."""

    symbol: str = Field(pattern=r"^[0-9]{6}\.(SH|SZ)$")
    trading_date: date
    open: float = Field(gt=0)
    high: float = Field(gt=0)
    low: float = Field(gt=0)
    close: float = Field(gt=0)
    volume: int = Field(ge=0)
    amount: float = Field(ge=0)
    adj_close: float = Field(gt=0)
    suspended: bool = False
    limit_up: bool = False
    limit_down: bool = False


class DataStatus(BaseModel):
    """Summary of the locally available market-data cache."""

    provider: str
    latest_trading_date: date | None
    instrument_count: int
    bar_count: int
