#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"

(
  cd "$ROOT_DIR/quant-research"
  . .venv/bin/activate
  pytest -q
)

(
  cd "$ROOT_DIR/trade-executor"
  mvn -q verify
)
