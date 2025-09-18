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

export interface GoalFormData {
  goals: string[]
  style: 'aggressive' | 'balanced' | 'flexible'
  mustKeepCategories?: string[]
  categoryCaps?: Record<string, number>
  notes?: string
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

export interface BudgetRequest {
  monthlyIncome: number
  fixedExpenses: number
  savingsRate: number
  categories: string[]
}

export interface Allocation {
  category: string
  amount: number
  pctOfIncome: number
}

export interface BudgetPlanResponse {
  monthlyIncome: number
  fixedExpenses: number
  targetSavings: number
  variableTotal: number
  allocations: Allocation[]
  recommendations: string[]
}

// New Budget Wizard Types
export interface BudgetWizardAllocation {
  name: string
  amount: number
}

export interface BudgetWizardRequest {
  month: number
  year: number
  income: number
  fixed: BudgetWizardAllocation[]
  variable: BudgetWizardAllocation[]
}

export interface BudgetWizardResponse {
  id: string
  month: number
  year: number
  income: number
  fixed: BudgetWizardAllocation[]
  variable: BudgetWizardAllocation[]
  createdAt: string
  updatedAt: string
}

export interface BudgetPrefillResponse {
  incomeEstimate: number
  fixed: BudgetWizardAllocation[]
  variableSuggestions: BudgetWizardAllocation[]
}

// AI Budget Types
export interface CategoryActual {
  category: string
  actual: number
  target?: number
}

export interface FinancialTotals {
  expenses: number
  savings: number
  netCashFlow: number
}

export interface FinancialSnapshotResponse {
  month: string
  income: number
  actualsByCategory: CategoryActual[]
  totals: FinancialTotals
  targetsByCategory?: CategoryTarget[]
  acceptedAt?: string
}

export interface CategoryTarget {
  category: string
  target: number
  reason: string
}

export interface BudgetSummary {
  savingsRate: number
  notes: string[]
}

export interface GenerateBudgetRequest {
  month: string
  goals: string[]
  style: 'aggressive' | 'balanced' | 'flexible'
  constraints?: {
    mustKeepCategories?: string[]
    categoryCaps?: Record<string, number>
  }
  notes?: string
}

export interface GenerateBudgetResponse {
  month: string
  targetsByCategory: CategoryTarget[]
  summary: BudgetSummary
  promptTokens: number
  completionTokens: number
}

export interface AcceptBudgetRequest {
  month: string
  targetsByCategory: CategoryTarget[]
}

export interface AcceptBudgetResponse {
  status: string
}

export interface PlaidStatusResponse {
  hasItem: boolean
}
