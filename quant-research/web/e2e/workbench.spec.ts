import { expect, test } from '@playwright/test'

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
  { symbol: '510300.SH', name: '沪深300ETF', asset_type: 'ETF', exchange: 'SH', lot_size: 100 },
  { symbol: '159915.SZ', name: '创业板ETF', asset_type: 'ETF', exchange: 'SZ', lot_size: 100 }
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

const backtest = {
  run_id: 'bt-e2e',
  strategy_id: 'etf_stock_daily_v1',
  metrics: {
    total_return_pct: 0.0277,
    max_drawdown_pct: 0.0053,
    annual_turnover_pct: 0.75,
    trade_count: 8
  },
  equity_curve: [
    { trading_date: '2026-03-02', equity: 999776, cash: 254304, drawdown_pct: 0 },
    { trading_date: '2026-03-06', equity: 1027741, cash: 260346, drawdown_pct: 0 }
  ],
  fills: [{ fill_id: 'fill-1', symbol: '510300.SH', side: 'BUY', quantity: 77500, price: 4 }]
}

const paper = {
  account_id: 'acct-main-a',
  cash: 249990,
  equity: 999775,
  positions: [{ symbol: '510300.SH', quantity: 75600, market_value: 309960 }],
  orders: [{ order_id: 'order-1', symbol: '510300.SH', side: 'BUY', quantity: 75600, status: 'FILLED' }],
  fills: [{ fill_id: 'fill-1', symbol: '510300.SH', side: 'BUY', quantity: 75600, price: 4.1 }],
  equity_curve: [{ trading_date: '2026-03-06', equity: 999775, cash: 249990, drawdown_pct: 0 }]
}

const signal = {
  signal_id: 'sig-e2e',
  account_id: 'acct-main-a',
  as_of: '2026-05-12T22:00:00+08:00',
  strategy_id: 'etf_stock_daily_v1',
  universe: ['510300.SH'],
  portfolio: {
    cash_target_pct: 0.12,
    targets: [{ symbol: '510300.SH', target_pct: 0.31, confidence: 0.6, reason: 'model_allocation' }]
  },
  constraints: {},
  checksum: 'sha256:abc'
}

test('shows Python research data and runs core actions', async ({ page }) => {
  await page.route('**/api/v1/**', async (route) => {
    const url = route.request().url()
    const data = url.includes('/overview')
      ? overview
      : url.includes('/instruments')
        ? instruments
        : url.includes('/market-bars')
          ? bars
          : url.includes('/backtest-runs')
            ? backtest
            : url.includes('/paper/accounts') && route.request().method() === 'POST'
              ? paper
              : url.includes('/paper/accounts')
                ? overview.paper_account
                : url.includes('/signals')
                  ? [signal]
                  : {}
    await route.fulfill({ json: { success: true, data } })
  })

  await page.goto('/')
  await expect(page.getByRole('heading', { name: 'Python 研究层控制台' })).toBeVisible()
  await expect(page.getByText('2026-03-06').first()).toBeVisible()
  await expect(page.getByText('4.10')).toBeVisible()

  await page.getByRole('button', { name: '运行回测' }).click()
  await expect(page.getByText('bt-e2e')).toBeVisible()
  await expect(page.getByText('2.77%')).toBeVisible()

  await page.getByRole('button', { name: '信号驱动纸交易' }).click()
  await expect(page.getByText('75600')).toBeVisible()
  await expect(page.getByText('sig-e2e')).toBeVisible()
})
