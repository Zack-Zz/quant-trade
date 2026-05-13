# quant-research

Python service for strategy research, backtest, paper trading, and signal generation.

## What It Provides

- Local CSV market-data provider with bundled sample A-share daily bars.
- SQLite-backed research repository for strategy runs, backtests, paper accounts, and signal history.
- Daily-bar backtest engine with A-share lot-size, fee, tax, suspension, and limit-lock handling.
- Python paper-trading simulator that reuses the backtest matcher.
- FastAPI endpoints for both Java signal consumption and the local Web dashboard.
- React/Vite Web workbench in `web/`.

## Run API

```bash
python -m venv .venv
source .venv/bin/activate
pip install -e .[dev]
uvicorn quant_research.serve.main:app --reload --port 8000
```

## Run Web

```bash
cd web
pnpm install
pnpm dev
```

The Web app defaults to `http://localhost:8000` for the FastAPI base URL. Override with `VITE_API_BASE_URL` when needed.

## API Highlights

- `GET /signal?account_id=acct-main-a`
- `GET /api/v1/overview`
- `GET /api/v1/instruments`
- `GET /api/v1/market-bars?symbol=510300.SH`
- `POST /api/v1/backtest-runs`
- `GET /api/v1/paper/accounts/acct-main-a`
- `POST /api/v1/paper/accounts/acct-main-a/run-signal`
- `GET /api/v1/signals`

## Test

```bash
pytest -q
cd web && pnpm test && pnpm build
```
