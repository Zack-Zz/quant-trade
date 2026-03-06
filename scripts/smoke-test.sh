#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"

if ! curl -fsS "http://localhost:8000/health" >/dev/null; then
  echo "quant-research health check failed"
  exit 1
fi

SIGNAL_JSON="$(curl -fsS "http://localhost:8000/signal?account_id=acct-main-a")"
if [[ "$SIGNAL_JSON" != *"signal_id"* ]]; then
  echo "signal response invalid"
  exit 1
fi

echo "smoke test passed"
