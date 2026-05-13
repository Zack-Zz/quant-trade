import { useEffect, useMemo, useState } from 'react'

import { api } from './api'
import type { BacktestRun, Instrument, MarketBar, Overview, PaperAccount, SignalPayload } from './types'

const ACCOUNT_ID = 'acct-main-a'

type LoadState = {
  overview: Overview | null
  instruments: Instrument[]
  bars: MarketBar[]
  paper: PaperAccount | null
  signals: SignalPayload[]
  backtest: BacktestRun | null
}

const emptyState: LoadState = {
  overview: null,
  instruments: [],
  bars: [],
  paper: null,
  signals: [],
  backtest: null
}

function App() {
  const [state, setState] = useState<LoadState>(emptyState)
  const [selectedSymbol, setSelectedSymbol] = useState('510300.SH')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    void loadDashboard(selectedSymbol)
  }, [selectedSymbol])

  async function loadDashboard(symbol: string) {
    setLoading(true)
    setError(null)
    try {
      const [overview, instruments, bars, paper, signals] = await Promise.all([
        api.overview(),
        api.instruments(),
        api.marketBars(symbol),
        api.paperAccount(ACCOUNT_ID),
        api.signals()
      ])
      setState((current) => ({ ...current, overview, instruments, bars, paper, signals }))
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : '加载失败')
    } finally {
      setLoading(false)
    }
  }

  async function runBacktest() {
    setError(null)
    const backtest = await api.runBacktest()
    setState((current) => ({ ...current, backtest }))
  }

  async function runPaperSignal() {
    setError(null)
    const paper = await api.runPaperSignal(ACCOUNT_ID)
    const signals = await api.signals()
    setState((current) => ({ ...current, paper, signals }))
  }

  const equityPath = useMemo(() => buildEquityPath(state.backtest?.equity_curve ?? state.paper?.equity_curve ?? []), [state])

  return (
    <main className="shell">
      <header className="topbar">
        <div>
          <p className="eyebrow">Quant Research Workbench</p>
          <h1>Python 研究层控制台</h1>
        </div>
        <div className="actions">
          <button onClick={runBacktest}>运行回测</button>
          <button onClick={runPaperSignal}>信号驱动纸交易</button>
        </div>
      </header>

      {loading && <p className="notice">正在加载 Python 业务数据...</p>}
      {error && <p className="error">{error}</p>}

      <section className="metrics" aria-label="Overview">
        <Metric label="最新交易日" value={state.overview?.data_status.latest_trading_date ?? '-'} />
        <Metric label="标的数量" value={state.overview?.data_status.instrument_count ?? 0} />
        <Metric label="行情 Bar" value={state.overview?.data_status.bar_count ?? 0} />
        <Metric label="纸账户权益" value={formatMoney(state.paper?.equity ?? state.overview?.paper_account.equity ?? 0)} />
      </section>

      <section className="grid">
        <div className="panel">
          <div className="panelHeader">
            <h2>Market Data</h2>
            <select value={selectedSymbol} onChange={(event) => setSelectedSymbol(event.target.value)}>
              {state.instruments.map((item) => (
                <option key={item.symbol} value={item.symbol}>
                  {item.symbol} {item.name}
                </option>
              ))}
            </select>
          </div>
          <table>
            <thead>
              <tr>
                <th>日期</th>
                <th>收盘</th>
                <th>成交额</th>
              </tr>
            </thead>
            <tbody>
              {state.bars.map((bar) => (
                <tr key={`${bar.symbol}-${bar.trading_date}`}>
                  <td>{bar.trading_date}</td>
                  <td>{bar.close.toFixed(2)}</td>
                  <td>{formatMoney(bar.amount)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        <div className="panel">
          <div className="panelHeader">
            <h2>Strategy & Backtest</h2>
            <span>{state.backtest?.run_id ?? '尚未运行'}</span>
          </div>
          <div className="chart" role="img" aria-label="Equity curve">
            <svg viewBox="0 0 320 120" preserveAspectRatio="none">
              <path d={equityPath} />
            </svg>
          </div>
          <div className="miniGrid">
            <Metric label="收益率" value={formatPct(state.backtest?.metrics.total_return_pct)} />
            <Metric label="最大回撤" value={formatPct(state.backtest?.metrics.max_drawdown_pct)} />
            <Metric label="成交数" value={state.backtest?.metrics.trade_count ?? 0} />
          </div>
        </div>

        <div className="panel">
          <div className="panelHeader">
            <h2>Paper Trading</h2>
            <span>{ACCOUNT_ID}</span>
          </div>
          <table>
            <thead>
              <tr>
                <th>标的</th>
                <th>数量</th>
                <th>市值</th>
              </tr>
            </thead>
            <tbody>
              {(state.paper?.positions ?? []).map((position) => (
                <tr key={position.symbol}>
                  <td>{position.symbol}</td>
                  <td>{position.quantity}</td>
                  <td>{formatMoney(position.market_value)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        <div className="panel">
          <div className="panelHeader">
            <h2>Signals</h2>
            <span>{state.signals.length} 条</span>
          </div>
          <div className="signalList">
            {state.signals.slice(0, 5).map((signal) => (
              <article key={signal.signal_id}>
                <strong>{signal.signal_id}</strong>
                <p>{signal.strategy_id} · {signal.as_of}</p>
                <div className="targets">
                  {signal.portfolio.targets.map((target) => (
                    <span key={target.symbol}>
                      {target.symbol} {(target.target_pct * 100).toFixed(1)}%
                    </span>
                  ))}
                </div>
              </article>
            ))}
          </div>
        </div>
      </section>
    </main>
  )
}

function Metric({ label, value }: { label: string; value: string | number }) {
  return (
    <div className="metric">
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  )
}

function buildEquityPath(points: Array<{ equity: number }>) {
  if (points.length === 0) {
    return 'M 0 100 L 320 100'
  }
  const values = points.map((point) => point.equity)
  const min = Math.min(...values)
  const max = Math.max(...values)
  const spread = Math.max(max - min, 1)
  return points
    .map((point, index) => {
      const x = points.length === 1 ? 0 : (index / (points.length - 1)) * 320
      const y = 110 - ((point.equity - min) / spread) * 100
      return `${index === 0 ? 'M' : 'L'} ${x.toFixed(2)} ${y.toFixed(2)}`
    })
    .join(' ')
}

function formatMoney(value: number) {
  return new Intl.NumberFormat('zh-CN', { maximumFractionDigits: 0 }).format(value)
}

function formatPct(value?: number) {
  return value === undefined ? '-' : `${(value * 100).toFixed(2)}%`
}

export default App
