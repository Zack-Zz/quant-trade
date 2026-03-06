from __future__ import annotations

import hashlib
from datetime import datetime
from zoneinfo import ZoneInfo

from quant_research.data.market_provider import BrokerMarketDataProvider
from quant_research.models.signal import Constraints, Portfolio, Signal, TargetPosition
from quant_research.strategy.daily_alloc import DailyEtfStockStrategy, StrategyInput


class SignalService:
    def __init__(self) -> None:
        self._market_data = BrokerMarketDataProvider()
        self._strategy = DailyEtfStockStrategy()

    def create_latest_signal(self, account_id: str) -> Signal:
        etf_symbols = ["510300.SH", "159915.SZ"]
        stock_symbols = ["600519.SH", "000333.SZ"]
        context = StrategyInput(etf_symbols=etf_symbols, stock_symbols=stock_symbols)
        target_weights = self._strategy.generate_target_weights(context)

        symbols = [*etf_symbols, *stock_symbols]
        quotes = self._market_data.latest_quotes(symbols)

        targets: list[TargetPosition] = []
        for symbol, target_pct in target_weights.items():
            targets.append(
                TargetPosition(
                    symbol=symbol,
                    target_pct=target_pct,
                    confidence=0.6,
                    reason="model_allocation" if symbol in quotes else "fallback",
                )
            )

        as_of = datetime.now(ZoneInfo("Asia/Shanghai")).replace(microsecond=0)
        strategy_id = self._strategy.strategy_id
        signal_id = f"sig-{as_of.strftime('%Y%m%d%H%M%S')}-{strategy_id}"
        idempotency_key = f"{strategy_id}|{as_of.isoformat()}|{account_id}"

        signal = Signal(
            schema_version="1.0.0",
            signal_id=signal_id,
            account_id=account_id,
            as_of=as_of,
            strategy_id=strategy_id,
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
        )

        digest = hashlib.sha256(signal.model_dump_json().encode("utf-8")).hexdigest()
        signal.checksum = f"sha256:{digest}"
        return signal

    def explain(self, signal_id: str) -> dict[str, str | float]:
        return {
            "signal_id": signal_id,
            "strategy_id": self._strategy.strategy_id,
            "style": "daily-balance-etf-stock",
            "confidence_floor": 0.55,
        }
