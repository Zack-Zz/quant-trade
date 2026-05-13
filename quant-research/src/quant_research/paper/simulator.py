"""Deterministic A-share daily-bar simulator for backtest and paper trading."""

from __future__ import annotations

from dataclasses import dataclass, field
from datetime import date
from itertools import count

from quant_research.models.market import MarketBar
from quant_research.models.trading import EquityPoint, PositionSnapshot, SimFill, SimOrder


LOT_SIZE = 100
COMMISSION_RATE = 0.0003
SELL_TAX_RATE = 0.001


@dataclass
class SimulatorState:
    """Mutable account state owned by one simulation run."""

    cash: float
    positions: dict[str, int] = field(default_factory=dict)
    bought_today: set[str] = field(default_factory=set)
    orders: list[SimOrder] = field(default_factory=list)
    fills: list[SimFill] = field(default_factory=list)
    equity_curve: list[EquityPoint] = field(default_factory=list)


class DailyBarSimulator:
    """Apply target weights to daily bars with conservative A-share constraints."""

    def __init__(self) -> None:
        """Create a simulator with deterministic order and fill identifiers."""
        self._order_ids = count(1)
        self._fill_ids = count(1)

    def apply_targets(
        self,
        state: SimulatorState,
        trading_date: date,
        bars: dict[str, MarketBar],
        targets: dict[str, float],
    ) -> None:
        """Rebalance the state toward target weights using same-day close prices."""
        state.bought_today.clear()
        equity = self.mark_to_market(state, bars)
        for symbol, target_pct in targets.items():
            bar = bars.get(symbol)
            if bar is None or bar.suspended or bar.limit_up or bar.limit_down:
                continue

            current_qty = state.positions.get(symbol, 0)
            desired_value = equity * target_pct
            delta_value = desired_value - current_qty * bar.close
            if abs(delta_value) < bar.close * LOT_SIZE:
                continue

            side = "BUY" if delta_value > 0 else "SELL"
            quantity = self._target_quantity(abs(delta_value), bar.close)
            if quantity <= 0:
                continue
            if side == "SELL":
                quantity = min(quantity, current_qty)
            if quantity <= 0:
                continue
            self._execute_order(state, trading_date, bar, side, quantity)

        self.record_equity(state, trading_date, bars)

    def mark_to_market(self, state: SimulatorState, bars: dict[str, MarketBar]) -> float:
        """Calculate current equity from cash plus close-price market value."""
        market_value = 0.0
        for symbol, quantity in state.positions.items():
            bar = bars.get(symbol)
            if bar is not None:
                market_value += quantity * bar.close
        return state.cash + market_value

    def position_snapshots(self, state: SimulatorState, bars: dict[str, MarketBar]) -> list[PositionSnapshot]:
        """Return current positions valued with the latest available bars."""
        snapshots: list[PositionSnapshot] = []
        for symbol, quantity in sorted(state.positions.items()):
            if quantity <= 0:
                continue
            bar = bars.get(symbol)
            price = bar.close if bar is not None else 0.0
            snapshots.append(
                PositionSnapshot(
                    symbol=symbol,
                    quantity=quantity,
                    market_value=round(quantity * price, 2),
                    cost_basis=round(quantity * price, 2),
                )
            )
        return snapshots

    def record_equity(self, state: SimulatorState, trading_date: date, bars: dict[str, MarketBar]) -> None:
        """Append a daily equity point after all target orders are matched."""
        equity = round(self.mark_to_market(state, bars), 2)
        peak = max((point.equity for point in state.equity_curve), default=equity)
        peak = max(peak, equity)
        drawdown = 0.0 if peak <= 0 else (peak - equity) / peak
        state.equity_curve.append(
            EquityPoint(
                trading_date=trading_date,
                equity=equity,
                cash=round(state.cash, 2),
                drawdown_pct=round(drawdown, 6),
            )
        )

    def _execute_order(
        self,
        state: SimulatorState,
        trading_date: date,
        bar: MarketBar,
        side: str,
        quantity: int,
    ) -> None:
        """Create an order and fill it immediately at the daily close price."""
        gross = quantity * bar.close
        commission = max(5.0, gross * COMMISSION_RATE)
        tax = gross * SELL_TAX_RATE if side == "SELL" else 0.0
        total_cost = gross + commission if side == "BUY" else 0.0
        if side == "BUY" and total_cost > state.cash:
            quantity = self._target_quantity(max(state.cash - 5.0, 0.0), bar.close)
            if quantity <= 0:
                return
            gross = quantity * bar.close
            commission = max(5.0, gross * COMMISSION_RATE)
            total_cost = gross + commission

        order_id = f"order-{next(self._order_ids)}"
        order = SimOrder(
            order_id=order_id,
            symbol=bar.symbol,
            side=side,
            quantity=quantity,
            price=bar.close,
            trading_date=trading_date,
            status="FILLED",
        )
        fill = SimFill(
            fill_id=f"fill-{next(self._fill_ids)}",
            order_id=order_id,
            symbol=bar.symbol,
            side=side,
            quantity=quantity,
            price=bar.close,
            trading_date=trading_date,
            commission=round(commission, 2),
            tax=round(tax, 2),
        )

        if side == "BUY":
            state.cash -= total_cost
            state.positions[bar.symbol] = state.positions.get(bar.symbol, 0) + quantity
            state.bought_today.add(bar.symbol)
        else:
            # T+1 is represented conservatively: only shares already in the
            # position before today's buys can be sold in this daily simulator.
            state.cash += gross - commission - tax
            state.positions[bar.symbol] = max(0, state.positions.get(bar.symbol, 0) - quantity)

        state.orders.append(order)
        state.fills.append(fill)

    def _target_quantity(self, value: float, price: float) -> int:
        """Round intended notional value down to an A-share board lot."""
        if price <= 0:
            return 0
        return int(value // price // LOT_SIZE * LOT_SIZE)
