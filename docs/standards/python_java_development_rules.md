# Python And Java Development Rules

## Status

- Scope: Python and Java code in this repository
- Owner: quant-trade maintainers
- Status: active
- Version: v1
- Last Updated: 2026-05-12

## Goal

Keep Python and Java code readable, typed, testable, and safe for trading workflows.

## Mandatory Comment Rule

关键的代码行和复杂的代码逻辑必须加上注释。

Comments are required when code carries business risk, trading semantics, non-obvious transformation logic, external integration behavior, or operational safety decisions. Comments must explain intent, assumptions, constraints, or failure behavior. Do not add empty comments that only repeat the code.

Add comments for:

- Risk controls, order sizing, portfolio weight calculations, idempotency, reconciliation, and broker behavior.
- Non-obvious formulas, thresholds, rounding rules, and A-share market rules such as lot size or T+1 handling.
- Complex branching, retries, fallback paths, concurrency, transactions, cache invalidation, or state recovery.
- External API adapters, schema conversions, time-zone handling, and cross-language contract mapping.
- Any code where a future maintainer could reasonably ask “why is this done this way?”

Avoid comments for:

- Simple assignments, obvious getters, straightforward constructor wiring, or code already made clear by naming.
- Stale design explanations that belong in a design document instead of inline code.

## Python Rules

- Use type hints for public functions, service methods, adapters, and strategy interfaces.
- Prefer Pydantic models or typed dataclasses at API, contract, and adapter boundaries.
- Python modules, classes, public functions, and public methods must include docstrings.
- Private functions and methods must include docstrings or nearby comments when they contain key logic, complex branching, non-obvious transformations, external I/O, or trading-sensitive behavior.
- Method docstrings should explain responsibility, parameters when not obvious, return semantics, side effects, exceptions, and important trading assumptions.
- Keep functions small and explicit. Extract helper functions when branching or transformations become hard to scan.
- Avoid hidden global mutable state in strategy, signal generation, and market-data providers.
- Raise specific exceptions and preserve root causes with exception chaining where useful.
- Validate external inputs at FastAPI, contract, and data-provider boundaries.
- Keep response payloads aligned with the project envelope shape: `success`, `data`, `error`, and `meta`.
- Add or update tests before changing behavior. Run `pytest -q` in `quant-research`.

### Python Comment Examples

```python
def create_latest_signal(self, account_id: str) -> Signal:
    """Create the latest target-portfolio signal for one trading account.

    The generated idempotency key must stay stable for the signal timestamp so
    the Java executor can retry safely without duplicate orders.
    """
```

```python
# A-share orders must be rounded down to board lots; over-rounding can create
# accidental leverage when cash is tight.
shares = int(raw_shares // LOT_SIZE) * LOT_SIZE
```

```python
# Keep the idempotency key stable for one generated signal so Java execution can
# safely retry without placing duplicate orders.
idempotency_key = f"{strategy_id}|{as_of.isoformat()}|{account_id}"
```

## Java Rules

- Use Java 17, Maven, UTF-8, LF, 4-space indentation, no tabs, and no wildcard imports.
- Prefer immutable data carriers such as records for domain payloads and contract values.
- Keep public APIs strongly typed. Avoid `Map<String, Object>` except for explicit extension points.
- Add interfaces only for real boundaries such as broker gateways, external clients, ledger storage, pluggable strategies, or costly test substitutes.
- Public classes, interfaces, records, enums, and annotations should have type Javadoc when added or materially changed.
- Public or protected methods need Javadoc when they perform non-trivial behavior, mutate state, return nullable values, throw domain exceptions, or call external systems.
- Inline comments are required around non-obvious trading logic, transaction/idempotency boundaries, external adapter assumptions, and recovery behavior.
- Return empty collections instead of `null` where practical, and defensively copy mutable collections crossing public boundaries.
- Use actionable exception messages without leaking secrets, credentials, account-sensitive data, or raw broker payloads.
- Add or update tests before changing behavior. Run `mvn -q test` or `mvn -q verify` in `trade-executor`.

### Java Comment Examples

```java
// A-share orders must use board lots. Rounding down avoids sending an invalid
// quantity and prevents the planner from exceeding the intended allocation.
int shares = toLot(Math.abs(deltaValue) / price);
```

```java
// Mark rejected signals as processed so a circuit-breaker rejection is not
// repeatedly retried and re-logged during the same execution cycle.
ledger.markProcessed(signal.idempotencyKey());
```

## Review Checklist

Before finishing Python or Java changes:

1. Critical lines and complex logic have intent-focused comments.
2. Comments explain why or what must be preserved, not merely what the syntax does.
3. Public contract behavior is documented where callers need it.
4. Tests cover normal paths, boundary cases, and failure paths.
5. The smallest useful validation command has passed.
