# Signal Contract v1

`signal.schema.json` is the single source of truth between `quant-research` and `trade-executor`.

## Contract Rules

- Required: `as_of`, `strategy_id`, `portfolio`, `constraints`, `account_id`, `signal_id`, `schema_version`
- Symbol format: `XXXXXX.SH` or `XXXXXX.SZ`
- `target_pct` range: `[0,1]`
- Idempotency key rule: `strategy_id + as_of + account_id`
- Envelope consumers should verify `checksum` when provided

## Validation

Python side validates with `jsonschema` and Pydantic models.
Java side validates through DTO constraints and integration contract tests.

## Versioning

- Backward-compatible changes increase minor version
- Breaking changes require new schema major and explicit migration path
