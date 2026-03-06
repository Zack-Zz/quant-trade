from datetime import datetime

import pytest

from quant_research.models.signal import Constraints, Portfolio, Signal, TargetPosition


def test_signal_weights_must_not_exceed_non_cash() -> None:
    with pytest.raises(ValueError):
        Signal(
            schema_version="1.0.0",
            signal_id="sig-20260306101010-v1",
            account_id="acct-1",
            as_of=datetime.fromisoformat("2026-03-06T15:00:00+08:00"),
            strategy_id="s1",
            universe=["510300.SH", "159915.SZ"],
            portfolio=Portfolio(
                cash_target_pct=0.2,
                targets=[
                    TargetPosition(symbol="510300.SH", target_pct=0.7),
                    TargetPosition(symbol="159915.SZ", target_pct=0.2),
                ],
            ),
            constraints=Constraints(
                max_turnover_pct=0.18,
                max_single_position_pct=0.4,
                rebalance="DAILY_CLOSE",
            ),
            idempotency_key="s1|2026-03-06T15:00:00+08:00|acct-1",
        )


def test_valid_signal_model() -> None:
    signal = Signal(
        schema_version="1.0.0",
        signal_id="sig-20260306101010-v1",
        account_id="acct-1",
        as_of=datetime.fromisoformat("2026-03-06T15:00:00+08:00"),
        strategy_id="s1",
        universe=["510300.SH"],
        portfolio=Portfolio(
            cash_target_pct=0.2,
            targets=[TargetPosition(symbol="510300.SH", target_pct=0.8)],
        ),
        constraints=Constraints(
            max_turnover_pct=0.18,
            max_single_position_pct=0.4,
            rebalance="DAILY_CLOSE",
        ),
        idempotency_key="s1|2026-03-06T15:00:00+08:00|acct-1",
    )
    assert signal.strategy_id == "s1"
