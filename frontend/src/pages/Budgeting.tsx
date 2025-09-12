import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { api } from '@/lib/api'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { formatCurrency } from '@/lib/utils'
import { 
  Calculator,
  Trash2,
  TrendingUp,
  Target,
  Bot
} from 'lucide-react'
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts'
import toast from 'react-hot-toast'

interface BudgetPlan {
  id: number
  name: string
  totalBudget: number
  period: string
  isActive: boolean
  categories: BudgetCategory[]
  createdAt: string
}

interface BudgetCategory {
  id: number
  category: string
  budgetAmount: number
  spentAmount: number
}

interface Goal {
  id: number
  name: string
  targetAmount: number
  currentAmount: number
  targetDate: string
}

export default function Budgeting() {
  const [isGeneratingBudget, setIsGeneratingBudget] = useState(false)
  const queryClient = useQueryClient()

  const { data: budgets } = useQuery<BudgetPlan[]>({
    queryKey: ['budgets'],
    queryFn: async () => {
      const response = await api.get('/budgets')
      return response.data
    }
  })

  const { data: goals } = useQuery<Goal[]>({
    queryKey: ['goals'],
    queryFn: async () => {
      const response = await api.get('/goals')
      return response.data
    }
  })

  const generateBudgetMutation = useMutation({
    mutationFn: async () => {
      const response = await api.post('/ai/budget/generate')
      return response.data
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['budgets'] })
      toast.success('AI budget plan generated successfully!')
    },
    onError: () => {
      toast.error('Failed to generate budget plan')
    }
  })

  const activateBudgetMutation = useMutation({
    mutationFn: async (budgetId: number) => {
      const response = await api.post(`/budgets/${budgetId}/activate`)
      return response.data
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['budgets'] })
      toast.success('Budget plan activated!')
    },
    onError: () => {
      toast.error('Failed to activate budget plan')
    }
  })

  const deleteBudgetMutation = useMutation({
    mutationFn: async (budgetId: number) => {
      await api.delete(`/budgets/${budgetId}`)
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['budgets'] })
      toast.success('Budget plan deleted')
    },
    onError: () => {
      toast.error('Failed to delete budget plan')
    }
  })

  const handleGenerateBudget = async () => {
    setIsGeneratingBudget(true)
    try {
      await generateBudgetMutation.mutateAsync()
    } finally {
      setIsGeneratingBudget(false)
    }
  }

  const activeBudget = budgets?.find(budget => budget.isActive)
  const inactiveBudgets = budgets?.filter(budget => !budget.isActive) || []

  const chartData = activeBudget?.categories?.map(category => ({
    category: category.category,
    budgeted: category.budgetAmount / 100,
    spent: Math.abs(category.spentAmount) / 100,
    remaining: Math.max(0, (category.budgetAmount + category.spentAmount) / 100)
  })) || []

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-3xl font-bold text-gray-900">Budgeting</h1>
        <Button 
          onClick={handleGenerateBudget}
          disabled={isGeneratingBudget}
          className="flex items-center space-x-2"
        >
          <Bot size={18} />
          <span>{isGeneratingBudget ? 'Generating...' : 'Generate AI Budget'}</span>
        </Button>
      </div>

      {activeBudget && (
        <div className="space-y-6">
          <Card>
            <CardHeader>
              <div className="flex items-center justify-between">
                <div>
                  <CardTitle className="flex items-center space-x-2">
                    <Target className="h-5 w-5" />
                    <span>{activeBudget.name}</span>
                  </CardTitle>
                  <CardDescription>
                    Active budget for {activeBudget.period} • Total: {formatCurrency(activeBudget.totalBudget)}
                  </CardDescription>
                </div>
              </div>
            </CardHeader>
            <CardContent>
              <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-6">
                <div className="text-center p-4 bg-blue-50 rounded-lg">
                  <p className="text-sm text-gray-600">Total Budget</p>
                  <p className="text-2xl font-bold text-blue-600">
                    {formatCurrency(activeBudget.totalBudget)}
                  </p>
                </div>
                <div className="text-center p-4 bg-red-50 rounded-lg">
                  <p className="text-sm text-gray-600">Total Spent</p>
                  <p className="text-2xl font-bold text-red-600">
                    {formatCurrency(activeBudget.categories.reduce((sum, cat) => sum + Math.abs(cat.spentAmount), 0))}
                  </p>
                </div>
                <div className="text-center p-4 bg-green-50 rounded-lg">
                  <p className="text-sm text-gray-600">Remaining</p>
                  <p className="text-2xl font-bold text-green-600">
                    {formatCurrency(activeBudget.categories.reduce((sum, cat) => sum + Math.max(0, cat.budgetAmount + cat.spentAmount), 0))}
                  </p>
                </div>
              </div>

              <div className="space-y-4">
                <h3 className="text-lg font-semibold">Category Breakdown</h3>
                {activeBudget.categories.map((category) => {
                  const spent = Math.abs(category.spentAmount)
                  const budget = category.budgetAmount
                  const remaining = Math.max(0, budget - spent)
                  const percentage = budget > 0 ? (spent / budget) * 100 : 0
                  const isOverBudget = spent > budget

                  return (
                    <div key={category.id} className="p-4 border rounded-lg">
                      <div className="flex items-center justify-between mb-2">
                        <h4 className="font-medium">{category.category}</h4>
                        <div className="text-right">
                          <p className="text-sm text-gray-600">
                            {formatCurrency(spent)} of {formatCurrency(budget)}
                          </p>
                          <p className={`text-xs font-medium ${isOverBudget ? 'text-red-600' : 'text-green-600'}`}>
                            {isOverBudget ? `Over by ${formatCurrency(spent - budget)}` : `${formatCurrency(remaining)} remaining`}
                          </p>
                        </div>
                      </div>
                      <div className="w-full bg-gray-200 rounded-full h-2">
                        <div 
                          className={`h-2 rounded-full transition-all ${isOverBudget ? 'bg-red-500' : 'bg-blue-500'}`}
                          style={{ width: `${Math.min(100, percentage)}%` }}
                        />
                      </div>
                      <p className="text-xs text-gray-500 mt-1">{percentage.toFixed(1)}% used</p>
                    </div>
                  )
                })}
              </div>
            </CardContent>
          </Card>

          {chartData.length > 0 && (
            <Card>
              <CardHeader>
                <CardTitle>Budget vs Spending</CardTitle>
                <CardDescription>Compare your budgeted amounts with actual spending</CardDescription>
              </CardHeader>
              <CardContent>
                <ResponsiveContainer width="100%" height={400}>
                  <BarChart data={chartData}>
                    <CartesianGrid strokeDasharray="3 3" />
                    <XAxis dataKey="category" />
                    <YAxis />
                    <Tooltip formatter={(value) => formatCurrency(Number(value) * 100)} />
                    <Bar dataKey="budgeted" fill="#3b82f6" name="Budgeted" />
                    <Bar dataKey="spent" fill="#ef4444" name="Spent" />
                  </BarChart>
                </ResponsiveContainer>
              </CardContent>
            </Card>
          )}
        </div>
      )}

      {inactiveBudgets.length > 0 && (
        <Card>
          <CardHeader>
            <CardTitle>Saved Budget Plans</CardTitle>
            <CardDescription>Your previously created budget plans</CardDescription>
          </CardHeader>
          <CardContent>
            <div className="space-y-4">
              {inactiveBudgets.map((budget) => (
                <div key={budget.id} className="p-4 border rounded-lg">
                  <div className="flex items-center justify-between">
                    <div>
                      <h3 className="font-semibold">{budget.name}</h3>
                      <p className="text-sm text-gray-600">
                        {formatCurrency(budget.totalBudget)} • {budget.period}
                      </p>
                      <p className="text-xs text-gray-500">
                        Created {new Date(budget.createdAt).toLocaleDateString()}
                      </p>
                    </div>
                    <div className="flex items-center space-x-2">
                      <Button
                        size="sm"
                        onClick={() => activateBudgetMutation.mutate(budget.id)}
                        disabled={activateBudgetMutation.isPending}
                      >
                        <TrendingUp size={16} className="mr-1" />
                        Activate
                      </Button>
                      <Button
                        size="sm"
                        variant="destructive"
                        onClick={() => deleteBudgetMutation.mutate(budget.id)}
                        disabled={deleteBudgetMutation.isPending}
                      >
                        <Trash2 size={16} />
                      </Button>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </CardContent>
        </Card>
      )}

      {goals && goals.length > 0 && (
        <Card>
          <CardHeader>
            <CardTitle>Financial Goals</CardTitle>
            <CardDescription>Track your progress towards financial milestones</CardDescription>
          </CardHeader>
          <CardContent>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              {goals.map((goal) => {
                const percentage = goal.targetAmount > 0 ? (goal.currentAmount / goal.targetAmount) * 100 : 0
                const remaining = Math.max(0, goal.targetAmount - goal.currentAmount)

                return (
                  <div key={goal.id} className="p-4 border rounded-lg">
                    <div className="flex items-center justify-between mb-2">
                      <h3 className="font-semibold">{goal.name}</h3>
                      <p className="text-sm text-gray-600">
                        Target: {new Date(goal.targetDate).toLocaleDateString()}
                      </p>
                    </div>
                    <div className="space-y-2">
                      <div className="flex justify-between text-sm">
                        <span>{formatCurrency(goal.currentAmount)} saved</span>
                        <span>{formatCurrency(goal.targetAmount)} goal</span>
                      </div>
                      <div className="w-full bg-gray-200 rounded-full h-2">
                        <div 
                          className="h-2 bg-green-500 rounded-full transition-all"
                          style={{ width: `${Math.min(100, percentage)}%` }}
                        />
                      </div>
                      <div className="flex justify-between text-xs text-gray-500">
                        <span>{percentage.toFixed(1)}% complete</span>
                        <span>{formatCurrency(remaining)} to go</span>
                      </div>
                    </div>
                  </div>
                )
              })}
            </div>
          </CardContent>
        </Card>
      )}

      {!activeBudget && inactiveBudgets.length === 0 && (
        <Card>
          <CardContent className="flex flex-col items-center justify-center py-12">
            <Calculator className="h-12 w-12 text-gray-400 mb-4" />
            <h3 className="text-lg font-semibold text-gray-900 mb-2">No Budget Plans Yet</h3>
            <p className="text-gray-600 text-center mb-6">
              Create your first budget plan using AI or start from scratch to take control of your finances.
            </p>
            <Button onClick={handleGenerateBudget} disabled={isGeneratingBudget}>
              <Bot size={18} className="mr-2" />
              {isGeneratingBudget ? 'Generating...' : 'Generate AI Budget'}
            </Button>
          </CardContent>
        </Card>
      )}
    </div>
  )
}