# Operations Runbook (dev/paper/live-sim/live)

## Environment Layers

- `dev`: local module verification only
- `backtest`: offline strategy validation in `quant-research`
- `paper`: Java + PaperBroker + PostgreSQL
- `live-sim`: Java + QMT adapter in simulation mode
- `live`: Java + QMT adapter with strict kill switches

## Daily Process

1. Generate signal after market close
2. Run `ExecutionOrchestrator.runOnce(accountId)` before next session
3. Verify risk decisions and planned orders
4. Execute and reconcile at end of day
5. Archive signal, risk, order, fill, and pnl records

## Alerts

- Signal endpoint unavailable for 3 consecutive pulls
- Order failure ratio > 20% in one execution batch
- Circuit breaker triggered (daily loss or max drawdown)
- Reconcile mismatch exists after retry

## Rollback

1. Disable live mode (`BROKER_MODE=paper`)
2. Revert config version for risk parameters
3. Pin previous strategy version on signal service
4. Re-run dry reconciliation
5. Resume with manual approval gate

## Incident Checklist

- Capture trace_id and signal_id
- Freeze new opening orders
- Keep close/reduce operations enabled only
- Persist broker snapshots for replay
- Create postmortem and add regression tests
