# Architecture Documentation

## Status

- Scope: target architecture and module designs for `quant-trade`
- Owner: quant-trade maintainers
- Status: active
- Last Updated: 2026-05-13

## Purpose

This directory turns the root-level `quant_trade_ultimate_design.md` into maintainable architecture documents.

The root document remains the original ultimate-goal source. Documents here record the reviewed target state, known gaps, module-level designs, and staged implementation path.

## Reading Order

1. [Ultimate Design Review](ultimate_design_review.md)
2. [Target Architecture](target_architecture.md)
3. [Roadmap](roadmap.md)
4. Module designs in [modules](modules/)

## Module Designs

- [Contracts](modules/contracts.md)
- [Quant Data](modules/quant_data.md)
- [Quant Research](modules/quant_research.md)
- [Market Analyzer](modules/market_analyzer.md)
- [Stock Analyzer](modules/stock_analyzer.md)
- [Decision Engine](modules/decision_engine.md)
- [Backtest Engine](modules/backtest_engine.md)
- [Paper Trading](modules/paper_trading.md)
- [Signal Service](modules/signal_service.md)
- [Trade Executor](modules/trade_executor.md)
- [Risk Engine](modules/risk_engine.md)
- [Order Planner](modules/order_planner.md)
- [Execution Ledger](modules/execution_ledger.md)
- [Broker Gateway](modules/broker_gateway.md)
- [Web Console](modules/web_console.md)
- [Observability](modules/observability.md)

## Maintenance Rules

- Keep module documents aligned with code ownership in `contracts`, `quant-research`, `trade-executor`, `infra`, and future modules.
- Record target-state ambition separately from current-state facts.
- When code changes public contracts or module boundaries, update the relevant module design and the roadmap.
- Prefer small architecture documents over a single oversized design file.
