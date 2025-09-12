import { useQuery, useQueryClient } from '@tanstack/react-query'
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
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, PieChart, Pie, Cell } from 'recharts'

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

interface SpendingSummary {
  totalIncome: number
  totalExpenses: number
  netCashFlow: number
  categories: Array<{
    category: string
    amount: number
    percentage: number
  }>
}

const COLORS = ['#0088FE', '#00C49F', '#FFBB28', '#FF8042', '#8884d8', '#82ca9d']

export default function Dashboard() {
  const queryClient = useQueryClient()
  const isMockMode = import.meta.env.VITE_DATA_MODE === 'mock'
  
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

  const { data: spendingSummary } = useQuery<SpendingSummary>({
    queryKey: ['spending', 'summary'],
    queryFn: async () => {
      const response = await api.get('/spending/summary?period=30')
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

  const totalBalance = totalBalanceData?.balance || 0

  return (
    <div className="space-y-6">
      {isMockMode && (
        <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-4">
          <div className="flex items-center justify-between">
            <div className="flex items-center space-x-2">
              <Database className="h-5 w-5 text-yellow-600" />
              <div>
                <p className="text-sm font-medium text-yellow-800">Mock Data Mode</p>
                <p className="text-xs text-yellow-600">Using demo data for development</p>
              </div>
            </div>
            <div className="flex space-x-2">
              <Button 
                size="sm" 
                variant="outline" 
                onClick={seedMockData}
                className="text-yellow-700 border-yellow-300 hover:bg-yellow-100"
              >
                <Database className="h-3 w-3 mr-1" />
                Seed Data
              </Button>
              <Button 
                size="sm" 
                variant="outline" 
                onClick={resetMockData}
                className="text-yellow-700 border-yellow-300 hover:bg-yellow-100"
              >
                <RefreshCw className="h-3 w-3 mr-1" />
                Reset Data
              </Button>
            </div>
          </div>
        </div>
      )}
      
      <div className="flex items-center justify-between">
        <h1 className="text-3xl font-bold text-gray-900">Dashboard</h1>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Total Balance</CardTitle>
            <DollarSign className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{formatCurrency(totalBalance)}</div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Monthly Income</CardTitle>
            <TrendingUp className="h-4 w-4 text-green-600" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold text-green-600">
              {formatCurrency(spendingSummary?.totalIncome || 0)}
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Monthly Expenses</CardTitle>
            <TrendingDown className="h-4 w-4 text-red-600" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold text-red-600">
              {formatCurrency(spendingSummary?.totalExpenses || 0)}
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Net Cash Flow</CardTitle>
            <PiggyBank className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className={`text-2xl font-bold ${(spendingSummary?.netCashFlow || 0) >= 0 ? 'text-green-600' : 'text-red-600'}`}>
              {formatCurrency(spendingSummary?.netCashFlow || 0)}
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
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis 
                    dataKey="date" 
                    tickFormatter={(date) => new Date(date).toLocaleDateString()} 
                  />
                  <YAxis />
                  <Tooltip 
                    formatter={(value) => [formatCurrency(Number(value)), 'Balance']}
                    labelFormatter={(date) => new Date(date).toLocaleDateString()}
                  />
                  <Line 
                    type="monotone" 
                    dataKey="balance" 
                    stroke="#2563eb" 
                    strokeWidth={2}
                    dot={{ fill: '#2563eb' }}
                  />
                </LineChart>
              </ResponsiveContainer>
            ) : (
              <div className="flex items-center justify-center h-[300px] text-gray-500">
                No balance trend data available
              </div>
            )}
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Spending by Category</CardTitle>
            <CardDescription>Last 30 days breakdown</CardDescription>
          </CardHeader>
          <CardContent>
            {spendingSummary?.categories && spendingSummary.categories.length > 0 ? (
              <ResponsiveContainer width="100%" height={300}>
                <PieChart>
                  <Pie
                    data={spendingSummary.categories}
                    cx="50%"
                    cy="50%"
                    labelLine={false}
                    label={({ category, percentage }) => `${category} ${percentage}%`}
                    outerRadius={80}
                    fill="#8884d8"
                    dataKey="amount"
                  >
                    {spendingSummary.categories.map((_, index) => (
                      <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                    ))}
                  </Pie>
                  <Tooltip formatter={(value) => formatCurrency(Number(value))} />
                </PieChart>
              </ResponsiveContainer>
            ) : (
              <div className="flex items-center justify-center h-[300px] text-gray-500">
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
                <div key={account.id} className="flex items-center justify-between p-3 border rounded-lg">
                  <div className="flex items-center space-x-3">
                    {account.type === 'depository' ? (
                      <Wallet className="h-5 w-5 text-blue-600" />
                    ) : (
                      <CreditCard className="h-5 w-5 text-purple-600" />
                    )}
                    <div>
                      <p className="font-medium">{account.name}</p>
                      <p className="text-sm text-gray-500 capitalize">{account.type}</p>
                    </div>
                  </div>
                  <div className="text-right">
                    <p className="font-medium">{formatCurrency(account.balance)}</p>
                  </div>
                </div>
              ))}
              {!accounts?.length && (
                <p className="text-gray-500 text-center py-4">No accounts connected</p>
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
                <div key={transaction.id} className="flex items-center justify-between p-3 border rounded-lg">
                  <div className="flex-1">
                    <p className="font-medium truncate">{transaction.description}</p>
                    <p className="text-sm text-gray-500">{transaction.category}</p>
                  </div>
                  <div className="text-right ml-4">
                    <p className={`font-medium ${transaction.amount < 0 ? 'text-red-600' : 'text-green-600'}`}>
                      {formatCurrency(Math.abs(transaction.amount))}
                    </p>
                    <p className="text-xs text-gray-500">{new Date(transaction.date).toLocaleDateString()}</p>
                  </div>
                </div>
              ))}
              {!recentTransactions?.length && (
                <p className="text-gray-500 text-center py-4">No recent transactions</p>
              )}
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  )
}