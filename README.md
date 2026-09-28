# quant-trade

> **[2026-09-28] 本项目已停止维护（stop-maintenance）。**
> 研究与决策支持工作已全部迁移至 **quant**（`/Users/zhouze/Documents/git-projects/quant`），
> quant 是唯一后续维护入口。本项目不再接收任何修改。
>
> 历史成果与恢复方式：
> - 完整归档（含 Git bundle 全历史、worktree 源文件副本及 SHA-256 清单、
>   SQLite 备份、历史报告，均经 P0-01 独立核验）：
>   `/Users/zhouze/Documents/git-projects/quant/data/archives/quant-trade/`
> - 归档清单与哈希：同目录 `inventory/`；许可：`LICENSE.quant-trade`
> - 本目录按原样保留，未做任何删除或只读化处理；恢复任何内容请从归档校验后拷回。
> - 旧全市场早晚报入口（`report/`，最后输出 2026-06-06）已确认停用：
>   经 2026-09-28 所有者确认无消费者、无其他机器/人工调用、远端无其他
>   推送权限者。后续选股研究入口为 quant 的 `current-ai-tracking` 工作流
>   与本地数据浏览页面（`python -m quant.data.browser`）。
>
> 注意：本项目包含的 broker/交易执行相关代码从未、也不得接入真实券商
> 账户或自动下单；quant 项目同样仅做研究与决策支持。

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

## Development Rules

Project-specific development and AI execution rules live in [docs/standards/project_development_rules.md](docs/standards/project_development_rules.md).

## Safety Notes

- No hardcoded production credentials
- QMT adapter is intentionally scaffolded and not active by default
- Use `BROKER_MODE=paper` until live adapter passes full simulation and replay tests
