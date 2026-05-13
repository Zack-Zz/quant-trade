from __future__ import annotations

from datetime import date, datetime
from typing import Any

from pydantic import BaseModel, ConfigDict, Field, field_validator, model_validator


class TargetPosition(BaseModel):
    """Target weight for one tradable instrument inside a signal portfolio."""

    model_config = ConfigDict(extra="forbid")

    symbol: str = Field(pattern=r"^[0-9]{6}\.(SH|SZ)$")
    target_pct: float = Field(ge=0.0, le=1.0)
    confidence: float | None = Field(default=None, ge=0.0, le=1.0)
    reason: str | None = Field(default=None, max_length=256)


class Portfolio(BaseModel):
    """Target portfolio allocation carried by a signal."""

    model_config = ConfigDict(extra="forbid")

    cash_target_pct: float = Field(ge=0.0, le=1.0)
    targets: list[TargetPosition]


class Constraints(BaseModel):
    """Execution constraints that downstream risk and planning must enforce."""

    model_config = ConfigDict(extra="forbid")

    max_turnover_pct: float = Field(ge=0.0, le=1.0)
    max_single_position_pct: float = Field(gt=0.0, le=1.0)
    rebalance: str = Field(pattern=r"^(DAILY_CLOSE|WEEKLY_CLOSE|MONTHLY_CLOSE)$")
    max_daily_loss_pct: float = Field(default=0.01, ge=0.0, le=1.0)
    max_drawdown_pct: float = Field(default=0.08, ge=0.0, le=1.0)
    min_avg_daily_amount: float | None = Field(default=None, ge=0.0)
    exclude_tags: list[str] = Field(default_factory=list)


class Signal(BaseModel):
    """Versioned target-portfolio signal shared by Python and Java."""

    model_config = ConfigDict(extra="forbid")

    schema_version: str = "1.0.0"
    signal_id: str = Field(min_length=8, max_length=128, pattern=r"^[a-zA-Z0-9._:-]+$")
    account_id: str = Field(min_length=1, max_length=64, pattern=r"^[a-zA-Z0-9._:-]+$")
    trading_date: date | None = None
    as_of: datetime
    strategy_id: str = Field(min_length=1, max_length=128)
    strategy_version: str | None = Field(default=None, min_length=1, max_length=64)
    rebalance_cycle: str | None = Field(default=None, pattern=r"^(DAILY_CLOSE|WEEKLY_CLOSE|MONTHLY_CLOSE)$")
    data_version: str | None = Field(default=None, min_length=1, max_length=128)
    status: str | None = Field(
        default=None,
        pattern=r"^(GENERATED|VALIDATED|APPROVED|PUBLISHED|CONSUMED|EXECUTED|EXPIRED|CANCELED|REJECTED)$",
    )
    universe: list[str] = Field(default_factory=list)
    portfolio: Portfolio
    constraints: Constraints
    checksum: str | None = Field(default=None, pattern=r"^sha256:[a-fA-F0-9]{64}$")
    idempotency_key: str
    metadata: dict[str, Any] = Field(default_factory=dict)

    @field_validator("universe")
    @classmethod
    def validate_universe_symbols(cls, value: list[str]) -> list[str]:
        """Validate universe symbols before Java execution consumes them."""
        for symbol in value:
            if len(symbol) != 9 or symbol[6] != ".":
                raise ValueError("symbol format must be XXXXXX.SH/SZ")
        return value

    @model_validator(mode="after")
    def validate_weights(self) -> "Signal":
        """Ensure target weights fit the non-cash allocation and v2 key contract."""
        target_sum = sum(target.target_pct for target in self.portfolio.targets)
        max_targets = 1.0 - self.portfolio.cash_target_pct
        if target_sum - max_targets > 1e-8:
            raise ValueError("target weights exceed available non-cash allocation")
        if self.schema_version == "2.0.0":
            missing = [
                name
                for name in ("trading_date", "strategy_version", "rebalance_cycle", "data_version", "status")
                if getattr(self, name) is None
            ]
            if missing:
                raise ValueError(f"signal v2 missing required fields: {', '.join(missing)}")
            # The execution-side idempotency key must be stable for a strategy
            # cycle. Wall-clock timestamps are intentionally excluded to avoid
            # duplicate live orders when a signal is fetched repeatedly.
            expected = (
                f"{self.strategy_id}|{self.strategy_version}|{self.account_id}|"
                f"{self.trading_date.isoformat()}|{self.rebalance_cycle}"
            )
            if self.idempotency_key != expected:
                raise ValueError("signal v2 idempotency_key must use the stable trading-cycle format")
        return self


class ApiEnvelope(BaseModel):
    """Project-wide API envelope for FastAPI responses."""

    success: bool
    data: dict[str, Any] | list[Any] | None = None
    error: str | None = None
    meta: dict[str, Any] = Field(default_factory=dict)
