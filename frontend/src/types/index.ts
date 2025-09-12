export interface User {
  id: number
  email: string
  firstName: string
  lastName: string
}

export interface AuthResponse {
  accessToken: string
  tokenType: string
  expiresIn: number
  user: User
}

export interface Transaction {
  id: number
  accountId: number
  externalId: string
  date: string
  name: string
  merchantName?: string
  amountCents: number
  currency: string
  categoryTop?: string
  categorySub?: string
  isTransfer: boolean
}

export interface Account {
  id: number
  accountId: string
  mask?: string
  name: string
  institutionName?: string
  type: string
  subtype?: string
}

export interface Goal {
  id: number
  name: string
  targetCents: number
  targetDate?: string
  savedCents: number
  status: 'ACTIVE' | 'COMPLETED' | 'PAUSED' | 'CANCELLED'
}

export interface BudgetPlan {
  id: number
  period: 'WEEKLY' | 'BIWEEKLY' | 'MONTHLY'
  startDate: string
  endDate: string
  planJson: string
  status: 'ACTIVE' | 'COMPLETED' | 'CANCELLED'
}

export interface SpendingSummary {
  category: string
  totalCents: number
  transactionCount: number
  trend: 'up' | 'down' | 'stable'
}

export interface DailySpending {
  date: string
  totalCents: number
}

export interface ChatMessage {
  id: string
  role: 'user' | 'assistant'
  content: string
  timestamp: Date
}