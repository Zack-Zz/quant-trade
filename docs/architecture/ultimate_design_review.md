# Ultimate Design Review

## Status

- Scope: review of `quant_trade_ultimate_design.md`
- Owner: quant-trade maintainers
- Status: active
- Last Updated: 2026-05-13

## Summary

`quant_trade_ultimate_design.md` is a sound ultimate target for this repository. Its core direction should be kept:

- The platform is self-owned, while research and backtest engines can be pluggable.
- Strategies produce target-portfolio signals, not direct broker orders.
- Execution must pass through contract validation, risk, order planning, ledger recording, broker gateway, reconciliation, and kill switch checks.
- Paper and readonly stages are mandatory before any live trading stage.

The document is intentionally ahead of the current codebase. The architecture docs in this directory preserve the target while making current gaps, phase boundaries, and contract corrections explicit.

## Reviewed Issues

| Area | Finding | Architecture Decision |
|---|---|---|
| Signal idempotency | Current signal contract says idempotency uses `strategy_id + as_of + account_id`; ultimate target requires a stable trading-cycle key. | Treat stable idempotency as the target: `strategy_id + strategy_version + account_id + trading_date + rebalance_cycle`. Add this in Signal v2 rather than silently changing v1. |
| Repository layout | Target includes root modules such as `quant-data`, `broker-gateway`, and `web-console`; current code does not. | Keep current modules in place. Add new root modules only when the relevant phase starts. Keep current Web under `quant-research/web` until the console grows beyond research workflows. |
| Storage | Python research state currently uses SQLite; Java execution has Flyway ledger draft. Ultimate target uses PostgreSQL schemas. | SQLite remains local MVP storage. Execution safety moves first to PostgreSQL through `JdbcExecutionLedger`; research data can migrate later. |
| Backtest engine scope | Ultimate target lists several adapters. Adding all at once would make the first implementation too broad. | Keep the built-in daily engine as smoke coverage. Add one prioritized open-source adapter first, then expand. |
| Implementation status | `docs/implementation-status.md` can lag behind code. | Treat code and tests as current-state truth. Use this architecture directory for target-state and migration design. |
| Live trading | Ultimate target includes full live automation. | Full live remains a late-phase goal. Readonly, live-sim, live-small, approval, reconciliation, and kill switch must be proven first. |

## Current State Map

| Target Capability | Current Location | Current Maturity |
|---|---|---|
| Signal contract | `contracts/signal` | v1 schema and examples exist; v2 fields are pending |
| Research API | `quant-research/src/quant_research/serve` | FastAPI MVP with overview, market bars, strategy, backtest, paper, and signal endpoints |
| Market data | `quant-research/data/market` and provider code | CSV cache MVP; production data platform pending |
| Backtest | `quant-research/src/quant_research/backtest` | Daily-bar smoke engine |
| Paper simulation | `quant-research/src/quant_research/paper` and Java `PaperBroker` | Basic simulator and broker scaffold; full order state machine pending |
| Execution | `trade-executor/src/main/java/.../core` | Orchestrator skeleton with tests |
| Risk and planner | `trade-executor/src/main/java/.../risk`, `planner` | Default implementations exist; rule depth needs expansion |
| Ledger | Java in-memory ledger plus Flyway V1 | PostgreSQL implementation pending |
| Web | `quant-research/web` | Research-oriented local dashboard |
| Observability | `infra` and docs | Docker compose exists; metrics and alerts pending |

## Correction Principles

- Do not edit the root ultimate design unless explicitly asked; keep it as the raw source.
- In module docs, use "current state" and "target design" to avoid confusing MVP with final architecture.
- For contract changes, define a versioned migration path rather than changing meaning in place.
- Keep live trading gated by paper, readonly, simulation, manual approval, ledger recovery, and reconciliation tests.

## Recommended Near-Term Focus

1. Signal v2 contract and stable idempotency.
2. `HttpSignalClient` and execution-side signal consumption.
3. PostgreSQL-backed execution ledger.
4. Paper broker order state machine and duplicate-execution tests.
5. Web execution view for signal, risk, plan, order, fill, and reconcile state.
