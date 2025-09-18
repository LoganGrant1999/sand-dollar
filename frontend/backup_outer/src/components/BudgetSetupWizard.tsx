import { useState, useEffect } from 'react'
import { useQuery, useMutation } from '@tanstack/react-query'
import { api } from '@/lib/api'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { Spinner } from '@/components/ui/spinner'
import { formatCurrency } from '@/lib/utils'
import { 
  ArrowLeft,
  ArrowRight,
  CheckCircle2,
  TrendingUp,
  DollarSign,
  Zap,
  Plus,
  X,
  Loader2
} from 'lucide-react'
import type { BudgetPrefillResponse, BudgetWizardRequest, BudgetWizardAllocation, BudgetWizardResponse } from '@/types'
import toast from 'react-hot-toast'

interface BudgetSetupWizardProps {
  onComplete: (budget: BudgetWizardResponse) => void
  onCancel: () => void
}

export default function BudgetSetupWizard({ onComplete, onCancel }: BudgetSetupWizardProps) {
  const [currentStep, setCurrentStep] = useState(1)
  const [isLoading, setIsLoading] = useState(false)
  
  // Form data
  const [income, setIncome] = useState('')
  const [fixedAllocations, setFixedAllocations] = useState<BudgetWizardAllocation[]>([
    { name: 'Rent/Mortgage', amount: 0 },
    { name: 'Utilities', amount: 0 },
    { name: 'Insurance', amount: 0 }
  ])
  const [variableAllocations, setVariableAllocations] = useState<BudgetWizardAllocation[]>([
    { name: 'Groceries', amount: 0 },
    { name: 'Transportation', amount: 0 },
    { name: 'Entertainment', amount: 0 }
  ])

  // Get current month/year
  const now = new Date()
  const currentMonth = now.getMonth() + 1
  const currentYear = now.getFullYear()

  // Fetch prefill data
  const { data: prefillData, isLoading: prefillLoading } = useQuery<BudgetPrefillResponse>({
    queryKey: ['budget-prefill'],
    queryFn: async () => {
      const response = await api.get('/mock/budget/prefill')
      return response.data
    }
  })

  // Create budget mutation
  const createBudgetMutation = useMutation({
    mutationFn: async (budgetRequest: BudgetWizardRequest) => {
      const response = await api.post('/budgets', budgetRequest)
      return response.data
    },
    onSuccess: (response: BudgetWizardResponse) => {
      toast.success('Budget created successfully!')
      onComplete(response)
    },
    onError: (error: any) => {
      toast.error('Failed to create budget: ' + (error.response?.data?.message || error.message))
    }
  })

  // Apply prefill data when available
  useEffect(() => {
    if (prefillData) {
      setIncome(prefillData.incomeEstimate.toString())
      
      // Apply fixed allocations if they have amounts
      const prefillFixed = prefillData.fixed.filter(item => item.amount > 0)
      if (prefillFixed.length > 0) {
        setFixedAllocations(prefillFixed)
      }
      
      // Apply variable suggestions if they have amounts
      const prefillVariable = prefillData.variableSuggestions.filter(item => item.amount > 0)
      if (prefillVariable.length > 0) {
        setVariableAllocations(prefillVariable)
      }
    }
  }, [prefillData])

  const addFixedAllocation = () => {
    setFixedAllocations([...fixedAllocations, { name: '', amount: 0 }])
  }

  const removeFixedAllocation = (index: number) => {
    setFixedAllocations(fixedAllocations.filter((_, i) => i !== index))
  }

  const updateFixedAllocation = (index: number, field: 'name' | 'amount', value: string | number) => {
    const updated = [...fixedAllocations]
    updated[index] = { ...updated[index], [field]: value }
    setFixedAllocations(updated)
  }

  const addVariableAllocation = () => {
    setVariableAllocations([...variableAllocations, { name: '', amount: 0 }])
  }

  const removeVariableAllocation = (index: number) => {
    setVariableAllocations(variableAllocations.filter((_, i) => i !== index))
  }

  const updateVariableAllocation = (index: number, field: 'name' | 'amount', value: string | number) => {
    const updated = [...variableAllocations]
    updated[index] = { ...updated[index], [field]: value }
    setVariableAllocations(updated)
  }

  const handleNext = () => {
    if (currentStep === 1) {
      if (!income || parseFloat(income) <= 0) {
        toast.error('Please enter a valid monthly income')
        return
      }
    } else if (currentStep === 2) {
      const validFixed = fixedAllocations.filter(item => item.name && item.amount > 0)
      if (validFixed.length === 0) {
        toast.error('Please add at least one fixed expense')
        return
      }
    }
    
    if (currentStep < 3) {
      setCurrentStep(currentStep + 1)
    }
  }

  const handlePrevious = () => {
    if (currentStep > 1) {
      setCurrentStep(currentStep - 1)
    }
  }

  const handleSubmit = async () => {
    const validFixed = fixedAllocations.filter(item => item.name && item.amount > 0)
    const validVariable = variableAllocations.filter(item => item.name && item.amount > 0)
    
    if (validFixed.length === 0) {
      toast.error('Please add at least one fixed expense')
      return
    }
    
    if (validVariable.length === 0) {
      toast.error('Please add at least one variable category')
      return
    }

    const budgetRequest: BudgetWizardRequest = {
      month: currentMonth,
      year: currentYear,
      income: parseFloat(income),
      fixed: validFixed,
      variable: validVariable
    }

    setIsLoading(true)
    try {
      await createBudgetMutation.mutateAsync(budgetRequest)
    } finally {
      setIsLoading(false)
    }
  }

  const totalFixed = fixedAllocations.reduce((sum, item) => sum + (item.amount || 0), 0)
  const totalVariable = variableAllocations.reduce((sum, item) => sum + (item.amount || 0), 0)
  const totalBudget = totalFixed + totalVariable
  const remainingIncome = parseFloat(income || '0') - totalBudget

  const steps = [
    { number: 1, title: 'Income & Fixed Costs', description: 'Set your monthly income and fixed expenses' },
    { number: 2, title: 'Variable Categories', description: 'Plan your flexible spending categories' },
    { number: 3, title: 'Review & Save', description: 'Review your budget and save it' }
  ]

  return (
    <div className="max-w-4xl mx-auto space-y-6">
      {/* Progress Steps */}
      <div className="flex flex-col sm:flex-row items-center justify-center space-y-4 sm:space-y-0 sm:space-x-4 mb-8">
        {steps.map((step, index) => (
          <div key={step.number} className="flex items-center w-full sm:w-auto">
            <div className={`flex items-center justify-center w-12 h-12 rounded-2xl border-2 ${
              currentStep >= step.number 
                ? 'bg-primary border-primary text-primary-foreground shadow-soft' 
                : 'border-border text-muted-foreground'
            }`}>
              {currentStep > step.number ? (
                <CheckCircle2 size={20} />
              ) : (
                <span className="text-sm font-medium">{step.number}</span>
              )}
            </div>
            {index < steps.length - 1 && (
              <div className={`hidden sm:block w-16 h-0.5 mx-4 ${
                currentStep > step.number ? 'bg-primary' : 'bg-border'
              }`} />
            )}
          </div>
        ))}
      </div>

      <div className="text-center mb-8">
        <h2 className="text-2xl font-bold text-[var(--color-text-primary)]">{steps[currentStep - 1].title}</h2>
        <p className="text-[var(--color-text-secondary)] mt-2">{steps[currentStep - 1].description}</p>
        {prefillLoading && (
          <div className="mt-4">
            <Spinner text="Loading smart suggestions from your transactions..." />
          </div>
        )}
        {prefillData && !prefillLoading && (
        <Badge variant="outline" className="mt-2 border-[var(--color-success)] text-[var(--color-success)]">
            <Zap size={14} className="mr-1" />
            Smart suggestions applied from your transactions
          </Badge>
        )}
      </div>

      {/* Step 1: Income & Fixed Costs */}
      {currentStep === 1 && (
        <div className="space-y-6">
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center space-x-2">
                <DollarSign className="h-5 w-5 text-[var(--color-success)]" />
                <span>Monthly Income</span>
              </CardTitle>
              <CardDescription>
                Enter your total monthly income after taxes
              </CardDescription>
            </CardHeader>
            <CardContent>
              <div className="relative">
                <span className="absolute left-3 top-3 text-[var(--color-text-secondary)]">$</span>
                <input
                  type="number"
                  value={income}
                  onChange={(e) => setIncome(e.target.value)}
                  className="w-full rounded-lg border border-[rgba(255,255,255,0.08)] bg-[var(--color-input-bg)] py-3 pl-8 pr-4 text-2xl font-bold text-[var(--color-text-primary)] focus:outline-none focus:ring-2 focus:ring-[var(--color-accent-teal)] focus:border-transparent"
                  placeholder="5000"
                  min="0"
                  step="0.01"
                />
              </div>
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle className="flex items-center justify-between">
                <div className="flex items-center space-x-2">
                  <TrendingUp className="h-5 w-5 text-[var(--color-error)]" />
                  <span>Fixed Expenses</span>
                </div>
                <Button size="sm" onClick={addFixedAllocation} variant="outline">
                  <Plus size={16} className="mr-1" />
                  Add Expense
                </Button>
              </CardTitle>
              <CardDescription>
                Regular monthly expenses that don't change (rent, insurance, etc.)
              </CardDescription>
            </CardHeader>
            <CardContent>
              <div className="space-y-4">
                {fixedAllocations.map((allocation, index) => (
                  <div key={index} className="flex items-center space-x-4">
                    <input
                      type="text"
                      value={allocation.name}
                      onChange={(e) => updateFixedAllocation(index, 'name', e.target.value)}
                      className="flex-1 rounded-lg border border-[rgba(255,255,255,0.08)] bg-[var(--color-input-bg)] px-3 py-2 text-[var(--color-text-primary)] focus:outline-none focus:ring-2 focus:ring-[var(--color-accent-teal)] focus:border-transparent"
                      placeholder="Expense name"
                    />
                    <div className="relative">
                      <span className="absolute left-3 top-2.5 text-[var(--color-text-secondary)]">$</span>
                      <input
                        type="number"
                        value={allocation.amount || ''}
                        onChange={(e) => updateFixedAllocation(index, 'amount', parseFloat(e.target.value) || 0)}
                        className="w-32 rounded-lg border border-[rgba(255,255,255,0.08)] bg-[var(--color-input-bg)] py-2 pl-8 pr-4 text-[var(--color-text-primary)] focus:outline-none focus:ring-2 focus:ring-[var(--color-accent-teal)] focus:border-transparent"
                        placeholder="0"
                        min="0"
                        step="0.01"
                      />
                    </div>
                    <Button
                      size="sm"
                      variant="outline"
                      onClick={() => removeFixedAllocation(index)}
                      disabled={fixedAllocations.length <= 1}
                    >
                      <X size={16} />
                    </Button>
                  </div>
                ))}
              </div>
              {totalFixed > 0 && (
                <div className="mt-4 pt-4 border-t border-[rgba(255,255,255,0.08)]">
                  <div className="flex justify-between items-center font-semibold">
                    <span>Total Fixed Expenses:</span>
                    <span className="text-[var(--color-error)]">{formatCurrency(totalFixed * 100)}</span>
                  </div>
                </div>
              )}
            </CardContent>
          </Card>
        </div>
      )}

      {/* Step 2: Variable Categories */}
      {currentStep === 2 && (
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center justify-between">
              <div className="flex items-center space-x-2">
                <TrendingUp className="h-5 w-5 text-[var(--color-accent-blue)]" />
                <span>Variable Categories</span>
              </div>
              <Button size="sm" onClick={addVariableAllocation} variant="outline">
                <Plus size={16} className="mr-1" />
                Add Category
              </Button>
            </CardTitle>
            <CardDescription>
              Flexible spending categories that can vary month to month
            </CardDescription>
          </CardHeader>
          <CardContent>
            <div className="space-y-4">
              {variableAllocations.map((allocation, index) => (
                <div key={index} className="flex items-center space-x-4">
                  <input
                    type="text"
                    value={allocation.name}
                    onChange={(e) => updateVariableAllocation(index, 'name', e.target.value)}
                    className="flex-1 rounded-lg border border-[rgba(255,255,255,0.08)] bg-[var(--color-input-bg)] px-3 py-2 text-[var(--color-text-primary)] focus:outline-none focus:ring-2 focus:ring-[var(--color-accent-teal)] focus:border-transparent"
                    placeholder="Category name"
                  />
                  <div className="relative">
                    <span className="absolute left-3 top-2.5 text-[var(--color-text-secondary)]">$</span>
                    <input
                      type="number"
                      value={allocation.amount || ''}
                      onChange={(e) => updateVariableAllocation(index, 'amount', parseFloat(e.target.value) || 0)}
                      className="w-32 rounded-lg border border-[rgba(255,255,255,0.08)] bg-[var(--color-input-bg)] py-2 pl-8 pr-4 text-[var(--color-text-primary)] focus:outline-none focus:ring-2 focus:ring-[var(--color-accent-teal)] focus:border-transparent"
                      placeholder="0"
                      min="0"
                      step="0.01"
                    />
                  </div>
                  <Button
                    size="sm"
                    variant="outline"
                    onClick={() => removeVariableAllocation(index)}
                    disabled={variableAllocations.length <= 1}
                  >
                    <X size={16} />
                  </Button>
                </div>
              ))}
            </div>
            {totalVariable > 0 && (
              <div className="mt-4 pt-4 border-t border-[rgba(255,255,255,0.08)]">
              <div className="flex justify-between items-center font-semibold">
                <span>Total Variable Budget:</span>
                <span className="text-[var(--color-accent-blue)]">{formatCurrency(totalVariable * 100)}</span>
              </div>
              </div>
            )}
          </CardContent>
        </Card>
      )}

      {/* Step 3: Review & Save */}
      {currentStep === 3 && (
        <div className="space-y-6">
          <Card>
            <CardHeader>
              <CardTitle>Budget Summary</CardTitle>
              <CardDescription>
                Review your budget for {new Date(currentYear, currentMonth - 1).toLocaleDateString('en-US', { month: 'long', year: 'numeric' })}
              </CardDescription>
            </CardHeader>
            <CardContent>
              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
                <div className="text-center p-4 bg-[var(--color-success)]/10 rounded-lg">
                  <p className="text-sm text-[var(--color-text-secondary)]">Monthly Income</p>
                  <p className="text-xl font-bold text-[var(--color-success)]">
                    {formatCurrency(parseFloat(income || '0') * 100)}
                  </p>
                </div>
                <div className="text-center p-4 bg-[var(--color-error)]/10 rounded-lg">
                  <p className="text-sm text-[var(--color-text-secondary)]">Fixed Expenses</p>
                  <p className="text-xl font-bold text-[var(--color-error)]">
                    {formatCurrency(totalFixed * 100)}
                  </p>
                </div>
                <div className="text-center p-4 bg-[var(--color-accent-blue)]/12 rounded-lg">
                  <p className="text-sm text-[var(--color-text-secondary)]">Variable Budget</p>
                  <p className="text-xl font-bold text-[var(--color-accent-blue)]">
                    {formatCurrency(totalVariable * 100)}
                  </p>
                </div>
                <div className={`rounded-lg p-4 text-center ${remainingIncome >= 0 ? 'bg-[var(--color-accent-teal)]/15' : 'bg-[var(--color-warning)]/20'}`}>
                  <p className="text-sm text-[var(--color-text-secondary)]">Remaining</p>
                  <p className={`text-xl font-bold ${remainingIncome >= 0 ? 'text-[var(--color-accent-blue)]' : 'text-[var(--color-warning)]'}`}>
                    {formatCurrency(remainingIncome * 100)}
                  </p>
                </div>
              </div>

              <div className="grid grid-cols-1 gap-8">
                {/* Fixed Expenses */}
                <div>
                  <h3 className="text-lg font-semibold mb-3 text-[var(--color-error)]">Fixed Expenses</h3>
                  <div className="space-y-2">
                    {fixedAllocations.filter(item => item.name && item.amount > 0).map((allocation, index) => (
                      <div key={index} className="flex justify-between items-center p-2 bg-[var(--color-error)]/10 rounded">
                        <span className="text-sm">{allocation.name}</span>
                        <span className="font-medium text-[var(--color-error)]">{formatCurrency(allocation.amount * 100)}</span>
                      </div>
                    ))}
                  </div>
                </div>

                {/* Variable Categories */}
                <div>
                  <h3 className="text-lg font-semibold mb-3 text-[var(--color-accent-blue)]">Variable Categories</h3>
                  <div className="space-y-2">
                    {variableAllocations.filter(item => item.name && item.amount > 0).map((allocation, index) => (
                      <div key={index} className="flex justify-between items-center p-2 bg-[var(--color-accent-blue)]/12 rounded">
                        <span className="text-sm">{allocation.name}</span>
                        <span className="font-medium text-[var(--color-accent-blue)]">{formatCurrency(allocation.amount * 100)}</span>
                      </div>
                    ))}
                  </div>
                </div>
              </div>

              {remainingIncome < 0 && (
                <div className="mt-6 rounded-lg border border-[var(--color-warning)]/60 bg-[var(--color-warning)]/20 p-4">
                  <p className="text-sm text-[var(--color-warning)]">
                    ⚠️ <strong>Budget Warning:</strong> Your planned expenses exceed your income by {formatCurrency(Math.abs(remainingIncome) * 100)}. 
                    Consider reducing some categories or increasing your income.
                  </p>
                </div>
              )}
            </CardContent>
          </Card>
        </div>
      )}

      {/* Navigation */}
      <div className="flex justify-between items-center pt-6">
        <div className="flex flex-col sm:flex-row gap-3 sm:gap-2">
          <Button variant="outline" onClick={onCancel} className="w-full sm:w-auto">
            Cancel
          </Button>
          {currentStep > 1 && (
            <Button variant="outline" onClick={handlePrevious} className="w-full sm:w-auto">
              <ArrowLeft size={16} className="mr-1" />
              Previous
            </Button>
          )}
        </div>

        <div className="flex flex-col sm:flex-row gap-3 sm:gap-2">
          {currentStep < 3 ? (
            <Button onClick={handleNext} className="w-full sm:w-auto">
              Next
              <ArrowRight size={16} className="ml-1" />
            </Button>
          ) : (
            <Button onClick={handleSubmit} disabled={isLoading || createBudgetMutation.isPending} className="w-full sm:w-auto">
              {isLoading || createBudgetMutation.isPending ? (
                <>
                  <Loader2 size={16} className="mr-2 animate-spin" />
                  Saving...
                </>
              ) : (
                'Save Budget'
              )}
            </Button>
          )}
        </div>
      </div>
    </div>
  )
}
