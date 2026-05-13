from __future__ import annotations

import hashlib
import json
from datetime import date, datetime
from zoneinfo import ZoneInfo

from quant_research.data.market_provider import CsvMarketDataProvider, MarketDataProvider
from quant_research.models.signal import Constraints, Portfolio, Signal, TargetPosition
from quant_research.strategy.daily_alloc import DailyEtfStockStrategy, StrategyInput


def canonical_signal_payload(signal: Signal) -> str:
    """Return the cross-language canonical JSON payload used for Signal checksums."""
    payload = _normalize_json_numbers(signal.model_dump(mode="json", exclude={"checksum"}))
    return json.dumps(payload, ensure_ascii=False, separators=(",", ":"), sort_keys=True)


def signal_checksum(signal: Signal) -> str:
    """Return the sha256 checksum for a signal using the canonical payload contract."""
    digest = hashlib.sha256(canonical_signal_payload(signal).encode("utf-8")).hexdigest()
    return f"sha256:{digest}"


def _normalize_json_numbers(value: object) -> object:
    """Normalize whole-number floats so Java and Python produce identical JSON."""
    if isinstance(value, dict):
        return {key: _normalize_json_numbers(item) for key, item in value.items()}
    if isinstance(value, list):
        return [_normalize_json_numbers(item) for item in value]
    if isinstance(value, float) and value.is_integer():
        return int(value)
    return value


class SignalService:
    """Generate target-portfolio signals from the Python strategy layer."""

    def __init__(self, market_data: MarketDataProvider | None = None) -> None:
        """Create a signal service with injectable market data."""
        self._market_data = market_data or CsvMarketDataProvider()
        self._strategy = DailyEtfStockStrategy()

    def create_latest_signal(self, account_id: str, schema_version: str = "2.0.0") -> Signal:
        """Create the latest target-portfolio signal for one account.

        ``schema_version`` keeps the legacy `/signal` endpoint compatible while
        new `/api/v1/signals/*` endpoints use Signal v2 stable idempotency.
        """
        etf_symbols = [item.symbol for item in self._market_data.list_instruments() if item.asset_type == "ETF"]
        stock_symbols = [item.symbol for item in self._market_data.list_instruments() if item.asset_type == "STOCK"]
        context = StrategyInput(etf_symbols=etf_symbols, stock_symbols=stock_symbols)
        target_weights = self._strategy.generate_target_weights(context)

        symbols = [*etf_symbols, *stock_symbols]
        latest_bars = self._market_data.latest_bars(symbols)

        targets: list[TargetPosition] = []
        for symbol, target_pct in target_weights.items():
            bar = latest_bars.get(symbol)
            targets.append(
                TargetPosition(
                    symbol=symbol,
                    target_pct=target_pct,
                    confidence=0.6,
                    reason="model_allocation" if bar is not None else "missing_market_data",
                )
            )

        as_of = datetime.now(ZoneInfo("Asia/Shanghai")).replace(microsecond=0)
        strategy_id = "etf_stock_daily" if schema_version == "2.0.0" else self._strategy.strategy_id
        strategy_version = "v2"
        rebalance_cycle = "DAILY_CLOSE"
        trading_date = self._resolve_trading_date(latest_bars)
        data_version = f"csv-{trading_date.strftime('%Y%m%d')}-v1"

        if schema_version == "2.0.0":
            signal_id = f"sig-{trading_date.isoformat().replace('-', '')}-{strategy_id}-{strategy_version}-{account_id}"
            idempotency_key = f"{strategy_id}|{strategy_version}|{account_id}|{trading_date.isoformat()}|{rebalance_cycle}"
        else:
            signal_id = f"sig-{as_of.strftime('%Y%m%d%H%M%S')}-{strategy_id}"
            idempotency_key = f"{strategy_id}|{as_of.isoformat()}|{account_id}"

        signal = Signal(
            schema_version=schema_version,
            signal_id=signal_id,
            account_id=account_id,
            trading_date=trading_date if schema_version == "2.0.0" else None,
            as_of=as_of,
            strategy_id=strategy_id,
            strategy_version=strategy_version if schema_version == "2.0.0" else None,
            rebalance_cycle=rebalance_cycle if schema_version == "2.0.0" else None,
            data_version=data_version if schema_version == "2.0.0" else None,
            status="PUBLISHED" if schema_version == "2.0.0" else None,
            universe=symbols,
            portfolio=Portfolio(cash_target_pct=0.12, targets=targets),
            constraints=Constraints(
                max_turnover_pct=0.18,
                max_single_position_pct=0.40,
                rebalance="DAILY_CLOSE",
                max_daily_loss_pct=0.01,
                max_drawdown_pct=0.08,
                min_avg_daily_amount=200_000_000,
                exclude_tags=["ST", "退市", "停牌"],
            ),
            idempotency_key=idempotency_key,
            metadata={"source": "local-csv"} if schema_version == "2.0.0" else {},
        )

        # The checksum contract is shared with Java: remove checksum, sort all
        # JSON keys, then sha256 the compact UTF-8 payload.
        signal.checksum = signal_checksum(signal)
        return signal

    def explain(self, signal_id: str) -> dict[str, str | float]:
        """Return a lightweight explanation for the latest strategy version."""
        return {
            "signal_id": signal_id,
            "strategy_id": self._strategy.strategy_id,
            "style": "daily-balance-etf-stock",
            "confidence_floor": 0.55,
        }

    def _resolve_trading_date(self, latest_bars: dict[str, object]) -> date:
        """Resolve the newest trading date used to make Signal v2 replayable."""
        dates = [bar.trading_date for bar in latest_bars.values() if hasattr(bar, "trading_date")]
        if dates:
            return max(dates)
        status = self._market_data.status()
        if status.latest_trading_date is None:
            raise ValueError("no market data is available for signal generation")
        return status.latest_trading_date
