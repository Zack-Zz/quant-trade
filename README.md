# quant-trade

A-share quant trading monorepo with Python signal generation and Java execution.

## Workspace Layout

- `contracts/signal`: versioned signal schema and examples
- `quant-research`: strategy, backtest, and FastAPI signal service
- `trade-executor`: Java execution, risk, planning, broker adapters, and ledger
- `infra`: deployment artifacts (docker compose)
- `scripts`: local orchestration and smoke tests
- `docs`: runbook and implementation status

## MVP Delivered in This Commit

- Signal Contract v1 (`schema_version=1.0.0`) and contract tests
- Python API: `/health`, `/version`, `/signal`, `/explain`
- Java API surface:
  - `SignalClient.fetchLatest(accountId)`
  - `RiskEngine.evaluate(signal, snapshot)`
  - `OrderPlanner.plan(signal, snapshot, marketData)`
  - `Broker.placeOrders/cancel/queryOrders/queryPositions`
  - `ReconcileService.run(tradingDate)`
  - `ExecutionOrchestrator.runOnce(accountId)`
- Flyway SQL migration for PostgreSQL ledger tables and indexes
- CI pipeline for Python and Java tests

## Quick Start

```bash
./scripts/dev-up.sh
./scripts/smoke-test.sh
./scripts/dev-down.sh
```

## Safety Notes

- No hardcoded production credentials
- QMT adapter is intentionally scaffolded and not active by default
- Use `BROKER_MODE=paper` until live adapter passes full simulation and replay tests
