export type ApiEnvelope<T> = {
  success: boolean
  data: T
  error?: string | null
  meta?: Record<string, unknown>
}

export type DataStatus = {
  provider: string
  latest_trading_date: string | null
  instrument_count: number
  bar_count: number
}

export type Overview = {
  data_status: DataStatus
  latest_backtest: BacktestRun | null
  latest_signal: SignalPayload | null
  paper_account: PaperAccount
}

export type Instrument = {
  symbol: string
  name: string
  asset_type: string
  exchange: string
  lot_size: number
}

export type MarketBar = {
  symbol: string
  trading_date: string
  open: number
  high: number
  low: number
  close: number
  volume: number
  amount: number
  adj_close: number
  suspended: boolean
  limit_up: boolean
  limit_down: boolean
}

export type EquityPoint = {
  trading_date: string
  equity: number
  cash: number
  drawdown_pct: number
}

export type BacktestRun = {
  run_id: string
  strategy_id: string
  metrics: {
    total_return_pct: number
    max_drawdown_pct: number
    annual_turnover_pct: number
    trade_count: number
  }
  equity_curve: EquityPoint[]
  fills: Array<{ fill_id: string; symbol: string; side: string; quantity: number; price: number }>
}

export type PaperAccount = {
  account_id: string
  cash: number
  equity: number
  positions: Array<{ symbol: string; quantity: number; market_value: number }>
  orders: Array<{ order_id: string; symbol: string; side: string; quantity: number; status: string }>
  fills: Array<{ fill_id: string; symbol: string; side: string; quantity: number; price: number }>
  equity_curve: EquityPoint[]
}

export type SignalPayload = {
  signal_id: string
  account_id: string
  as_of: string
  strategy_id: string
  universe: string[]
  portfolio: {
    cash_target_pct: number
    targets: Array<{ symbol: string; target_pct: number; confidence?: number; reason?: string }>
  }
  constraints: Record<string, unknown>
  checksum?: string | null
}
