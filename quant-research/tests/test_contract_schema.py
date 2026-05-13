import json
from pathlib import Path

from jsonschema import Draft202012Validator


def test_example_signal_matches_schema() -> None:
    root = Path(__file__).resolve().parents[2]
    schema_path = root / "contracts" / "signal" / "signal.schema.json"
    example_path = root / "contracts" / "signal" / "examples" / "etf_stock_daily_v1.json"

    schema = json.loads(schema_path.read_text(encoding="utf-8"))
    example = json.loads(example_path.read_text(encoding="utf-8"))

    validator = Draft202012Validator(schema)
    errors = sorted(validator.iter_errors(example), key=lambda e: e.path)
    assert errors == []


def test_v2_example_signal_matches_schema() -> None:
    """Signal v2 example should encode the stable trading-cycle contract."""
    root = Path(__file__).resolve().parents[2]
    schema_path = root / "contracts" / "signal" / "v2" / "signal.schema.json"
    example_path = root / "contracts" / "signal" / "v2" / "examples" / "etf_stock_daily_v2.json"

    schema = json.loads(schema_path.read_text(encoding="utf-8"))
    example = json.loads(example_path.read_text(encoding="utf-8"))

    validator = Draft202012Validator(schema)
    errors = sorted(validator.iter_errors(example), key=lambda e: e.path)

    assert errors == []
    assert example["schema_version"] == "2.0.0"
    assert example["idempotency_key"] == "etf_stock_daily|v2|acct-main-a|2026-03-06|DAILY_CLOSE"
