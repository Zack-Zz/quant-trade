from __future__ import annotations

from datetime import datetime
from typing import Any

from pydantic import BaseModel, Field, field_validator, model_validator


class TargetPosition(BaseModel):
    symbol: str = Field(pattern=r"^[0-9]{6}\.(SH|SZ)$")
    target_pct: float = Field(ge=0.0, le=1.0)
    confidence: float | None = Field(default=None, ge=0.0, le=1.0)
    reason: str | None = Field(default=None, max_length=256)


class Portfolio(BaseModel):
    cash_target_pct: float = Field(ge=0.0, le=1.0)
    targets: list[TargetPosition]


class Constraints(BaseModel):
    max_turnover_pct: float = Field(ge=0.0, le=1.0)
    max_single_position_pct: float = Field(gt=0.0, le=1.0)
    rebalance: str = Field(pattern=r"^(DAILY_CLOSE|WEEKLY_CLOSE|MONTHLY_CLOSE)$")
    max_daily_loss_pct: float = Field(default=0.01, ge=0.0, le=1.0)
    max_drawdown_pct: float = Field(default=0.08, ge=0.0, le=1.0)
    min_avg_daily_amount: float | None = Field(default=None, ge=0.0)
    exclude_tags: list[str] = Field(default_factory=list)


class Signal(BaseModel):
    schema_version: str = "1.0.0"
    signal_id: str = Field(min_length=8, max_length=128, pattern=r"^[a-zA-Z0-9._:-]+$")
    account_id: str = Field(min_length=1, max_length=64, pattern=r"^[a-zA-Z0-9._:-]+$")
    as_of: datetime
    strategy_id: str = Field(min_length=1, max_length=128)
    universe: list[str] = Field(default_factory=list)
    portfolio: Portfolio
    constraints: Constraints
    checksum: str | None = Field(default=None, pattern=r"^sha256:[a-fA-F0-9]{64}$")
    idempotency_key: str

    @field_validator("universe")
    @classmethod
    def validate_universe_symbols(cls, value: list[str]) -> list[str]:
        for symbol in value:
            if len(symbol) != 9 or symbol[6] != ".":
                raise ValueError("symbol format must be XXXXXX.SH/SZ")
        return value

    @model_validator(mode="after")
    def validate_weights(self) -> "Signal":
        target_sum = sum(target.target_pct for target in self.portfolio.targets)
        max_targets = 1.0 - self.portfolio.cash_target_pct
        if target_sum - max_targets > 1e-8:
            raise ValueError("target weights exceed available non-cash allocation")
        return self


class ApiEnvelope(BaseModel):
    success: bool
    data: dict[str, Any] | list[Any] | None = None
    error: str | None = None
    meta: dict[str, Any] = Field(default_factory=dict)
