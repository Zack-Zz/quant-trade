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


def test_signal_v2_stable_idempotency_fields() -> None:
    """Signal v2 should carry stable cycle fields used by Java execution."""
    signal = Signal(
        schema_version="2.0.0",
        signal_id="sig-20260306-etf-stock-daily-v2-acct-1",
        account_id="acct-1",
        trading_date="2026-03-06",
        as_of=datetime.fromisoformat("2026-03-06T15:00:00+08:00"),
        strategy_id="etf_stock_daily",
        strategy_version="v2",
        rebalance_cycle="DAILY_CLOSE",
        data_version="csv-20260306-v1",
        status="PUBLISHED",
        universe=["510300.SH"],
        portfolio=Portfolio(
            cash_target_pct=0.2,
            targets=[TargetPosition(symbol="510300.SH", target_pct=0.8)],
        ),
        constraints=Constraints(
            max_turnover_pct=0.18,
            max_single_position_pct=0.9,
            rebalance="DAILY_CLOSE",
        ),
        idempotency_key="etf_stock_daily|v2|acct-1|2026-03-06|DAILY_CLOSE",
        metadata={"source": "test"},
    )

    assert signal.strategy_version == "v2"
    assert signal.idempotency_key == "etf_stock_daily|v2|acct-1|2026-03-06|DAILY_CLOSE"


def test_signal_v2_rejects_unstable_idempotency_key() -> None:
    """Signal v2 must reject time-based idempotency keys."""
    with pytest.raises(ValueError):
        Signal(
            schema_version="2.0.0",
            signal_id="sig-20260306-etf-stock-daily-v2-acct-1",
            account_id="acct-1",
            trading_date="2026-03-06",
            as_of=datetime.fromisoformat("2026-03-06T15:00:00+08:00"),
            strategy_id="etf_stock_daily",
            strategy_version="v2",
            rebalance_cycle="DAILY_CLOSE",
            data_version="csv-20260306-v1",
            status="PUBLISHED",
            universe=["510300.SH"],
            portfolio=Portfolio(
                cash_target_pct=0.2,
                targets=[TargetPosition(symbol="510300.SH", target_pct=0.8)],
            ),
            constraints=Constraints(
                max_turnover_pct=0.18,
                max_single_position_pct=0.9,
                rebalance="DAILY_CLOSE",
            ),
            idempotency_key="etf_stock_daily|2026-03-06T15:00:00+08:00|acct-1",
        )


def test_signal_rejects_unknown_top_level_field() -> None:
    """Signal contracts should fail closed on unknown top-level fields."""
    with pytest.raises(ValueError):
        Signal(
            schema_version="1.0.0",
            signal_id="sig-20260306101010-v1",
            account_id="acct-1",
            as_of=datetime.fromisoformat("2026-03-06T15:00:00+08:00"),
            strategy_id="s1",
            universe=["510300.SH"],
            portfolio=Portfolio(cash_target_pct=0.2, targets=[TargetPosition(symbol="510300.SH", target_pct=0.8)]),
            constraints=Constraints(max_turnover_pct=0.18, max_single_position_pct=0.4, rebalance="DAILY_CLOSE"),
            idempotency_key="s1|2026-03-06T15:00:00+08:00|acct-1",
            unexpected="blocked",
        )


def test_signal_rejects_unknown_nested_field_but_allows_metadata_extensions() -> None:
    """Nested portfolio fields are strict while metadata remains extensible."""
    with pytest.raises(ValueError):
        Signal(
            schema_version="2.0.0",
            signal_id="sig-20260306-etf-stock-daily-v2-acct-1",
            account_id="acct-1",
            trading_date="2026-03-06",
            as_of=datetime.fromisoformat("2026-03-06T15:00:00+08:00"),
            strategy_id="etf_stock_daily",
            strategy_version="v2",
            rebalance_cycle="DAILY_CLOSE",
            data_version="csv-20260306-v1",
            status="PUBLISHED",
            universe=["510300.SH"],
            portfolio=Portfolio(
                cash_target_pct=0.2,
                targets=[TargetPosition(symbol="510300.SH", target_pct=0.8, unknown_nested="blocked")],
            ),
            constraints=Constraints(max_turnover_pct=0.18, max_single_position_pct=0.9, rebalance="DAILY_CLOSE"),
            idempotency_key="etf_stock_daily|v2|acct-1|2026-03-06|DAILY_CLOSE",
            metadata={"custom_research_note": {"nested": True}},
        )
