import { render, screen, waitFor } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import App from './App'

const overview = {
  data_status: {
    provider: 'csv',
    latest_trading_date: '2026-03-06',
    instrument_count: 4,
    bar_count: 20
  },
  latest_backtest: null,
  latest_signal: null,
  paper_account: {
    account_id: 'acct-main-a',
    cash: 1_000_000,
    equity: 1_000_000,
    positions: [],
    orders: [],
    fills: [],
    equity_curve: []
  }
}

const instruments = [
  { symbol: '510300.SH', name: '沪深300ETF', asset_type: 'ETF', exchange: 'SH', lot_size: 100 }
]

const bars = [
  {
    symbol: '510300.SH',
    trading_date: '2026-03-06',
    open: 4.08,
    high: 4.12,
    low: 4.05,
    close: 4.1,
    volume: 125000000,
    amount: 512500000,
    adj_close: 4.1,
    suspended: false,
    limit_up: false,
    limit_down: false
  }
]

describe('App', () => {
  beforeEach(() => {
    vi.stubGlobal(
      'fetch',
      vi.fn(async (url: string) => {
        const path = String(url)
        const data = path.includes('/overview')
          ? overview
          : path.includes('/instruments')
            ? instruments
            : path.includes('/market-bars')
              ? bars
              : path.includes('/paper/accounts')
                ? overview.paper_account
                : []
        return {
          ok: true,
          status: 200,
          json: async () => ({ success: true, data })
        }
      })
    )
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('renders dashboard data from Python APIs', async () => {
    render(<App />)

    await waitFor(() => expect(screen.getAllByText('2026-03-06').length).toBeGreaterThan(0))
    expect(screen.getByText('Market Data')).toBeInTheDocument()
    expect(screen.getByText(/510300\.SH/)).toBeInTheDocument()
    expect(screen.getByText('4.10')).toBeInTheDocument()
  })
})
