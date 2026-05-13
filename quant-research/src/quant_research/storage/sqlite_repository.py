"""SQLite repository for local research and paper-trading state."""

from __future__ import annotations

import json
import sqlite3
from datetime import UTC, datetime
from pathlib import Path
from typing import Any


class ResearchRepository:
    """Persist Python-layer runs and snapshots in a local SQLite database."""

    def __init__(self, db_path: Path | str | None = None) -> None:
        """Create a repository using a local app database by default."""
        package_root = Path(__file__).resolve().parents[3]
        self.db_path = Path(db_path) if db_path is not None else package_root / "data" / "quant_research.sqlite3"
        self.db_path.parent.mkdir(parents=True, exist_ok=True)
        self.initialize()

    def initialize(self) -> None:
        """Create tables if they are not present yet."""
        with self._connect() as conn:
            conn.executescript(
                """
                CREATE TABLE IF NOT EXISTS data_imports (
                  id INTEGER PRIMARY KEY AUTOINCREMENT,
                  provider TEXT NOT NULL,
                  latest_trading_date TEXT,
                  instrument_count INTEGER NOT NULL,
                  bar_count INTEGER NOT NULL,
                  created_at TEXT NOT NULL
                );

                CREATE TABLE IF NOT EXISTS strategy_runs (
                  run_id TEXT PRIMARY KEY,
                  payload_json TEXT NOT NULL,
                  created_at TEXT NOT NULL
                );

                CREATE TABLE IF NOT EXISTS backtest_runs (
                  run_id TEXT PRIMARY KEY,
                  payload_json TEXT NOT NULL,
                  created_at TEXT NOT NULL
                );

                CREATE TABLE IF NOT EXISTS paper_accounts (
                  account_id TEXT PRIMARY KEY,
                  payload_json TEXT NOT NULL,
                  updated_at TEXT NOT NULL
                );

                CREATE TABLE IF NOT EXISTS signal_history (
                  signal_id TEXT PRIMARY KEY,
                  payload_json TEXT NOT NULL,
                  created_at TEXT NOT NULL
                );
                """
            )

    def record_data_status(self, payload: dict[str, Any]) -> None:
        """Persist one market-data status observation for the overview page."""
        with self._connect() as conn:
            conn.execute(
                """
                INSERT INTO data_imports (
                  provider, latest_trading_date, instrument_count, bar_count, created_at
                ) VALUES (?, ?, ?, ?, ?)
                """,
                (
                    payload["provider"],
                    payload.get("latest_trading_date"),
                    payload["instrument_count"],
                    payload["bar_count"],
                    self._now(),
                ),
            )

    def latest_data_status(self) -> dict[str, Any] | None:
        """Return the latest recorded market-data status, if any."""
        with self._connect() as conn:
            row = conn.execute(
                """
                SELECT provider, latest_trading_date, instrument_count, bar_count, created_at
                FROM data_imports
                ORDER BY id DESC
                LIMIT 1
                """
            ).fetchone()
        if row is None:
            return None
        return dict(row)

    def save_strategy_run(self, run_id: str, payload: dict[str, Any]) -> None:
        """Persist a strategy run payload by run id."""
        self._upsert_json("strategy_runs", "run_id", run_id, payload, "created_at")

    def get_strategy_run(self, run_id: str) -> dict[str, Any] | None:
        """Return one strategy run payload, if present."""
        return self._get_json("strategy_runs", "run_id", run_id)

    def list_strategy_runs(self, limit: int = 20) -> list[dict[str, Any]]:
        """Return recent strategy runs newest first."""
        return self._list_json("strategy_runs", limit)

    def save_backtest_run(self, run_id: str, payload: dict[str, Any]) -> None:
        """Persist a backtest run payload by run id."""
        self._upsert_json("backtest_runs", "run_id", run_id, payload, "created_at")

    def get_backtest_run(self, run_id: str) -> dict[str, Any] | None:
        """Return one backtest run payload, if present."""
        return self._get_json("backtest_runs", "run_id", run_id)

    def list_backtest_runs(self, limit: int = 20) -> list[dict[str, Any]]:
        """Return recent backtest runs newest first."""
        return self._list_json("backtest_runs", limit)

    def save_paper_account(self, account_id: str, payload: dict[str, Any]) -> None:
        """Persist the current paper-account snapshot."""
        self._upsert_json("paper_accounts", "account_id", account_id, payload, "updated_at")

    def get_paper_account(self, account_id: str) -> dict[str, Any] | None:
        """Return one paper-account snapshot, if present."""
        return self._get_json("paper_accounts", "account_id", account_id)

    def save_signal(self, signal_id: str, payload: dict[str, Any]) -> None:
        """Persist a generated signal for Web inspection and audit."""
        self._upsert_json("signal_history", "signal_id", signal_id, payload, "created_at")

    def get_signal(self, signal_id: str) -> dict[str, Any] | None:
        """Return one signal payload, if present."""
        return self._get_json("signal_history", "signal_id", signal_id)

    def list_signals(self, limit: int = 20) -> list[dict[str, Any]]:
        """Return recent generated signals newest first."""
        return self._list_json("signal_history", limit)

    def _connect(self) -> sqlite3.Connection:
        """Open a row-factory connection to the local database."""
        conn = sqlite3.connect(self.db_path)
        conn.row_factory = sqlite3.Row
        return conn

    def _upsert_json(self, table: str, key_column: str, key: str, payload: dict[str, Any], time_column: str) -> None:
        """Store JSON payloads using SQLite upsert semantics."""
        with self._connect() as conn:
            conn.execute(
                f"""
                INSERT INTO {table} ({key_column}, payload_json, {time_column})
                VALUES (?, ?, ?)
                ON CONFLICT({key_column}) DO UPDATE SET
                  payload_json = excluded.payload_json,
                  {time_column} = excluded.{time_column}
                """,
                (key, json.dumps(payload, ensure_ascii=False), self._now()),
            )

    def _get_json(self, table: str, key_column: str, key: str) -> dict[str, Any] | None:
        """Load one JSON payload from a keyed table."""
        with self._connect() as conn:
            row = conn.execute(
                f"SELECT payload_json FROM {table} WHERE {key_column} = ?",
                (key,),
            ).fetchone()
        return None if row is None else json.loads(row["payload_json"])

    def _list_json(self, table: str, limit: int) -> list[dict[str, Any]]:
        """Load recent JSON payloads from a table with creation timestamps."""
        safe_limit = max(1, min(limit, 100))
        order_column = "updated_at" if table == "paper_accounts" else "created_at"
        with self._connect() as conn:
            rows = conn.execute(
                f"SELECT payload_json FROM {table} ORDER BY {order_column} DESC LIMIT ?",
                (safe_limit,),
            ).fetchall()
        return [json.loads(row["payload_json"]) for row in rows]

    def _now(self) -> str:
        """Return an ISO timestamp suitable for SQLite text columns."""
        return datetime.now(UTC).replace(microsecond=0).isoformat()
