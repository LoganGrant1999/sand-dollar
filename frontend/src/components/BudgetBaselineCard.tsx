import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { Loader2, TrendingUp, TrendingDown, DollarSign, Calendar } from 'lucide-react'
import { api } from '@/lib/api'

interface BudgetBaseline {
  monthlyIncome: number
  totalMonthlyExpenses: number
  monthlyExpensesByCategory: Record<string, number>
  paycheckCadence: string
  categoryConfidenceScores: Record<string, number>
}

const PAYCHECK_CADENCE_LABELS = {
  WEEKLY: 'Weekly',
  BIWEEKLY: 'Bi-weekly',
  SEMI_MONTHLY: 'Semi-monthly',
  MONTHLY: 'Monthly',
  IRREGULAR: 'Irregular'
}

export default function BudgetBaselineCard() {
  const [expanded, setExpanded] = useState(false)

  const baselineQuery = useQuery({
    queryKey: ['budget', 'baseline'],
    queryFn: async (): Promise<BudgetBaseline> => {
      const response = await api.get('/budget/baseline')
      return response.data
    },
    retry: false,
  })

  const formatCurrency = (amount: number) => {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD',
    }).format(amount)
  }

  const getConfidenceColor = (score: number) => {
    if (score >= 0.8) return 'text-green-600'
    if (score >= 0.6) return 'text-yellow-600'
    return 'text-red-600'
  }

  const getConfidenceLabel = (score: number) => {
    if (score >= 0.8) return 'High'
    if (score >= 0.6) return 'Medium'
    return 'Low'
  }

  if (baselineQuery.isLoading) {
    return (
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <DollarSign className="h-5 w-5" />
            Budget Baseline
          </CardTitle>
          <CardDescription>
            AI-powered analysis of your spending patterns and income
          </CardDescription>
        </CardHeader>
        <CardContent>
          <div className="flex items-center gap-2 text-[var(--color-text-secondary)]">
            <Loader2 className="h-4 w-4 animate-spin" />
            <span>Calculating baseline...</span>
          </div>
        </CardContent>
      </Card>
    )
  }

  if (baselineQuery.error) {
    return (
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <DollarSign className="h-5 w-5" />
            Budget Baseline
          </CardTitle>
          <CardDescription>
            AI-powered analysis of your spending patterns and income
          </CardDescription>
        </CardHeader>
        <CardContent>
          <div className="space-y-3">
            <p className="text-sm text-[var(--color-error)]">
              Unable to calculate baseline. Please ensure you have transaction data from the last 3 months.
            </p>
            <Button variant="outline" onClick={() => baselineQuery.refetch()}>
              Retry
            </Button>
          </div>
        </CardContent>
      </Card>
    )
  }

  const baseline = baselineQuery.data
  if (!baseline) return null

  const netIncome = baseline.monthlyIncome - baseline.totalMonthlyExpenses
  const savingsRate = baseline.monthlyIncome > 0
    ? ((netIncome / baseline.monthlyIncome) * 100)
    : 0

  const topCategories = Object.entries(baseline.monthlyExpensesByCategory)
    .sort(([, a], [, b]) => b - a)
    .slice(0, 5)

  return (
    <Card>
      <CardHeader>
        <CardTitle className="flex items-center gap-2">
          <DollarSign className="h-5 w-5" />
          Budget Baseline
        </CardTitle>
        <CardDescription>
          AI-powered analysis based on your last 3 months of spending
        </CardDescription>
      </CardHeader>
      <CardContent className="space-y-4">
        {/* Summary Stats */}
        <div className="grid grid-cols-2 gap-4">
          <div className="space-y-1">
            <p className="text-sm text-[var(--color-text-secondary)]">Monthly Income</p>
            <p className="text-lg font-semibold text-green-600 flex items-center gap-1">
              <TrendingUp className="h-4 w-4" />
              {formatCurrency(baseline.monthlyIncome)}
            </p>
          </div>
          <div className="space-y-1">
            <p className="text-sm text-[var(--color-text-secondary)]">Monthly Expenses</p>
            <p className="text-lg font-semibold text-red-600 flex items-center gap-1">
              <TrendingDown className="h-4 w-4" />
              {formatCurrency(baseline.totalMonthlyExpenses)}
            </p>
          </div>
        </div>

        {/* Net Income & Savings Rate */}
        <div className="rounded-lg border border-[rgba(255,255,255,0.08)] bg-[rgba(255,255,255,0.02)] p-3">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm text-[var(--color-text-secondary)]">Net Monthly</p>
              <p className={`text-lg font-semibold ${netIncome >= 0 ? 'text-green-600' : 'text-red-600'}`}>
                {formatCurrency(netIncome)}
              </p>
            </div>
            <div className="text-right">
              <p className="text-sm text-[var(--color-text-secondary)]">Savings Rate</p>
              <p className={`text-lg font-semibold ${savingsRate >= 20 ? 'text-green-600' : savingsRate >= 10 ? 'text-yellow-600' : 'text-red-600'}`}>
                {savingsRate.toFixed(1)}%
              </p>
            </div>
          </div>
        </div>

        {/* Paycheck Cadence */}
        <div className="flex items-center gap-2">
          <Calendar className="h-4 w-4 text-[var(--color-text-secondary)]" />
          <span className="text-sm text-[var(--color-text-secondary)]">Paycheck Frequency:</span>
          <Badge variant="outline">
            {PAYCHECK_CADENCE_LABELS[baseline.paycheckCadence as keyof typeof PAYCHECK_CADENCE_LABELS] || baseline.paycheckCadence}
          </Badge>
        </div>

        {/* Top Categories */}
        <div className="space-y-3">
          <div className="flex items-center justify-between">
            <h4 className="text-sm font-medium">Top Spending Categories</h4>
            <Button
              variant="ghost"
              size="sm"
              onClick={() => setExpanded(!expanded)}
            >
              {expanded ? 'Show less' : 'Show more'}
            </Button>
          </div>

          <div className="space-y-2">
            {topCategories.slice(0, expanded ? topCategories.length : 3).map(([category, amount]) => {
              const confidence = baseline.categoryConfidenceScores[category] || 0
              const percentage = baseline.totalMonthlyExpenses > 0
                ? ((amount / baseline.totalMonthlyExpenses) * 100)
                : 0

              return (
                <div key={category} className="flex items-center justify-between text-sm">
                  <div className="flex items-center gap-2">
                    <span>{category}</span>
                    <Badge
                      variant="outline"
                      className={`text-xs ${getConfidenceColor(confidence)}`}
                    >
                      {getConfidenceLabel(confidence)}
                    </Badge>
                  </div>
                  <div className="text-right">
                    <div className="font-medium">{formatCurrency(amount)}</div>
                    <div className="text-xs text-[var(--color-text-secondary)]">
                      {percentage.toFixed(1)}%
                    </div>
                  </div>
                </div>
              )
            })}
          </div>
        </div>

        {expanded && (
          <div className="space-y-2 pt-2 border-t border-[rgba(255,255,255,0.08)]">
            <h5 className="text-sm font-medium">Confidence Scores</h5>
            <p className="text-xs text-[var(--color-text-secondary)]">
              Confidence indicates how consistent spending is in each category based on historical patterns.
            </p>
          </div>
        )}
      </CardContent>
    </Card>
  )
}