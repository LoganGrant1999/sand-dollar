import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { Skeleton } from '@/components/ui/skeleton'
import { Loader2, TrendingUp, TrendingDown, DollarSign, Calendar, ChevronDown, ChevronUp } from 'lucide-react'
import { api } from '@/lib/api'
import { cn } from '@/lib/utils'
import type { BudgetOverviewResponse } from '@/types'

const CONFIDENCE_COLORS = {
  High: 'bg-green-100 text-green-800 dark:bg-green-900/20 dark:text-green-400',
  Medium: 'bg-yellow-100 text-yellow-800 dark:bg-yellow-900/20 dark:text-yellow-400',
  Low: 'bg-red-100 text-red-800 dark:bg-red-900/20 dark:text-red-400'
} as const

interface BudgetOverviewCardProps {
  onAction?: () => void
  actionLabel?: string
  className?: string
}

export default function BudgetOverviewCard({
  onAction,
  actionLabel = 'Customize Budget',
  className,
}: BudgetOverviewCardProps) {
  const [expanded, setExpanded] = useState(false)

  const overviewQuery = useQuery({
    queryKey: ['budget', 'overview'],
    queryFn: async (): Promise<BudgetOverviewResponse> => {
      const response = await api.get('/budget/overview')
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

  const formatPercentage = (rate: number) => {
    return `${(rate * 100).toFixed(1)}%`
  }

  const getComparisonIcon = (mtd: number, typical: number) => {
    if (mtd > typical) return <TrendingUp className="h-4 w-4 text-red-500" />
    if (mtd < typical) return <TrendingDown className="h-4 w-4 text-green-500" />
    return <div className="h-4 w-4" />
  }

  const getComparisonColor = (mtd: number, typical: number, isIncome = false) => {
    if (mtd > typical) return isIncome ? 'text-green-600' : 'text-red-600'
    if (mtd < typical) return isIncome ? 'text-red-600' : 'text-green-600'
    return 'text-gray-600'
  }

  if (overviewQuery.isLoading) {
    return (
      <Card className={className}>
        <CardHeader>
          <CardTitle>This Month vs. Typical</CardTitle>
          <CardDescription>Loading your budget overview...</CardDescription>
        </CardHeader>
        <CardContent className="space-y-6">
          <div className="grid gap-4 md:grid-cols-4">
            {[1, 2, 3, 4].map((item) => (
              <div key={item} className="space-y-2">
                <Skeleton className="h-4 w-20" />
                <Skeleton className="h-8 w-full" />
                <Skeleton className="h-4 w-16" />
              </div>
            ))}
          </div>
          <Skeleton className="h-10 w-32" />
        </CardContent>
      </Card>
    )
  }

  if (overviewQuery.error) {
    return (
      <Card className={className}>
        <CardHeader>
          <CardTitle>This Month vs. Typical</CardTitle>
          <CardDescription>Failed to load budget overview</CardDescription>
        </CardHeader>
        <CardContent>
          <div className="flex items-center gap-2 text-red-600">
            <Loader2 className="h-4 w-4" />
            <span>Error loading data. Please try again.</span>
          </div>
          <Button
            variant="outline"
            className="mt-4"
            onClick={() => overviewQuery.refetch()}
          >
            Retry
          </Button>
        </CardContent>
      </Card>
    )
  }

  const data = overviewQuery.data!

  return (
    <Card className={className}>
      <CardHeader className="flex flex-col gap-2 md:flex-row md:items-center md:justify-between">
        <div>
          <CardTitle className="flex items-center gap-2">
            <Calendar className="h-5 w-5" />
            This Month vs. Typical
          </CardTitle>
          <CardDescription>
            {data.monthIso} month-to-date compared to your 3-month baseline
          </CardDescription>
        </div>
        {onAction && (
          <Button onClick={onAction}>
            {actionLabel}
          </Button>
        )}
      </CardHeader>

      <CardContent className="space-y-6">
        {/* Main Financial Overview */}
        <div className="grid gap-4 md:grid-cols-4">
          {/* Income */}
          <div className="space-y-2">
            <div className="flex items-center gap-2">
              <DollarSign className="h-4 w-4 text-green-600" />
              <span className="text-sm font-medium">Income</span>
            </div>
            <div className="space-y-1">
              <div className="text-2xl font-bold">{formatCurrency(data.incomeMTD)}</div>
              <div className="flex items-center gap-1 text-sm text-gray-600">
                {getComparisonIcon(data.incomeMTD, data.incomeTypical)}
                <span className={getComparisonColor(data.incomeMTD, data.incomeTypical, true)}>
                  vs. {formatCurrency(data.incomeTypical)}
                </span>
              </div>
            </div>
          </div>

          {/* Expenses */}
          <div className="space-y-2">
            <div className="flex items-center gap-2">
              <TrendingDown className="h-4 w-4 text-red-600" />
              <span className="text-sm font-medium">Expenses</span>
            </div>
            <div className="space-y-1">
              <div className="text-2xl font-bold">{formatCurrency(data.expensesMTD)}</div>
              <div className="flex items-center gap-1 text-sm text-gray-600">
                {getComparisonIcon(data.expensesMTD, data.expensesTypical)}
                <span className={getComparisonColor(data.expensesMTD, data.expensesTypical)}>
                  vs. {formatCurrency(data.expensesTypical)}
                </span>
              </div>
            </div>
          </div>

          {/* Net */}
          <div className="space-y-2">
            <div className="flex items-center gap-2">
              <TrendingUp className="h-4 w-4 text-blue-600" />
              <span className="text-sm font-medium">Net</span>
            </div>
            <div className="space-y-1">
              <div className="text-2xl font-bold">{formatCurrency(data.netMTD)}</div>
              <div className="flex items-center gap-1 text-sm text-gray-600">
                {getComparisonIcon(data.netMTD, data.netTypical)}
                <span className={getComparisonColor(data.netMTD, data.netTypical, true)}>
                  vs. {formatCurrency(data.netTypical)}
                </span>
              </div>
            </div>
          </div>

          {/* Savings Rate */}
          <div className="space-y-2">
            <div className="flex items-center gap-2">
              <TrendingUp className="h-4 w-4 text-purple-600" />
              <span className="text-sm font-medium">Savings Rate</span>
            </div>
            <div className="space-y-1">
              <div className="text-2xl font-bold">{formatPercentage(data.savingsRateMTD)}</div>
              <div className="flex items-center gap-1 text-sm text-gray-600">
                {getComparisonIcon(data.savingsRateMTD, data.savingsRateTypical)}
                <span className={getComparisonColor(data.savingsRateMTD, data.savingsRateTypical, true)}>
                  vs. {formatPercentage(data.savingsRateTypical)}
                </span>
              </div>
            </div>
          </div>
        </div>

        {/* Category Breakdown */}
        {data.categoriesMTD && data.categoriesMTD.length > 0 && (
          <div className="space-y-4">
            <Button
              variant="ghost"
              onClick={() => setExpanded(!expanded)}
              className="flex items-center gap-2 w-full justify-between"
            >
              <span className="font-medium">Category Breakdown</span>
              {expanded ? <ChevronUp className="h-4 w-4" /> : <ChevronDown className="h-4 w-4" />}
            </Button>

            {expanded && (
              <div className="space-y-3">
                {data.categoriesMTD.slice(0, 10).map((category) => (
                  <div key={category.key} className="flex items-center justify-between p-3 bg-gray-50 dark:bg-gray-800/50 rounded-lg">
                    <div className="flex items-center gap-3">
                      <span className="font-medium">{category.key}</span>
                      <Badge
                        variant="secondary"
                        className={cn(
                          'text-xs',
                          CONFIDENCE_COLORS[category.confidence as keyof typeof CONFIDENCE_COLORS] || CONFIDENCE_COLORS.Low
                        )}
                      >
                        {category.confidence}
                      </Badge>
                    </div>
                    <div className="text-right space-y-1">
                      <div className="font-bold">{formatCurrency(category.amountMTD)}</div>
                      <div className="flex items-center gap-1 text-sm text-gray-600">
                        {getComparisonIcon(category.amountMTD, category.amountTypical)}
                        <span className={getComparisonColor(category.amountMTD, category.amountTypical)}>
                          vs. {formatCurrency(category.amountTypical)}
                        </span>
                      </div>
                    </div>
                  </div>
                ))}
                {data.categoriesMTD.length > 10 && (
                  <div className="text-center text-sm text-gray-600">
                    And {data.categoriesMTD.length - 10} more categories...
                  </div>
                )}
              </div>
            )}
          </div>
        )}
      </CardContent>
    </Card>
  )
}