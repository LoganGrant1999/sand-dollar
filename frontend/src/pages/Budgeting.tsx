import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { api, generateBudgetJSON, getCurrentBudget } from '@/lib/api'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Spinner } from '@/components/ui/spinner'
import { CardSkeleton } from '@/components/ui/card-skeleton'
import { ListSkeleton } from '@/components/ui/list-skeleton'
import { formatCurrency } from '@/lib/utils'
import { 
  Calculator,
  Trash2,
  TrendingUp,
  Target,
  Bot,
  Plus,
  X,
  Zap
} from 'lucide-react'
import type { BudgetRequest, BudgetPlanResponse, BudgetWizardResponse } from '@/types'
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts'
import toast from 'react-hot-toast'
import BudgetSetupWizard from '@/components/BudgetSetupWizard'

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
  const [showBudgetForm, setShowBudgetForm] = useState(false)
  const [showWizard, setShowWizard] = useState(false)
  const [budgetFormData, setBudgetFormData] = useState({
    monthlyIncome: '',
    fixedExpenses: '',
    savingsRate: '',
    categories: ['Groceries', 'Dining Out', 'Transportation', 'Utilities', 'Insurance', 'Entertainment']
  })
  const [generatedBudget, setGeneratedBudget] = useState<BudgetPlanResponse | null>(null)
  const queryClient = useQueryClient()

  const { data: currentBudget, isLoading: budgetsLoading } = useQuery({
    queryKey: ['budgets', 'current'],
    queryFn: getCurrentBudget,
    retry: false
  })

  const { data: goals } = useQuery<Goal[]>({
    queryKey: ['goals'],
    queryFn: async () => {
      const response = await api.get('/goals')
      return response.data
    }
  })

  const generateBudgetMutation = useMutation({
    mutationFn: async (budgetRequest: BudgetRequest) => {
      return await generateBudgetJSON(budgetRequest)
    },
    onSuccess: (response: BudgetPlanResponse) => {
      setGeneratedBudget(response)
      toast.success('AI budget plan generated successfully!')
    },
    onError: (error) => {
      toast.error('Failed to generate budget plan: ' + error.message)
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
    if (!showBudgetForm) {
      setShowBudgetForm(true)
      return
    }

    const { monthlyIncome, fixedExpenses, savingsRate, categories } = budgetFormData
    
    if (!monthlyIncome || !fixedExpenses || !savingsRate) {
      toast.error('Please fill in all required fields')
      return
    }

    const budgetRequest: BudgetRequest = {
      monthlyIncome: parseFloat(monthlyIncome),
      fixedExpenses: parseFloat(fixedExpenses),
      savingsRate: parseFloat(savingsRate) / 100, // Convert percentage to decimal
      categories
    }

    setIsGeneratingBudget(true)
    try {
      await generateBudgetMutation.mutateAsync(budgetRequest)
    } finally {
      setIsGeneratingBudget(false)
    }
  }

  const addCategory = () => {
    setBudgetFormData(prev => ({
      ...prev,
      categories: [...prev.categories, '']
    }))
  }

  const removeCategory = (index: number) => {
    setBudgetFormData(prev => ({
      ...prev,
      categories: prev.categories.filter((_, i) => i !== index)
    }))
  }

  const updateCategory = (index: number, value: string) => {
    setBudgetFormData(prev => ({
      ...prev,
      categories: prev.categories.map((cat, i) => i === index ? value : cat)
    }))
  }

  const handleWizardComplete = (_budget: BudgetWizardResponse) => {
    setShowWizard(false)
    // Refresh the budgets query to show the new budget
    queryClient.invalidateQueries({ queryKey: ['budgets'] })
    toast.success('Budget created successfully!')
  }

  const handleWizardCancel = () => {
    setShowWizard(false)
  }

  // Handle the case where there might be an error message or no budget
  const hasBudget = currentBudget && !currentBudget.message && !currentBudget.error
  const activeBudget = hasBudget ? currentBudget : null

  // For now, we don't show inactive budgets since we're focusing on current budget
  // We'll use the history endpoint later if needed
  const inactiveBudgets: BudgetPlan[] = []

  // Chart data would need to be adapted to the new budget structure
  // For now, we'll set it to empty since the current structure is different
  const chartData: Array<{ category: string; budgeted: number; spent: number }> = []

  // Show wizard if requested
  if (showWizard) {
    return (
      <div className="space-y-6">
        <div className="flex items-center justify-between mb-6">
          <h1 className="text-3xl font-bold text-[var(--color-text-primary)]">Create Budget</h1>
        </div>
        <BudgetSetupWizard 
          onComplete={handleWizardComplete}
          onCancel={handleWizardCancel}
        />
      </div>
    )
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-3xl font-bold text-[var(--color-text-primary)]">Budgeting</h1>
        <div className="flex space-x-2">
          <Button 
            onClick={() => setShowWizard(true)}
            className="flex items-center space-x-2"
            variant="default"
          >
            <Zap size={18} />
            <span>Setup Wizard</span>
          </Button>
          <Button 
            onClick={handleGenerateBudget}
            disabled={isGeneratingBudget}
            className="flex items-center space-x-2"
            variant="outline"
          >
            <Bot size={18} />
            <span>{isGeneratingBudget ? 'Generating...' : showBudgetForm ? 'Generate Budget' : 'Generate AI Budget'}</span>
          </Button>
        </div>
      </div>

      {/* Budget Generation Form */}
      {showBudgetForm && (
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center space-x-2">
              <Bot className="h-5 w-5 text-[var(--color-accent-blue)]" />
              <span>Generate AI Budget Plan</span>
            </CardTitle>
            <CardDescription>
              Tell us about your finances and we'll create a personalized budget plan
            </CardDescription>
          </CardHeader>
          <CardContent>
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6 mb-8">
              <div className="space-y-3">
                <Label htmlFor="monthlyIncome">Monthly Income ($)</Label>
                <Input
                  id="monthlyIncome"
                  type="number"
                  value={budgetFormData.monthlyIncome}
                  onChange={(e) => setBudgetFormData(prev => ({ ...prev, monthlyIncome: e.target.value }))}
                  placeholder="6000"
                />
              </div>
              <div className="space-y-3">
                <Label htmlFor="fixedExpenses">Fixed Expenses ($)</Label>
                <Input
                  id="fixedExpenses"
                  type="number"
                  value={budgetFormData.fixedExpenses}
                  onChange={(e) => setBudgetFormData(prev => ({ ...prev, fixedExpenses: e.target.value }))}
                  placeholder="2200"
                />
              </div>
              <div className="space-y-3">
                <Label htmlFor="savingsRate">Savings Rate (%)</Label>
                <Input
                  id="savingsRate"
                  type="number"
                  value={budgetFormData.savingsRate}
                  onChange={(e) => setBudgetFormData(prev => ({ ...prev, savingsRate: e.target.value }))}
                  placeholder="20"
                  min="0"
                  max="100"
                />
              </div>
            </div>

            <div className="mb-8">
              <Label className="text-base font-semibold mb-4 block">Budget Categories</Label>
              <div className="space-y-4">
                {budgetFormData.categories.map((category, index) => (
                  <div key={index} className="flex flex-col sm:flex-row items-stretch sm:items-center gap-3">
                    <Input
                      type="text"
                      value={category}
                      onChange={(e) => updateCategory(index, e.target.value)}
                      placeholder="Enter category name"
                      className="flex-1"
                    />
                    <Button
                      size="sm"
                      variant="outline"
                      onClick={() => removeCategory(index)}
                      disabled={budgetFormData.categories.length <= 1}
                      className="w-full sm:w-auto"
                    >
                      <X size={16} />
                      <span className="sm:hidden ml-2">Remove</span>
                    </Button>
                  </div>
                ))}
                <Button
                  size="sm"
                  variant="outline"
                  onClick={addCategory}
                  className="w-full sm:w-auto flex items-center justify-center space-x-2"
                >
                  <Plus size={16} />
                  <span>Add Category</span>
                </Button>
              </div>
            </div>

            <div className="flex flex-col sm:flex-row justify-end gap-3 sm:gap-2">
              <Button
                variant="outline"
                onClick={() => {
                  setShowBudgetForm(false)
                  setGeneratedBudget(null)
                }}
                className="w-full sm:w-auto"
              >
                Cancel
              </Button>
              <Button
                onClick={handleGenerateBudget}
                disabled={isGeneratingBudget}
                className="w-full sm:w-auto flex items-center justify-center space-x-2"
              >
                <Bot size={16} />
                <span>{isGeneratingBudget ? 'Generating...' : 'Generate Budget'}</span>
              </Button>
            </div>
          </CardContent>
        </Card>
      )}

      {/* AI Budget Generation Loading */}
      {isGeneratingBudget && (
        <Card>
          <CardContent className="flex flex-col items-center justify-center py-12">
            <Spinner size="lg" text="Generating your personalized AI budget plan..." />
          </CardContent>
        </Card>
      )}

      {/* Generated Budget Display */}
      {generatedBudget && !isGeneratingBudget && (
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center space-x-2">
              <Bot className="h-5 w-5 text-[var(--color-success)]" />
              <span>AI Generated Budget Plan</span>
            </CardTitle>
            <CardDescription>
              Here's your personalized budget recommendation
            </CardDescription>
          </CardHeader>
          <CardContent className="space-y-6">
            {/* Budget Summary */}
            <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
              <div className="text-center p-4 bg-[var(--color-accent-blue)]/10 rounded-lg">
                <p className="text-sm text-[var(--color-text-secondary)]">Monthly Income</p>
                <p className="text-xl font-bold text-[var(--color-accent-blue)]">
                  {formatCurrency(generatedBudget.monthlyIncome)}
                </p>
              </div>
              <div className="text-center p-4 bg-[var(--color-error)]/10 rounded-lg">
                <p className="text-sm text-[var(--color-text-secondary)]">Fixed Expenses</p>
                <p className="text-xl font-bold text-[var(--color-error)]">
                  {formatCurrency(generatedBudget.fixedExpenses)}
                </p>
              </div>
              <div className="text-center p-4 bg-[var(--color-success)]/10 rounded-lg">
                <p className="text-sm text-[var(--color-text-secondary)]">Target Savings</p>
                <p className="text-xl font-bold text-[var(--color-success)]">
                  {formatCurrency(generatedBudget.targetSavings)}
                </p>
              </div>
              <div className="text-center p-4 bg-[var(--color-accent-blue)]/12 rounded-lg">
                <p className="text-sm text-[var(--color-text-secondary)]">Variable Budget</p>
                <p className="text-xl font-bold text-[var(--color-accent-blue)]">
                  {formatCurrency(generatedBudget.variableTotal)}
                </p>
              </div>
            </div>

            {/* Category Allocations with Progress Bars */}
            <div className="space-y-4">
              <h3 className="text-lg font-semibold">Category Allocations</h3>
              {generatedBudget.allocations.map((allocation, index) => {
                const percentageOfIncome = (allocation.pctOfIncome * 100)
                const maxPercent = 30 // Max percentage for visual scaling
                const visualWidth = Math.min((percentageOfIncome / maxPercent) * 100, 100)
                
                return (
                  <div key={index} className="p-4 border border-[rgba(255,255,255,0.08)] rounded-lg bg-[rgba(255,255,255,0.02)]">
                    <div className="flex items-center justify-between mb-2">
                      <h4 className="font-medium">{allocation.category}</h4>
                      <div className="text-right">
                        <p className="text-sm font-semibold text-[var(--color-text-primary)]">
                          {formatCurrency(allocation.amount)}
                        </p>
                        <p className="text-xs text-[var(--color-text-secondary)]">
                          {percentageOfIncome.toFixed(1)}% of income
                        </p>
                      </div>
                    </div>
                    
                    {/* Progress Bar */}
                    <div className="w-full bg-[rgba(255,255,255,0.1)] rounded-full h-3 mb-2">
                      <div 
                        className="h-3 bg-gradient-to-r from-[var(--color-accent-teal)] to-[var(--color-accent-blue)] rounded-full transition-all duration-700 ease-in-out"
                        style={{ width: `${visualWidth}%` }}
                      />
                    </div>
                    
                    <div className="flex justify-between text-xs text-[var(--color-text-secondary)]">
                      <span>{percentageOfIncome.toFixed(1)}% of income</span>
                      <span>{formatCurrency(allocation.amount)} allocated</span>
                    </div>
                  </div>
                )
              })}
            </div>

            {/* Recommendations */}
            {generatedBudget.recommendations && generatedBudget.recommendations.length > 0 && (
              <div className="space-y-2">
                <h3 className="text-lg font-semibold">Recommendations</h3>
                <div className="bg-[var(--color-warning)]/20 p-4 rounded-lg border border-[var(--color-warning)]/60">
                  <ul className="space-y-1">
                    {generatedBudget.recommendations.map((rec, index) => (
                      <li key={index} className="text-sm text-[var(--color-warning)] flex items-start">
                        <span className="inline-block w-2 h-2 bg-[var(--color-warning)] rounded-full mt-2 mr-2 flex-shrink-0" />
                        {rec}
                      </li>
                    ))}
                  </ul>
                </div>
              </div>
            )}

            <div className="flex flex-col sm:flex-row justify-end gap-3 sm:gap-2">
              <Button
                variant="outline"
                onClick={() => {
                  setGeneratedBudget(null)
                  setShowBudgetForm(false)
                }}
                className="w-full sm:w-auto"
              >
                Close
              </Button>
              <Button onClick={() => toast.success('Budget saved! (Demo)')} className="w-full sm:w-auto">
                Save Budget Plan
              </Button>
            </div>
          </CardContent>
        </Card>
      )}

      {budgetsLoading && (
        <div className="space-y-6">
          <Card>
            <CardHeader>
              <div className="space-y-2">
                <div className="h-6 bg-[rgba(255,255,255,0.1)] rounded animate-pulse w-1/3"></div>
                <div className="h-4 bg-[rgba(255,255,255,0.1)] rounded animate-pulse w-1/2"></div>
              </div>
            </CardHeader>
            <CardContent>
              <CardSkeleton variant="summary" className="mb-6" />
              <ListSkeleton variant="allocations" count={5} />
            </CardContent>
          </Card>
          <Card>
            <CardHeader>
              <div className="space-y-2">
                <div className="h-5 bg-[rgba(255,255,255,0.1)] rounded animate-pulse w-1/4"></div>
                <div className="h-4 bg-[rgba(255,255,255,0.1)] rounded animate-pulse w-3/4"></div>
              </div>
            </CardHeader>
            <CardContent>
              <div className="h-64 bg-[rgba(255,255,255,0.1)] rounded animate-pulse"></div>
            </CardContent>
          </Card>
          <CardSkeleton />
        </div>
      )}

      {activeBudget && !budgetsLoading && (
        <div className="space-y-6">
          <Card>
            <CardHeader>
              <div className="flex items-center justify-between">
                <div>
                  <CardTitle className="flex items-center space-x-2">
                    <Target className="h-5 w-5" />
                    <span>Current Budget Plan</span>
                  </CardTitle>
                  <CardDescription className="flex items-center space-x-4">
                    <span>Period: {activeBudget.period} • {activeBudget.startDate} to {activeBudget.endDate}</span>
                    {activeBudget.createdAt && (
                      <span className="text-xs text-[var(--color-text-secondary)]">
                        Last updated: {new Date(activeBudget.createdAt).toLocaleDateString()}
                      </span>
                    )}
                  </CardDescription>
                </div>
              </div>
            </CardHeader>
            <CardContent>
              {activeBudget.plan && (
                <>
                  <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-6">
                    <div className="text-center p-4 bg-[var(--color-accent-blue)]/10 rounded-lg">
                      <p className="text-sm text-[var(--color-text-secondary)]">Monthly Income</p>
                      <p className="text-2xl font-bold text-[var(--color-accent-blue)]">
                        {formatCurrency(activeBudget.plan.monthlyIncome || 0)}
                      </p>
                    </div>
                    <div className="text-center p-4 bg-[var(--color-error)]/10 rounded-lg">
                      <p className="text-sm text-[var(--color-text-secondary)]">Fixed Expenses</p>
                      <p className="text-2xl font-bold text-[var(--color-error)]">
                        {formatCurrency(activeBudget.plan.fixedExpenses || 0)}
                      </p>
                    </div>
                    <div className="text-center p-4 bg-[var(--color-success)]/10 rounded-lg">
                      <p className="text-sm text-[var(--color-text-secondary)]">Target Savings</p>
                      <p className="text-2xl font-bold text-[var(--color-success)]">
                        {formatCurrency(activeBudget.plan.targetSavings || 0)}
                      </p>
                    </div>
                  </div>

                  {activeBudget.plan.targets && (
                    <div className="space-y-4">
                      <h3 className="text-lg font-semibold">Budget Targets</h3>
                      {activeBudget.plan.targets.map((target: any, index: number) => {
                        const budget = target.limitCents || 0
                        const budgetDollars = budget / 100

                        return (
                          <div key={index} className="p-4 border border-[rgba(255,255,255,0.08)] rounded-lg bg-[rgba(255,255,255,0.02)]">
                            <div className="flex items-center justify-between mb-2">
                              <h4 className="font-medium">{target.name}</h4>
                              <div className="text-right">
                                <p className="text-sm text-[var(--color-text-secondary)]">
                                  Budget: {formatCurrency(budgetDollars)}
                                </p>
                              </div>
                            </div>
                            <div className="w-full bg-[rgba(255,255,255,0.1)] rounded-full h-2">
                              <div 
                                className="h-2 rounded-full bg-[var(--color-accent-blue)]/100 transition-all"
                                style={{ width: '0%' }}
                              />
                            </div>
                            <p className="text-xs text-[var(--color-text-secondary)] mt-1">Spending data will be available when connected to Plaid</p>
                          </div>
                        )
                      })}
                    </div>
                  )}
                </>
              )}
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
                      formatter={(value) => formatCurrency(Number(value) * 100)}
                      contentStyle={{
                        backgroundColor: 'var(--color-panel-dark)',
                        borderRadius: '1rem',
                        border: '1px solid rgba(255,255,255,0.08)',
                        color: 'var(--color-text-primary)',
                      }}
                    />
                    <Bar dataKey="budgeted" fill="var(--color-accent-blue)" name="Budgeted" radius={[12, 12, 0, 0]} />
                    <Bar dataKey="spent" fill="var(--color-error)" name="Spent" radius={[12, 12, 0, 0]} />
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
                <div key={budget.id} className="p-4 border border-[rgba(255,255,255,0.08)] rounded-lg bg-[rgba(255,255,255,0.02)]">
                  <div className="flex items-center justify-between">
                    <div>
                      <h3 className="font-semibold">{budget.name}</h3>
                      <p className="text-sm text-[var(--color-text-secondary)]">
                        {formatCurrency(budget.totalBudget)} • {budget.period}
                      </p>
                      <p className="text-xs text-[var(--color-text-secondary)]">
                        Created {new Date(budget.createdAt).toLocaleDateString()}
                      </p>
                    </div>
                    <div className="flex items-center space-x-2">
                      <Button
                        size="sm"
                        onClick={() => activateBudgetMutation.mutate(budget.id)}
                        disabled={activateBudgetMutation.isPending}
                      >
                        {activateBudgetMutation.isPending ? (
                          <Spinner size="sm" />
                        ) : (
                          <>
                            <TrendingUp size={16} className="mr-1" />
                            Activate
                          </>
                        )}
                      </Button>
                      <Button
                        size="sm"
                        variant="destructive"
                        onClick={() => deleteBudgetMutation.mutate(budget.id)}
                        disabled={deleteBudgetMutation.isPending}
                      >
                        {deleteBudgetMutation.isPending ? (
                          <Spinner size="sm" />
                        ) : (
                          <Trash2 size={16} />
                        )}
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
                  <div key={goal.id} className="p-4 border border-[rgba(255,255,255,0.08)] rounded-lg bg-[rgba(255,255,255,0.02)]">
                    <div className="flex items-center justify-between mb-2">
                      <h3 className="font-semibold">{goal.name}</h3>
                      <p className="text-sm text-[var(--color-text-secondary)]">
                        Target: {new Date(goal.targetDate).toLocaleDateString()}
                      </p>
                    </div>
                    <div className="space-y-2">
                      <div className="flex justify-between text-sm">
                        <span>{formatCurrency(goal.currentAmount)} saved</span>
                        <span>{formatCurrency(goal.targetAmount)} goal</span>
                      </div>
                      <div className="w-full bg-[rgba(255,255,255,0.1)] rounded-full h-2">
                        <div 
                          className="h-2 bg-[var(--color-success)]/100 rounded-full transition-all"
                          style={{ width: `${Math.min(100, percentage)}%` }}
                        />
                      </div>
                      <div className="flex justify-between text-xs text-[var(--color-text-secondary)]">
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
            <Calculator className="h-12 w-12 text-[var(--color-text-secondary)]/80 mb-4" />
            <h3 className="text-lg font-semibold text-[var(--color-text-primary)] mb-2">No Budget Plans Yet</h3>
            <p className="text-[var(--color-text-secondary)] text-center mb-6">
              Create your first budget plan using our smart wizard that can prefill data from your transactions.
            </p>
            <div className="flex space-x-3">
              <Button onClick={() => setShowWizard(true)}>
                <Zap size={18} className="mr-2" />
                Setup Wizard
              </Button>
              <Button onClick={handleGenerateBudget} disabled={isGeneratingBudget} variant="outline">
                <Bot size={18} className="mr-2" />
                {isGeneratingBudget ? 'Generating...' : 'AI Budget'}
              </Button>
            </div>
          </CardContent>
        </Card>
      )}
    </div>
  )
}
