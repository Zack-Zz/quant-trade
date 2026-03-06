# quant-research

Python service for strategy research, backtest and signal generation.

## Run

```bash
python -m venv .venv
source .venv/bin/activate
pip install -e .[dev]
uvicorn quant_research.serve.main:app --reload --port 8000
```

## Test

```bash
pytest -q
```
