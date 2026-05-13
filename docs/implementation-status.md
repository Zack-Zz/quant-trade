# Implementation Status

## Status

- Scope: current implementation state for the `quant-trade` monorepo
- Owner: quant-trade maintainers
- Status: active
- Last Updated: 2026-05-13

## Completed

- Monorepo structure for `contracts`, `quant-research`, `trade-executor`, `infra`, `scripts`, and `docs`
- Signal contract v1 schema + example payload
- Python FastAPI signal service: `/health`, `/version`, `/signal`, `/explain`, and `/api/v1/*` research endpoints
- Python CSV market-data provider, SQLite research repository, strategy run, daily-bar backtest, paper simulator, and signal history
- React/Vite Web MVP under `quant-research/web` for overview, market data, strategy/backtest, paper trading, and signals
- Java interfaces and default implementations for risk/planner/orchestrator/reconcile
- Paper broker and QMT adapter scaffold
- Flyway V1 migration for ledger tables and indexes
- CI workflow for Python and Java
- Dev scripts for local compose and smoke checks
- Architecture documentation under `docs/architecture`
- Signal v2 contract, Python/Java model support, stable trading-cycle idempotency, and v2 generate/latest APIs
- Java `HttpSignalClient`, expanded execution report/ledger lifecycle, Flyway V2 execution-run migration, and PostgreSQL `JdbcExecutionLedger` baseline
- Java execution ordering now persists strong-typed order plans before broker placement and broker order state after broker response
- Java Signal v2 checksum validation uses canonical JSON without `checksum`, recursive stable key sorting, whole-number float normalization, and sha256 before risk/planning/broker
- Java Signal v2 consumer rejects unknown top-level and nested fields outside extensible `metadata`
- Java execution ledger supports atomic idempotency claim for in-memory and JDBC implementations
- Java risk rejection marks execution runs as terminal `RISK_REJECTED` before marking the signal processed
- Java PaperBroker cash/position state, order lifecycle states, partial/reject/cancel/unknown handling, cash-protected A-share order planning, and risk-reject no-broker test
- Python Signal models reject unknown top-level and nested contract fields while allowing extensible `metadata`
- Python data quality report, market analyzer, stock analyzer, analysis APIs, and versioned `DailyBarSmokeEngine` backtest output
- Minimal Python `broker-gateway` FastAPI skeleton with paper/readonly guardrails, kill switch, envelope business errors, and paper order cancel

## Next (Implementation Pending)

- `/api/v1/signals/{signal_id}/explain` alias for v2 signal explain
- Complete Java ledger persistence for fills, snapshots, reconcile details, and restart recovery
- Kill switch inside Java `ExecutionOrchestrator`
- Execution Web view for signal, risk, order plan, orders, fills, reconcile, and kill switch state
- Trading calendar, provider-level `data_version`, `DecisionResult`, and strategy-run versioning
- Metrics export, alert routing, live-sim gating, Web kill-switch display, and restart replay tests
