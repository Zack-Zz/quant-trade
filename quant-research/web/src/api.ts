import type { ApiEnvelope, BacktestRun, Instrument, MarketBar, Overview, PaperAccount, SignalPayload } from './types'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8000'

async function request<T>(path: string, options?: RequestInit): Promise<T> {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    headers: { 'Content-Type': 'application/json', ...options?.headers },
    ...options
  })
  const envelope = (await response.json()) as ApiEnvelope<T>
  if (!response.ok || !envelope.success) {
    throw new Error(envelope.error ?? `Request failed: ${response.status}`)
  }
  return envelope.data
}

export const api = {
  overview: () => request<Overview>('/api/v1/overview'),
  instruments: () => request<Instrument[]>('/api/v1/instruments'),
  marketBars: (symbol: string) => request<MarketBar[]>(`/api/v1/market-bars?symbol=${encodeURIComponent(symbol)}`),
  runBacktest: () => request<BacktestRun>('/api/v1/backtest-runs', { method: 'POST', body: JSON.stringify({}) }),
  paperAccount: (accountId: string) => request<PaperAccount>(`/api/v1/paper/accounts/${accountId}`),
  runPaperSignal: (accountId: string) =>
    request<PaperAccount>(`/api/v1/paper/accounts/${accountId}/run-signal`, { method: 'POST' }),
  signals: () => request<SignalPayload[]>('/api/v1/signals')
}
