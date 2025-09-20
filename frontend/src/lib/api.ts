import axios from 'axios'

const API_BASE_URL = import.meta.env.VITE_API_BASE || '/api'

export const api = axios.create({
  baseURL: API_BASE_URL,
  withCredentials: true,
})

export async function safeFetch(path: string, init?: RequestInit) {
  const res = await fetch(API_BASE_URL + path, { credentials: 'include', ...init })
  const ct = res.headers.get('content-type') || ''
  const body = ct.includes('application/json') ? await res.json() : await res.text()
  if (!res.ok) throw new Error(typeof body === 'string' ? body : JSON.stringify(body))
  return body
}

/** ---- Endpoints your UI expects ---- **/

export async function adjustBudget(request: any): Promise<any> {
  const { data } = await api.post('/budgets/adjust', request)
  return data
}

export async function acceptAiBudget(request: any): Promise<any> {
  const { data } = await api.post('/ai/budget/accept', request)
  return data
}

export async function generateAiBudget(request: any): Promise<any> {
  const { data } = await api.post('/ai/budget/generate', request)
  return data
}

export async function getFinancialSnapshot(): Promise<any | null> {
  try {
    const { data } = await api.post('/ai/budget/snapshot')
    return data
  } catch (e: any) {
    if (e?.response?.status === 404) return null
    throw e
  }
}

export async function createPlaidLinkToken(): Promise<{ link_token: string }> {
  const { data } = await api.post('/plaid/link/token/create')
  return data
}

export async function exchangePlaidPublicToken(publicToken: string): Promise<any> {
  const { data } = await api.post('/plaid/item/public_token/exchange', { publicToken })
  return data
}

export async function fetchPlaidStatus(): Promise<{ hasItem: boolean }> {
  try {
    const { data } = await api.get('/plaid/status')
    return data
  } catch (e: any) {
    const status = e?.response?.status
    if (status === 404 || status === 501) return { hasItem: false }
    throw e
  }
}

export async function triggerPlaidInitialSync(): Promise<any> {
  const { data } = await api.post('/plaid/sync')
  return data
}

export async function aiChat(message: string): Promise<any> {
  const { data } = await api.post('/ai/chat', { message })
  return data
}

export async function aiChatStream(message: string): Promise<any> {
  const { data } = await api.post('/ai/chat/stream', { message })
  return data
}

// Type definitions
export interface ChatMessage {
  id: string
  content: string
  role: 'user' | 'assistant'
  timestamp: string
}

export interface BudgetAdjustmentRequest {
  adjustments: Array<{
    category: string
    amount: number
    reason: string
  }>
}

export interface BudgetAdjustmentResponse {
  success: boolean
  adjustments: Array<{
    category: string
    oldAmount: number
    newAmount: number
    difference: number
  }>
}

export interface SourceCategoryOption {
  category: string
  amount: number
  available: number
}
