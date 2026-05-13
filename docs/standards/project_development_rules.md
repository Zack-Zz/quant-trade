# Project Development Rules

## Status

- Scope: AI coding execution, Java/Python style, database changes, and documentation in this repository
- Owner: quant-trade maintainers
- Status: active
- Version: v1
- Last Updated: 2026-05-12

## Goal

Keep `quant-trade` changes production-oriented, testable, and easy to operate while respecting the current monorepo shape:

- `quant-research`: Python research, backtest, and signal API.
- `trade-executor`: Java execution, risk, planning, broker adapters, and ledger.
- `contracts/signal`: versioned cross-language signal contract.
- `infra`: local deployment artifacts.
- `docs`: project documentation and standards.

## AI Execution Governance

- Treat this repository as an early-stage trading system moving toward production safety.
- Prefer direct target-state implementation over compatibility wrappers, deprecated aliases, or dual paths unless the user explicitly asks for phased migration or backward compatibility.
- Before coding, identify the target module, package ownership, public API shape, validation command, and rollback risk.
- Keep reusable code free of product-specific UI wording and local workflow assumptions.
- Public contracts should be strongly typed and small. Use untyped maps only for explicit extension points such as `metadata` or `attributes`.
- In trading-sensitive paths, prefer explicit data flow, immutable-style transformations where practical, and fail-fast validation at boundaries.

## Module Boundaries

- `contracts/signal` is the canonical schema boundary between Python and Java. Cross-language changes must update examples and tests together.
- `quant-research` owns strategy, backtest, market-data adaptation for signal generation, and FastAPI response envelopes.
- `trade-executor` owns account snapshot consumption, risk checks, order planning, broker adapters, execution ledger, and reconciliation.
- `infra` should stay deployment-focused and avoid business logic.
- Shared concepts crossing modules must be documented in contracts or references, not duplicated in ad hoc comments.

## Java Rules

- Follow the dedicated [Python And Java Development Rules](python_java_development_rules.md) for detailed Java and Python coding requirements.
- Baseline: Java 17, Maven, UTF-8, LF, 4-space indentation, no tabs, no wildcard imports.
- Use one top-level type per Java file.
- Technical identifiers use English. Chinese comments or Javadocs are acceptable when explaining business responsibility, trading semantics, or non-obvious behavior.
- Key code lines and complex logic must include intent-focused comments, especially in trading, risk, idempotency, reconciliation, and broker integration paths.
- Public classes, interfaces, records, enums, and annotations should have type Javadoc when added or materially changed.
- Document nullable returns, mutation, validation exceptions, and external side effects in Javadocs or concise comments.
- Prefer strong typed request, response, option, and result objects over `Map<String, Object>`.
- Add interfaces only for real boundaries: broker gateways, storage abstractions, pluggable strategies, external clients, or costly test substitutes.
- Do not create `XxxService` / `XxxServiceImpl` pairs without a clear runtime variation or replacement reason.
- Use implementation names that reveal the runtime difference, such as `InMemoryExecutionLedger`, `JdbcExecutionLedger`, `PaperBroker`, or `QmtBroker`.
- Return empty collections instead of `null` where practical, and defensively copy mutable collections crossing public boundaries.
- Exception messages should be actionable and must not leak secrets, account credentials, or sensitive broker payloads.
- Run `mvn -q test` or `mvn -q verify` in `trade-executor` after Java changes.

## Python Rules

- Follow the dedicated [Python And Java Development Rules](python_java_development_rules.md) for detailed Java and Python coding requirements.
- Keep Python models and API boundaries schema-driven with Pydantic or typed dataclasses.
- Use explicit type hints for new public functions and service methods.
- Python modules, classes, public functions, and public methods must include docstrings; private helpers with key or complex logic need docstrings or nearby comments.
- Key code lines and complex logic must include intent-focused comments, especially around strategy calculations, signal idempotency, schema conversion, market-data adapters, and fallback behavior.
- Keep FastAPI responses in the project envelope shape: `success`, `data`, `error`, and `meta` where applicable.
- Validate external inputs at API and adapter boundaries.
- Avoid hidden global state in strategy, data-provider, and signal-generation code.
- Run `pytest -q` in `quant-research` after Python changes. Preserve the 80% coverage gate.

## Database And SQL Rules

- New table, column, and index names use English `lower_snake_case`.
- Avoid pinyin identifiers.
- Use SQL keywords in uppercase.
- Explicitly list columns in `SELECT` and `INSERT` statements.
- Do not use unrestricted `SELECT *` in production code.
- Prefer typed migrations under `trade-executor/src/main/resources/db/migration`.
- New PostgreSQL DDL should include useful `COMMENT ON TABLE` and `COMMENT ON COLUMN` statements when adding business tables or non-obvious fields.
- Use index names in the form `pk_<table>`, `uk_<table>_<columns>`, and `idx_<table>_<columns>` where practical.
- Do not add database foreign key constraints for trading business tables unless explicitly approved; document the reason when one is needed.
- Production data changes require review, backup thinking, and a rollback path.
- Validate important read/write SQL with `EXPLAIN` before production use.

## Documentation Rules

- Prefer lowercase `snake_case.md` for new English documentation file names.
- Use relative Markdown links, not machine-local absolute paths.
- New documentation should be reachable from `docs/README.md` or an area index.
- Architecture or module design docs should include scope, owner, status, and last-updated date.
- For non-trivial mechanisms, add nearby Mermaid diagrams rather than long prose-only explanations.
- Use this recommended order for design docs:
  1. Goal
  2. Scope and non-goals
  3. Current state
  4. Target design
  5. Key design points with diagrams
  6. Interfaces and contracts
  7. Data and state model
  8. Failure handling and observability
  9. Validation plan
  10. Open decisions
- Explain important English identifiers in Chinese on first appearance when writing Chinese docs, for example `ExecutionLedger`（执行流水账本）.

## Change Checklist

Before finishing a non-trivial change:

1. Confirm module ownership and public contract impact.
2. Update or add tests first when behavior changes.
3. Update docs or contract examples when caller-facing behavior changes.
4. Run the smallest useful validation command.
5. Review `git diff` for secrets, unrelated churn, and accidental generated files.
