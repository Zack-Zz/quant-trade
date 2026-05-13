# Architecture Roadmap

## Status

- Scope: staged delivery plan from current MVP to target architecture
- Owner: quant-trade maintainers
- Status: active
- Last Updated: 2026-05-13

## Phase 0: MVP Boundary Correction

Goal: run a safe local paper execution loop.

```text
Signal -> Risk -> OrderPlan -> PaperBroker -> Ledger -> Reconcile -> Web
```

Key work:

- Add Signal v2 fields: `trading_date`, `strategy_version`, `rebalance_cycle`, `data_version`.
- Change signal idempotency to a stable trading-cycle key.
- Implement execution-side `HttpSignalClient`.
- Implement `JdbcExecutionLedger` backed by PostgreSQL.
- Add execution run, risk decision, order plan, order, fill, and reconcile persistence.
- Strengthen PaperBroker with order state, cash, partial fill, rejection, and unknown status.
- Show execution chain in Web.
- Add duplicate execution and restart recovery tests.

Acceptance:

- Docker Compose can run the full paper chain.
- Re-running the same signal does not submit duplicate orders.
- Execution records survive process restart.

## Phase 1: Data Platform And Analysis

Goal: make research and execution depend on traceable, quality-checked data.

Key work:

- Add `quant-data` as the owner of instrument metadata, calendars, normalized bars, factors, and data quality reports.
- Add trading calendar, symbol lifecycle, ST, suspension, limit-up/down, and delist tags.
- Add market analyzer and stock analyzer APIs.
- Record `data_version` on analysis, signal, and backtest output.

Acceptance:

- Web can show data quality, market state, and stock scores.
- Strategies can reject bad data explicitly.
- Backtest and signal results can be traced to a data version.

## Phase 2: Backtest Engine Plugins

Goal: keep smoke testing local while adding stronger research engines through adapters.

Key work:

- Rename the current engine conceptually to `DailyBarSmokeEngine`.
- Define `BacktestEngineAdapter`.
- Add one priority open-source adapter first; keep others as later extensions.
- Standardize `BacktestRequest`, `BacktestResult`, metrics, and reports.
- Add parameter search and walk-forward workflow.

Acceptance:

- The same strategy can run through smoke and adapter engines.
- Reports are comparable across engines.
- Strategy version and data version are stored in each result.

## Phase 3: Paper Trading Hardening

Goal: make paper trading a realistic live rehearsal.

Key work:

- Implement full paper order state machine.
- Support partial fill, reject, cancel, delayed fill, and unknown status.
- Add paper reconciliation and daily report.
- Add fault injection and replay tests.

Acceptance:

- Multi-day paper runs are stable.
- Fault injection does not create duplicate orders.
- Reconciliation mismatch is visible in Web and alerting.

## Phase 4: Broker Gateway Readonly

Goal: isolate broker SDKs and prove account reading before any live placement.

Key work:

- Add root-level `broker-gateway` service when this phase starts.
- Implement QMT readonly account, positions, orders, fills, and heartbeat.
- Add raw broker event audit.
- Enforce readonly mode so placement and cancel endpoints are blocked.

Acceptance:

- Readonly mode runs for multiple trading days.
- Reconciliation with real account snapshots is stable.
- Readonly mode cannot place or cancel orders.

## Phase 5: Live-Sim And Small Live

Goal: rehearse live execution with real snapshots, then allow limited approved orders.

Key work:

- Add live-sim mode using real account snapshots and simulated order placement.
- Add manual approval and audit log.
- Add whitelist, single-order limit, daily limit, and forced reconciliation.
- Run kill switch drills.

Acceptance:

- Live-small runs with no major reconciliation or broker-state issues.
- All orders are traceable and approved.
- Kill switch blocks new opening orders immediately.

## Phase 6: Controlled Automation

Goal: scheduled live execution with full guardrails.

Key work:

- Add scheduler-driven execution.
- Add automatic reports, degradation, alerting, and incident replay.
- Keep manual override, kill switch, and mode downgrade available.

Acceptance:

- Every execution can be replayed by `trace_id`.
- Risk, audit, alerting, and rollback procedures are complete.
- The platform can downgrade from live to safer modes on failure.

## Priority Table

| Priority | Work | Reason |
|---:|---|---|
| P0 | Stable signal idempotency | Prevent duplicate orders |
| P0 | PostgreSQL execution ledger | Required for recovery and audit |
| P0 | HTTP signal consumption | Connect Python and Java loop |
| P0 | PaperBroker state machine | Required before live rehearsal |
| P0 | Kill switch | Live safety baseline |
| P1 | Data quality | Bad data creates bad trades |
| P1 | Market and stock analyzers | Required for useful decisions |
| P1 | Backtest adapter | Improve research confidence |
| P1 | Web execution view | Operators need visibility |
| P2 | Broker readonly | First safe broker integration |
| P2 | Live-sim | Last pre-live validation |
| P3 | Live-small | Limited production proof |
| P4 | Full live | Final automation stage |
