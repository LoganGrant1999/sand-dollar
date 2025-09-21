import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Skeleton } from '@/components/ui/skeleton'
import { Loader2, Calendar, ChevronDown, ChevronUp } from 'lucide-react'
import { api } from '@/lib/api'
import type { BudgetOverviewResponse } from '@/types'
import ProgressPill from './ProgressPill'


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

  const formatCurrencyFromCents = (cents: number) => {
    return formatCurrency(cents / 100)
  }

  const dollarsToCents = (dollars: number) => Math.round(dollars * 100)

  const getMonthName = (monthIso: string) => {
    try {
      const [year, month] = monthIso.split('-')
      const date = new Date(parseInt(year), parseInt(month) - 1)
      return date.toLocaleString('en-US', { month: 'long' })
    } catch {
      return 'This Month'
    }
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
  const monthName = getMonthName(data.monthIso)

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
        <div className="space-y-6">
          {/* Income */}
          <div>
            <h3 className="text-lg font-semibold mb-4">Income</h3>
            <ProgressPill
              current={dollarsToCents(data.incomeMTD)}
              typical={dollarsToCents(data.incomeTypical)}
              labelCurrent={`${monthName} to Date`}
              labelTypical="Monthly Avg (past 3 months)"
              format={formatCurrencyFromCents}
              color={data.incomeMTD > 0 ? "green" : "blue"}
            />
          </div>

          {/* Expenses */}
          <div>
            <h3 className="text-lg font-semibold mb-4">Expenses</h3>
            <ProgressPill
              current={dollarsToCents(data.expensesMTD)}
              typical={dollarsToCents(data.expensesTypical)}
              labelCurrent={`${monthName} to Date`}
              labelTypical="Monthly Avg (past 3 months)"
              format={formatCurrencyFromCents}
              color="red"
            />
          </div>

          {/* Net Cash Flow */}
          <div>
            <h3 className="text-lg font-semibold mb-4">Net Cash Flow</h3>
            <div className="bg-gray-50 dark:bg-gray-800/50 rounded-lg p-4">
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <div className="text-sm text-gray-600 dark:text-gray-400">{monthName} to Date</div>
                  <div className={`text-2xl font-bold ${data.netMTD >= 0 ? 'text-green-600' : 'text-red-600'}`}>
                    {formatCurrency(data.netMTD)}
                  </div>
                </div>
                <div>
                  <div className="text-sm text-gray-600 dark:text-gray-400">Monthly Avg (past 3 months)</div>
                  <div className={`text-2xl font-bold ${data.netTypical >= 0 ? 'text-green-600' : 'text-red-600'}`}>
                    {formatCurrency(data.netTypical)}
                  </div>
                </div>
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
              <div className="space-y-4">
                {data.categoriesMTD.map((category) => (
                  <div key={category.key} className="p-4 bg-gray-50 dark:bg-gray-800/50 rounded-lg">
                    <div className="mb-3">
                      <span className="font-medium text-gray-900 dark:text-gray-100">{category.key}</span>
                    </div>
                    <ProgressPill
                      current={dollarsToCents(category.amountMTD)}
                      typical={dollarsToCents(category.amountTypical)}
                      labelCurrent={`${monthName} to Date`}
                      labelTypical="Monthly Avg (past 3 months)"
                      format={formatCurrencyFromCents}
                      color="blue"
                    />
                  </div>
                ))}
              </div>
            )}
          </div>
        )}
      </CardContent>
    </Card>
  )
}