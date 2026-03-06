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
