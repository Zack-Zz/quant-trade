CREATE TABLE IF NOT EXISTS signal_log (
  id BIGSERIAL PRIMARY KEY,
  signal_id VARCHAR(128) NOT NULL,
  account_id VARCHAR(64) NOT NULL,
  strategy_id VARCHAR(128) NOT NULL,
  as_of TIMESTAMPTZ NOT NULL,
  payload_json JSONB NOT NULL,
  checksum VARCHAR(80),
  trace_id VARCHAR(64) NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT uniq_signal_per_account UNIQUE (signal_id, account_id)
);

CREATE TABLE IF NOT EXISTS risk_decision (
  id BIGSERIAL PRIMARY KEY,
  signal_id VARCHAR(128) NOT NULL,
  account_id VARCHAR(64) NOT NULL,
  approved BOOLEAN NOT NULL,
  messages JSONB NOT NULL,
  trace_id VARCHAR(64) NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS order_plan (
  id BIGSERIAL PRIMARY KEY,
  signal_id VARCHAR(128) NOT NULL,
  account_id VARCHAR(64) NOT NULL,
  symbol VARCHAR(16) NOT NULL,
  side VARCHAR(8) NOT NULL,
  quantity INT NOT NULL,
  limit_price NUMERIC(18, 4) NOT NULL,
  trace_id VARCHAR(64) NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS orders (
  id BIGSERIAL PRIMARY KEY,
  broker_order_id VARCHAR(128) NOT NULL,
  signal_id VARCHAR(128) NOT NULL,
  account_id VARCHAR(64) NOT NULL,
  symbol VARCHAR(16) NOT NULL,
  side VARCHAR(8) NOT NULL,
  status VARCHAR(32) NOT NULL,
  quantity INT NOT NULL,
  filled_quantity INT NOT NULL DEFAULT 0,
  trading_date DATE NOT NULL,
  trace_id VARCHAR(64) NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_orders_status_trading_date ON orders(status, trading_date);

CREATE TABLE IF NOT EXISTS fills (
  id BIGSERIAL PRIMARY KEY,
  broker_order_id VARCHAR(128) NOT NULL,
  account_id VARCHAR(64) NOT NULL,
  symbol VARCHAR(16) NOT NULL,
  quantity INT NOT NULL,
  price NUMERIC(18, 4) NOT NULL,
  trade_time TIMESTAMPTZ NOT NULL,
  trace_id VARCHAR(64) NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_fills_symbol_trade_time ON fills(symbol, trade_time);

CREATE TABLE IF NOT EXISTS positions_snapshot (
  id BIGSERIAL PRIMARY KEY,
  account_id VARCHAR(64) NOT NULL,
  trading_date DATE NOT NULL,
  symbol VARCHAR(16) NOT NULL,
  quantity INT NOT NULL,
  avg_cost NUMERIC(18, 4) NOT NULL,
  market_value NUMERIC(18, 4) NOT NULL,
  trace_id VARCHAR(64) NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS pnl_daily (
  id BIGSERIAL PRIMARY KEY,
  account_id VARCHAR(64) NOT NULL,
  trading_date DATE NOT NULL,
  daily_pnl_pct NUMERIC(8, 6) NOT NULL,
  drawdown_pct NUMERIC(8, 6) NOT NULL,
  trace_id VARCHAR(64) NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS reconcile_result (
  id BIGSERIAL PRIMARY KEY,
  account_id VARCHAR(64) NOT NULL,
  trading_date DATE NOT NULL,
  matched BOOLEAN NOT NULL,
  message TEXT NOT NULL,
  trace_id VARCHAR(64) NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
