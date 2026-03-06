from __future__ import annotations

from dataclasses import dataclass


@dataclass(frozen=True)
class MarketQuote:
    symbol: str
    last_price: float
    avg_daily_amount_20d: float
    suspended: bool
    at_limit_up: bool
    at_limit_down: bool


class BrokerMarketDataProvider:
    """Stub provider. Replace with real broker market data SDK adapter."""

    def latest_quotes(self, symbols: list[str]) -> dict[str, MarketQuote]:
        quotes: dict[str, MarketQuote] = {}
        for index, symbol in enumerate(symbols):
            quotes[symbol] = MarketQuote(
                symbol=symbol,
                last_price=10.0 + index,
                avg_daily_amount_20d=500_000_000,
                suspended=False,
                at_limit_up=False,
                at_limit_down=False,
            )
        return quotes
