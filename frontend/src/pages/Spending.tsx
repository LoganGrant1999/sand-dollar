import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { api } from '@/lib/api'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Select } from '@/components/ui/select'
import { formatCurrency, formatDate } from '@/lib/utils'
import { 
  TrendingDown,
  Calendar,
  DollarSign
} from 'lucide-react'
import { 
  LineChart, 
  Line, 
  XAxis, 
  YAxis, 
  CartesianGrid, 
  Tooltip, 
  ResponsiveContainer,
  PieChart as RechartsPieChart,
  Pie,
  Cell,
  BarChart,
  Bar
} from 'recharts'

interface Transaction {
  id: number
  amount: number
  description: string
  category: string
  date: string
  accountName: string
}

interface SpendingAnalytics {
  totalSpent: number
  averageDaily: number
  categories: Array<{
    category: string
    amount: number
    count: number
    percentage: number
  }>
  trends: Array<{
    date: string
    amount: number
  }>
  topMerchants: Array<{
    merchant: string
    amount: number
    count: number
  }>
}

type SpendingCategory = SpendingAnalytics['categories'][number]
const RADIAN = Math.PI / 180

const renderCategoryLabel = ({
  cx,
  cy,
  midAngle,
  outerRadius,
  payload,
}: {
  cx: number
  cy: number
  midAngle: number
  outerRadius: number
  payload: SpendingCategory
}) => {
  const radius = outerRadius + 18
  const x = cx + radius * Math.cos(-midAngle * RADIAN)
  const y = cy + radius * Math.sin(-midAngle * RADIAN)

  return (
    <text
      x={x}
      y={y}
      fill="var(--color-text-primary)"
      textAnchor={x > cx ? 'start' : 'end'}
      dominantBaseline="central"
      fontSize={12}
    >
      {`${payload.category.replace('_', ' ')} ${payload.percentage.toFixed(1)}%`}
    </text>
  )
}

const COLORS = ['#1DAE9F', '#4AB8FF', '#E6E4DC', '#B4F0DC', '#FFDFA3', '#FF6B6B', '#7F8DFF', '#FF9F9F']

export default function Spending() {
  const [period, setPeriod] = useState('30')
  const [category, setCategory] = useState('all')

  const { data: transactions } = useQuery<Transaction[]>({
    queryKey: ['transactions', period, category],
    queryFn: async () => {
      const params = new URLSearchParams()
      params.set('period', period)
      if (category !== 'all') {
        params.set('category', category)
      }
      params.set('limit', '50')
      
      const response = await api.get(`/transactions?${params.toString()}`)
      return response.data
    }
  })

  const { data: analytics } = useQuery<SpendingAnalytics>({
    queryKey: ['spending', 'analytics', period],
    queryFn: async () => {
      const response = await api.get(`/spending/analytics?period=${period}`)
      return response.data
    }
  })

  const periods = [
    { value: '7', label: 'Last 7 days' },
    { value: '30', label: 'Last 30 days' },
    { value: '90', label: 'Last 3 months' },
    { value: '365', label: 'Last year' }
  ]

  const categories = [
    { value: 'all', label: 'All Categories' },
    { value: 'food_and_drink', label: 'Food & Drink' },
    { value: 'retail', label: 'Shopping' },
    { value: 'transportation', label: 'Transportation' },
    { value: 'entertainment', label: 'Entertainment' },
    { value: 'healthcare', label: 'Healthcare' },
    { value: 'utilities', label: 'Utilities' },
    { value: 'other', label: 'Other' }
  ]

  const expenseTransactions = transactions?.filter(t => t.amount < 0) || []

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <h1 className="text-3xl font-bold text-[var(--color-text-primary)]">Spending Analysis</h1>
        <div className="flex flex-wrap items-center gap-3">
          <Select
            value={period}
            onChange={(e) => setPeriod(e.target.value)}
            className="w-44"
          >
            {periods.map(p => (
              <option key={p.value} value={p.value}>{p.label}</option>
            ))}
          </Select>
          <Select
            value={category}
            onChange={(e) => setCategory(e.target.value)}
            className="w-48"
          >
            {categories.map(c => (
              <option key={c.value} value={c.value}>{c.label}</option>
            ))}
          </Select>
        </div>
      </div>

      {analytics && (
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          <Card>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium text-[var(--color-text-secondary)]">Total Spent</CardTitle>
              <TrendingDown className="h-4 w-4 text-[var(--color-error)]" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold text-[var(--color-error)]">
                {formatCurrency(analytics.totalSpent)}
              </div>
              <p className="text-xs text-muted-foreground mt-1">
                Last {period} days
              </p>
            </CardContent>
          </Card>

          <Card>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium text-[var(--color-text-secondary)]">Daily Average</CardTitle>
              <Calendar className="h-4 w-4 text-[var(--color-accent-teal)]" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold text-[var(--color-text-primary)]">
                {formatCurrency(analytics.averageDaily)}
              </div>
              <p className="text-xs text-muted-foreground mt-1">
                Per day spending
              </p>
            </CardContent>
          </Card>

          <Card>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium text-[var(--color-text-secondary)]">Transactions</CardTitle>
              <DollarSign className="h-4 w-4 text-[var(--color-accent-blue)]" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold text-[var(--color-text-primary)]">
                {expenseTransactions.length}
              </div>
              <p className="text-xs text-muted-foreground mt-1">
                Total transactions
              </p>
            </CardContent>
          </Card>
        </div>
      )}

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {analytics?.trends && analytics.trends.length > 0 && (
          <Card>
            <CardHeader>
              <CardTitle>Spending Trend</CardTitle>
              <CardDescription>Daily spending over time</CardDescription>
            </CardHeader>
            <CardContent>
              <ResponsiveContainer width="100%" height={300}>
                <LineChart data={analytics.trends}>
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
                    formatter={(value) => [formatCurrency(Number(value)), 'Spent']}
                    labelFormatter={(date) => formatDate(date)}
                    contentStyle={{
                      backgroundColor: 'var(--color-panel-dark)',
                      borderRadius: '1rem',
                      border: '1px solid rgba(255,255,255,0.08)',
                      color: 'var(--color-text-primary)',
                    }}
                  />
                  <Line 
                    type="monotone" 
                    dataKey="amount" 
                    stroke="var(--color-error)" 
                    strokeWidth={2.5}
                    dot={{ fill: 'var(--color-accent-teal)', strokeWidth: 0 }}
                    activeDot={{ r: 6, fill: 'var(--color-accent-teal)' }}
                  />
                </LineChart>
              </ResponsiveContainer>
            </CardContent>
          </Card>
        )}

        {analytics?.categories && analytics.categories.length > 0 && (
          <Card>
            <CardHeader>
              <CardTitle>Spending by Category</CardTitle>
              <CardDescription>Breakdown of expenses by category</CardDescription>
            </CardHeader>
            <CardContent>
              <ResponsiveContainer width="100%" height={300}>
                <RechartsPieChart>
                  <Pie
                    data={analytics.categories}
                    cx="50%"
                    cy="50%"
                    labelLine={false}
                    label={renderCategoryLabel}
                    outerRadius={80}
                    fill="var(--color-accent-blue)"
                    dataKey="amount"
                  >
                    {analytics.categories.map((_, index) => (
                      <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                    ))}
                  </Pie>
                  <Tooltip
                    formatter={(value) => [formatCurrency(Number(value)), 'Amount']}
                    contentStyle={{
                      backgroundColor: 'var(--color-panel-dark)',
                      borderRadius: '1rem',
                      border: '1px solid rgba(255,255,255,0.08)',
                      color: 'var(--color-text-primary)',
                    }}
                  />
                </RechartsPieChart>
              </ResponsiveContainer>
            </CardContent>
          </Card>
        )}
      </div>

      {analytics?.categories && analytics.categories.length > 0 && (
        <Card>
          <CardHeader>
            <CardTitle>Category Details</CardTitle>
            <CardDescription>Detailed breakdown by spending category</CardDescription>
          </CardHeader>
          <CardContent>
            <ResponsiveContainer width="100%" height={400}>
              <BarChart data={analytics.categories}>
                <CartesianGrid stroke="rgba(255,255,255,0.08)" strokeDasharray="3 3" />
                <XAxis 
                  dataKey="category"
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
                  formatter={(value) => [formatCurrency(Number(value)), 'Amount']}
                  contentStyle={{
                    backgroundColor: 'var(--color-panel-dark)',
                    borderRadius: '1rem',
                    border: '1px solid rgba(255,255,255,0.08)',
                    color: 'var(--color-text-primary)',
                  }}
                />
                <Bar dataKey="amount" fill="var(--color-accent-teal)" radius={[12, 12, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </CardContent>
        </Card>
      )}

      {analytics?.topMerchants && analytics.topMerchants.length > 0 && (
        <Card>
          <CardHeader>
            <CardTitle>Top Merchants</CardTitle>
            <CardDescription>Your most frequent spending destinations</CardDescription>
          </CardHeader>
          <CardContent>
            <div className="space-y-4">
              {analytics.topMerchants.map((merchant, index) => (
                <div key={merchant.merchant} className="flex items-center justify-between rounded-lg border border-[rgba(255,255,255,0.08)] bg-[rgba(255,255,255,0.02)] p-3">
                  <div className="flex items-center space-x-3">
                    <div className="flex h-8 w-8 items-center justify-center rounded-full bg-gradient-to-r from-[var(--color-accent-teal)] to-[var(--color-accent-blue)]">
                      <span className="text-sm font-semibold text-[var(--color-bg-dark)]">{index + 1}</span>
                    </div>
                    <div>
                      <p className="font-medium text-[var(--color-text-primary)]">{merchant.merchant}</p>
                      <p className="text-sm text-[var(--color-text-secondary)]">{merchant.count} transactions</p>
                    </div>
                  </div>
                  <div className="text-right">
                    <p className="font-medium text-[var(--color-error)]">{formatCurrency(merchant.amount)}</p>
                  </div>
                </div>
              ))}
            </div>
          </CardContent>
        </Card>
      )}

      <Card>
        <CardHeader>
          <CardTitle>Recent Transactions</CardTitle>
          <CardDescription>Your latest spending activity</CardDescription>
        </CardHeader>
          <CardContent>
            <div className="space-y-3">
              {expenseTransactions.slice(0, 20).map((transaction) => (
              <div key={transaction.id} className="flex items-center justify-between rounded-lg border border-[rgba(255,255,255,0.08)] bg-[rgba(255,255,255,0.02)] p-3">
                <div className="min-w-0 flex-1">
                  <p className="truncate font-medium text-[var(--color-text-primary)]">{transaction.description}</p>
                  <div className="flex items-center space-x-2 text-sm text-[var(--color-text-secondary)]">
                    <span>{transaction.category.replace('_', ' ')}</span>
                    <span>•</span>
                    <span>{transaction.accountName}</span>
                  </div>
                </div>
                <div className="ml-4 text-right">
                  <p className="font-medium text-[var(--color-error)]">
                    {formatCurrency(Math.abs(transaction.amount))}
                  </p>
                  <p className="text-xs text-[var(--color-text-secondary)]">
                    {formatDate(transaction.date)}
                  </p>
                </div>
              </div>
              ))}
              {expenseTransactions.length === 0 && (
              <div className="py-8 text-center text-[var(--color-text-secondary)]">
                <TrendingDown className="mx-auto mb-4 h-12 w-12 text-[var(--color-error)]/60" />
                <p>No spending data found for this period</p>
              </div>
              )}
            </div>
          </CardContent>
      </Card>
    </div>
  )
}
