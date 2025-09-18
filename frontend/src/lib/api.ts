import axios, { isAxiosError } from 'axios'
import type {
  BudgetRequest,
  BudgetPlanResponse,
  FinancialSnapshotResponse,
  GenerateBudgetRequest,
  GenerateBudgetResponse,
  AcceptBudgetRequest,
  AcceptBudgetResponse,
  PlaidStatusResponse
} from '../types'
import { authClient } from './auth'

const API_BASE_URL = import.meta.env.VITE_API_BASE || 'http://localhost:8080/api'

export const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
})

// Request interceptor to include auth headers
api.interceptors.request.use((config) => {
  const authHeaders = authClient.getAuthHeaders()
  Object.assign(config.headers, authHeaders)
  return config
})

// Response interceptor to handle auth errors
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      authClient.logout()
      window.location.href = '/login'
    }
    return Promise.reject(error)
  }
)

// Plaid Integration
export async function createPlaidLinkToken(): Promise<{ link_token: string }> {
  try {
    const response = await api.post('/plaid/link/token/create')
    return response.data
  } catch (error) {
    if (isAxiosError(error) && error.response?.data) {
      throw error.response.data
    }
    throw error
  }
}

export async function exchangePlaidPublicToken(publicToken: string): Promise<unknown> {
  try {
    const response = await api.post('/plaid/item/public_token/exchange', { publicToken })
    return response.data
  } catch (error) {
    if (isAxiosError(error) && error.response?.data) {
      throw error.response.data
    }
    throw error
  }
}

export async function triggerPlaidInitialSync(): Promise<unknown> {
  const response = await api.post('/plaid/sync/initial')
  return response.data
}

export async function fetchPlaidStatus(): Promise<PlaidStatusResponse> {
  try {
    const response = await api.get('/plaid/status')
    return response.data
  } catch (error) {
    if (isAxiosError(error)) {
      const status = error.response?.status
      if (status === 404 || status === 501) {
        return { hasItem: false }
      }
    }
    throw error
  }
}

// AI Functions
export interface ChatMessage {
  role: string
  content: string
}

export interface ChatRequest {
  messages: ChatMessage[]
  temperature?: number
}

export async function aiChat(messages: ChatMessage[], temperature?: number): Promise<string> {
  const response = await api.post('/ai/chat/answer', { messages, temperature });
  return response.data;
}

export async function aiChatStream(
  messages: ChatMessage[], 
  temperature: number = 0.7, 
  onToken: (token: string) => void,
  onReconnect?: (attempt: number) => void,
  onError?: (error: Error) => void
): Promise<void> {
  const MAX_RETRIES = 3;
  const RETRY_DELAYS = [1000, 2000, 4000]; // 1s, 2s, 4s
  
  let controller: AbortController | null = null;

  const attemptStream = async (attempt: number): Promise<void> => {
    controller = new AbortController();
    
    try {
      // Get auth headers
      const authHeaders = authClient.getAuthHeaders()
      const headers: Record<string, string> = {
        "Accept": "text/event-stream",
        ...authHeaders
      }

      const response = await fetch(`${API_BASE_URL}/ai/chat/answer`, {
        method: 'POST',
        headers,
        body: JSON.stringify({ messages, temperature }),
        signal: controller.signal
      });

      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }

      const reader = response.body?.getReader();
      const decoder = new TextDecoder();

      if (!reader) {
        throw new Error('Failed to get response reader');
      }

      try {
        while (true) {
          const { done, value } = await reader.read();
          if (done) break;

          const chunk = decoder.decode(value);
          const lines = chunk.split('\n');

          for (const line of lines) {
            if (line.startsWith('data: ')) {
              const data = line.slice(6).trim();
              if (data && data !== '[DONE]') {
                onToken(data);
              }
            }
          }
        }
      } finally {
        reader.releaseLock();
      }

    } catch (error: any) {
      if (error.name === 'AbortError') {
        return; // Request was cancelled
      }

      if (attempt < MAX_RETRIES) {
        const delay = RETRY_DELAYS[attempt - 1];
        console.warn(`SSE connection failed (attempt ${attempt}/${MAX_RETRIES}), retrying in ${delay}ms...`, error);
        
        if (onReconnect) {
          onReconnect(attempt);
        }

        await new Promise(resolve => setTimeout(resolve, delay));
        return attemptStream(attempt + 1);
      } else {
        console.error('SSE connection failed after max retries', error);
        if (onError) {
          onError(error);
        } else {
          throw error;
        }
      }
    }
  };

  return attemptStream(1);
}

// Legacy functions for backward compatibility - convert string prompt to messages format
export async function aiChatLegacy(prompt: string): Promise<string> {
  return aiChat([{ role: 'user', content: prompt }]);
}

export async function aiChatStreamLegacy(prompt: string, onToken: (token: string) => void): Promise<void> {
  return aiChatStream([{ role: 'user', content: prompt }], 0.7, onToken);
}

export async function generateBudgetJSON(req: BudgetRequest): Promise<BudgetPlanResponse> {
  const response = await api.post('/ai/budget/plan', req);
  return response.data;
}

// Budget Adjustment functions
export interface BudgetAdjustmentRequest {
  instruction: string
  scope?: {
    month?: number
    year?: number
  }
  confirm?: boolean
  sourceCategory?: string
}

export interface BudgetAdjustmentResponse {
  status: string // "success", "needs_confirmation", "error"
  proposal?: {
    diffs: BudgetDiff[]
  }
  options?: SourceCategoryOption[]
  updatedBudget?: any
}

export interface BudgetDiff {
  category: string
  currentAmount: number
  newAmount: number
  deltaAmount: number
}

export interface SourceCategoryOption {
  category: string
  currentAmount: number
  suggestedReduction: number
}

export async function adjustBudget(request: BudgetAdjustmentRequest): Promise<BudgetAdjustmentResponse> {
  const response = await api.post('/budgets/adjust', request)
  return response.data
}

export async function adjustBudgetWithAI(request: BudgetAdjustmentRequest): Promise<{message: string, adjustmentData: BudgetAdjustmentResponse}> {
  const response = await api.post('/ai/budget/adjust', request)
  return response.data
}

// Budget functions
export async function getCurrentBudget(): Promise<any> {
  const response = await api.get('/mock/budget/active')
  return response.data
}

export async function getBudgetHistory(limit: number = 6): Promise<any[]> {
  const response = await api.get(`/mock/budget/history?limit=${limit}`)
  return response.data
}

// AI Budget functions
export async function getFinancialSnapshot(): Promise<FinancialSnapshotResponse | null> {
  try {
    const response = await api.post('/ai/budget/snapshot')
    return response.data
  } catch (error) {
    if (isAxiosError(error)) {
      const status = error.response?.status
      if (status === 404) {
        return null
      }
    }
    throw error
  }
}

export async function generateAiBudget(request: GenerateBudgetRequest): Promise<GenerateBudgetResponse> {
  const response = await api.post('/ai/budget/generate', request)
  return response.data
}

export async function acceptAiBudget(request: AcceptBudgetRequest): Promise<AcceptBudgetResponse> {
  const response = await api.post('/ai/budget/accept', request)
  return response.data
}

export default api
