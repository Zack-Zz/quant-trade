# trade-executor

Java execution core for quant-trade.

## Modules

- `client`: Signal pull client (HTTP stub)
- `risk`: Risk policy evaluation
- `planner`: Target weight to lot-size orders
- `broker`: Paper broker + QMT adapter scaffold
- `ledger`: Idempotency and execution records
- `reconcile`: End-of-day reconciliation shell
- `core`: `ExecutionOrchestrator.runOnce(accountId)` main workflow

## Verification

```bash
mvn -q test
mvn -q verify
```
