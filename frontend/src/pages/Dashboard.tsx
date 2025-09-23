import { useQuery, useQueryClient } from '@tanstack/react-query'
import { useMemo, useState } from 'react'
import { api } from '@/lib/api'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { formatCurrency } from '@/lib/utils'
import { 
  DollarSign, 
  TrendingUp, 
  TrendingDown, 
  PiggyBank,
  CreditCard,
  Wallet,
  Database,
  RefreshCw
} from 'lucide-react'
import toast from 'react-hot-toast'
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, PieChart, Pie, Cell, Legend } from 'recharts'
import type {
  GoalFormData,
} from '@/types'

interface Account {
  id: number
  name: string
  type: string
  balance: number
}

interface Transaction {
  id: number
  amount: number
  description: string
  category: string
  date: string
}

interface CategoryData {
  category: string
  amount: number
  percentage: number
}

// Function to get category colors that match BudgetOverviewCard
const getCategoryColor = (categoryName: string, index: number) => {
  // These hex values correspond to Tailwind CSS color classes used in BudgetOverviewCard
  const colors = [
    '#3B82F6',  // Blue-500
    '#10B981',  // Emerald-500
    '#8B5CF6',  // Purple-500
    '#F59E0B',  // Amber-500
    '#F43F5E',  // Rose-500
    '#06B6D4',  // Cyan-500
    '#F97316',  // Orange-500
    '#6366F1',  // Indigo-500
    '#14B8A6',  // Teal-500
    '#EC4899',  // Pink-500
    '#84CC16',  // Lime-500
    '#8B5CF6',  // Violet-500
    '#EF4444',  // Red-500
    '#EAB308',  // Yellow-500
    '#22C55E',  // Green-500
    '#0EA5E9',  // Sky-500
    '#D946EF',  // Fuchsia-500
    '#64748B',  // Slate-500
    '#71717A',  // Zinc-500
    '#78716C'   // Stone-500
  ]

  // Use index primarily for distinctness, fallback to hash for consistency
  if (index < colors.length) {
    return colors[index]
  }

  // For categories beyond our color count, use hash
  let hash = 0
  for (let i = 0; i < categoryName.length; i++) {
    hash = categoryName.charCodeAt(i) + ((hash << 5) - hash)
  }
  const colorIndex = Math.abs(hash) % colors.length
  return colors[colorIndex]
}

// Custom label component for hovered slice
const renderActiveLabel = (props: any) => {
  const { cx, cy, name, value } = props
  return (
    <text
      x={cx}
      y={cy}
      dy={8}
      textAnchor="middle"
      fill="white"
      fontSize={14}
      fontWeight="bold"
    >
      <tspan x={cx} dy="-0.5em">{name}</tspan>
      <tspan x={cx} dy="1.2em">{formatCurrency(value * 100)}</tspan>
    </text>
  )
}

export default function Dashboard() {
  const queryClient = useQueryClient()
  const isMockMode = import.meta.env.VITE_DATA_MODE === 'mock'
  const [activeIndex, setActiveIndex] = useState<number | null>(null)
  
  const { data: accounts } = useQuery<Account[]>({
    queryKey: ['accounts'],
    queryFn: async () => {
      const response = await api.get('/accounts')
      return response.data
    }
  })

  const seedMockData = async () => {
    try {
      toast.loading('Seeding mock data...', { id: 'seed' })
      const response = await api.post('/dev/mock/seed')
      toast.success(`Mock data seeded: ${response.data.accounts} accounts, ${response.data.transactions} transactions`, { id: 'seed' })
      // Refetch all queries to update the UI
      queryClient.invalidateQueries()
    } catch (error: any) {
      const message = error.response?.data?.error || 'Failed to seed mock data'
      toast.error(message, { id: 'seed' })
    }
  }

  const resetMockData = async () => {
    try {
      toast.loading('Resetting mock data...', { id: 'reset' })
      const response = await api.post('/dev/mock/reset')
      toast.success(`Mock data reset: ${response.data.accounts} accounts, ${response.data.transactions} transactions`, { id: 'reset' })
      // Refetch all queries to update the UI
      queryClient.invalidateQueries()
    } catch (error: any) {
      const message = error.response?.data?.error || 'Failed to reset mock data'
      toast.error(message, { id: 'reset' })
    }
  }

  const { data: recentTransactions } = useQuery<Transaction[]>({
    queryKey: ['transactions', 'recent'],
    queryFn: async () => {
      const response = await api.get('/transactions?limit=5')
      return response.data
    }
  })

  const { data: budgetOverview } = useQuery({
    queryKey: ['budget', 'overview'],
    queryFn: async () => {
      const response = await api.get('/budget/overview')
      return response.data
    }
  })

  const { data: balanceTrend } = useQuery({
    queryKey: ['balances', 'trend'],
    queryFn: async () => {
      const response = await api.get('/balances/trend?days=30')
      return response.data
    }
  })

  const { data: totalBalanceData } = useQuery({
    queryKey: ['balances', 'total'],
    queryFn: async () => {
      const response = await api.get('/balances/total')
      return response.data
    }
  })

  // Calculate cash balance from checking/savings accounts only
  const cashBalance = useMemo(() => {
    if (!accounts) return 0
    return accounts
      .filter(account => account.type === 'depository')
      .reduce((sum, account) => {
        // Backend sends balance in cents, formatCurrency will handle conversion
        return sum + account.balance
      }, 0)
  }, [accounts])

  return (
    <div className="space-y-6">
      {isMockMode && (
        <div className="rounded-[1rem] border border-[var(--color-warning)]/60 bg-[var(--color-warning)]/20 p-4">
          <div className="flex items-center justify-between">
            <div className="flex items-center space-x-2">
              <Database className="h-5 w-5 text-[var(--color-warning)]" />
              <div>
                <p className="text-sm font-medium text-[var(--color-text-primary)]">Mock Data Mode</p>
                <p className="text-xs text-[var(--color-warning)]">Using demo data for development</p>
              </div>
            </div>
            <div className="flex space-x-2">
              <Button 
                size="sm" 
                variant="outline" 
                onClick={seedMockData}
                className="border-[var(--color-warning)] text-[var(--color-warning)] hover:border-[var(--color-warning)] hover:bg-[var(--color-warning)]/30 hover:text-[var(--color-bg-dark)]"
              >
                <Database className="h-3 w-3 mr-1" />
                Seed Data
              </Button>
              <Button 
                size="sm" 
                variant="outline" 
                onClick={resetMockData}
                className="border-[var(--color-warning)] text-[var(--color-warning)] hover:border-[var(--color-warning)] hover:bg-[var(--color-warning)]/30 hover:text-[var(--color-bg-dark)]"
              >
                <RefreshCw className="h-3 w-3 mr-1" />
                Reset Data
              </Button>
            </div>
          </div>
        </div>
      )}
      
      <div className="flex items-center justify-between">
        <h1 className="text-3xl font-bold text-[var(--color-text-primary)]">Dashboard</h1>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium text-[var(--color-text-secondary)]">Current Cash Balance</CardTitle>
            <DollarSign className="h-4 w-4 text-[var(--color-accent-teal)]" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold text-[var(--color-text-primary)]">{formatCurrency(cashBalance)}</div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium text-[var(--color-text-secondary)]">MTD Income</CardTitle>
            <TrendingUp className="h-4 w-4 text-[var(--color-success)]" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold text-[var(--color-success)]">
              {formatCurrency((budgetOverview?.incomeMTD || 0) * 100)}
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium text-[var(--color-text-secondary)]">MTD Expenses</CardTitle>
            <TrendingDown className="h-4 w-4 text-[var(--color-error)]" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold text-[var(--color-error)]">
              {formatCurrency((budgetOverview?.expensesMTD || 0) * 100)}
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium text-[var(--color-text-secondary)]">Net Cash Flow</CardTitle>
            <PiggyBank className="h-4 w-4 text-[var(--color-accent-blue)]" />
          </CardHeader>
          <CardContent>
            <div className={`text-2xl font-bold ${(budgetOverview?.netMTD || 0) >= 0 ? 'text-[var(--color-success)]' : 'text-[var(--color-error)]'}`}>
              {formatCurrency((budgetOverview?.netMTD || 0) * 100)}
            </div>
          </CardContent>
        </Card>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <Card>
          <CardHeader>
            <CardTitle>Balance Trend</CardTitle>
            <CardDescription>Your account balance over time</CardDescription>
          </CardHeader>
          <CardContent>
            {balanceTrend && balanceTrend.length > 0 ? (
              <ResponsiveContainer width="100%" height={300}>
                <LineChart data={balanceTrend}>
                  <CartesianGrid stroke="rgba(255,255,255,0.08)" strokeDasharray="3 3" />
                  <XAxis 
                    dataKey="date" 
                    tickFormatter={(date) => new Date(date).toLocaleDateString()}
                    tick={{ fill: 'var(--color-text-secondary)', fontSize: 12 }}
                    axisLine={{ stroke: 'rgba(255,255,255,0.1)' }}
                    tickLine={{ stroke: 'rgba(255,255,255,0.1)' }}
                  />
                  <YAxis 
                    tick={{ fill: 'var(--color-text-secondary)', fontSize: 12 }}
                    axisLine={{ stroke: 'rgba(255,255,255,0.1)' }}
                    tickLine={{ stroke: 'rgba(255,255,255,0.1)' }}
                  />
                  <Tooltip
                    formatter={(value) => [formatCurrency(Number(value)), 'Balance']}
                    labelFormatter={(date) => new Date(date).toLocaleDateString()}
                    contentStyle={{
                      backgroundColor: 'var(--color-panel-dark)',
                      borderRadius: '1rem',
                      border: '1px solid rgba(255,255,255,0.08)',
                      color: 'var(--color-text-primary)',
                    }}
                  />
                  <Line 
                    type="monotone" 
                    dataKey="balance" 
                    stroke="var(--color-accent-blue)" 
                    strokeWidth={2.5}
                    dot={{ fill: 'var(--color-accent-teal)', strokeWidth: 0 }}
                    activeDot={{ r: 6, fill: 'var(--color-accent-teal)' }}
                  />
                </LineChart>
              </ResponsiveContainer>
            ) : (
              <div className="flex h-[300px] items-center justify-center text-[var(--color-text-secondary)]">
                No balance trend data available
              </div>
            )}
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Spending by Category</CardTitle>
            <CardDescription>Month-to-date breakdown</CardDescription>
          </CardHeader>
          <CardContent>
            {budgetOverview?.categoriesMTD && budgetOverview.categoriesMTD.length > 0 ? (() => {
              // Filter out categories with zero MTD spending
              const nonZeroCategories = budgetOverview.categoriesMTD.filter(cat => cat.amountMTD > 0);

              return nonZeroCategories.length > 0 ? (
                <ResponsiveContainer width="100%" height={300}>
                  <PieChart>
                    <Pie
                      data={nonZeroCategories.map(cat => ({
                        name: cat.key,
                        value: cat.amountMTD,
                        percentage: Math.round((cat.amountMTD / budgetOverview.expensesMTD) * 100)
                      }))}
                      cx="50%"
                      cy="40%"
                      outerRadius={100}
                      fill="var(--color-accent-blue)"
                      dataKey="value"
                      onMouseEnter={(_, index) => setActiveIndex(index)}
                      onMouseLeave={() => setActiveIndex(null)}
                    >
                      {nonZeroCategories.map((category, index) => (
                        <Cell
                          key={`cell-${index}`}
                          fill={getCategoryColor(category.key, index)}
                          stroke={activeIndex === index ? '#ffffff' : 'none'}
                          strokeWidth={activeIndex === index ? 3 : 0}
                          style={{
                            filter: activeIndex === index ? 'brightness(1.2)' : 'none',
                            cursor: 'pointer'
                          }}
                        />
                      ))}
                    </Pie>
                    {activeIndex !== null && nonZeroCategories[activeIndex] && (
                      <text
                        x="50%"
                        y="40%"
                        textAnchor="middle"
                        dominantBaseline="middle"
                        fill="#374151"
                        fontSize={14}
                        fontWeight="bold"
                      >
                        <tspan x="50%" dy="-0.5em">{nonZeroCategories[activeIndex].key}</tspan>
                        <tspan x="50%" dy="1.2em">{formatCurrency((nonZeroCategories[activeIndex].amountMTD || 0) * 100)}</tspan>
                      </text>
                    )}
                    <Legend
                      verticalAlign="bottom"
                      height={36}
                      wrapperStyle={{
                        paddingTop: '10px',
                        fontSize: '12px',
                        color: 'var(--color-text-secondary)'
                      }}
                    />
                  </PieChart>
                </ResponsiveContainer>
              ) : (
                <div className="flex h-[300px] items-center justify-center text-[var(--color-text-secondary)]">
                  No spending data available
                </div>
              );
            })() : (
              <div className="flex h-[300px] items-center justify-center text-[var(--color-text-secondary)]">
                No spending data available
              </div>
            )}
          </CardContent>
        </Card>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <Card>
          <CardHeader>
            <CardTitle>Accounts</CardTitle>
            <CardDescription>Your connected accounts</CardDescription>
          </CardHeader>
          <CardContent>
            <div className="space-y-4">
              {accounts?.map((account) => (
                <div key={account.id} className="flex items-center justify-between rounded-lg border border-[rgba(255,255,255,0.08)] bg-[rgba(255,255,255,0.02)] p-3">
                  <div className="flex items-center space-x-3">
                    {account.type === 'depository' ? (
                      <Wallet className="h-5 w-5 text-[var(--color-accent-teal)]" />
                    ) : (
                      <CreditCard className="h-5 w-5 text-[var(--color-accent-blue)]" />
                    )}
                    <div>
                      <p className="font-medium text-[var(--color-text-primary)]">{account.name}</p>
                      <p className="text-sm capitalize text-[var(--color-text-secondary)]">{account.type}</p>
                    </div>
                  </div>
                  <div className="text-right">
                    <p className="font-medium text-[var(--color-text-primary)]">{formatCurrency(account.balance)}</p>
                  </div>
                </div>
              ))}
              {!accounts?.length && (
                <p className="py-4 text-center text-[var(--color-text-secondary)]">No accounts connected</p>
              )}
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Recent Transactions</CardTitle>
            <CardDescription>Your latest spending activity</CardDescription>
          </CardHeader>
          <CardContent>
            <div className="space-y-4">
              {recentTransactions?.map((transaction) => (
                <div key={transaction.id} className="flex items-center justify-between rounded-lg border border-[rgba(255,255,255,0.08)] bg-[rgba(255,255,255,0.02)] p-3">
                  <div className="flex-1">
                    <p className="font-medium text-[var(--color-text-primary)] truncate">{transaction.description}</p>
                    <p className="text-sm text-[var(--color-text-secondary)]">{transaction.category}</p>
                  </div>
                  <div className="text-right ml-4">
                    <p className={`font-medium ${transaction.amount < 0 ? 'text-[var(--color-error)]' : 'text-[var(--color-success)]'}`}>
                      {formatCurrency(Math.abs(transaction.amount))}
                    </p>
                    <p className="text-xs text-[var(--color-text-secondary)]">{new Date(transaction.date).toLocaleDateString()}</p>
                  </div>
                </div>
              ))}
              {!recentTransactions?.length && (
                <p className="py-4 text-center text-[var(--color-text-secondary)]">No recent transactions</p>
              )}
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  )
}
