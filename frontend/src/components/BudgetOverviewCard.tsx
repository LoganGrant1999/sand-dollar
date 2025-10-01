import { useQuery } from '@tanstack/react-query'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Skeleton } from '@/components/ui/skeleton'
import { Loader2, Calendar } from 'lucide-react'
import { api } from '@/lib/api'
import type { BudgetOverviewResponse } from '@/types'
import ProgressPill from './ProgressPill'
import { formatCurrency, toNumber } from '@/utils/money'


interface BudgetOverviewCardProps {
  onAction?: () => void
  actionLabel?: string
  className?: string
  onSync?: () => void
  isSyncing?: boolean
  hasPlaidItem?: boolean
}

export default function BudgetOverviewCard({
  onAction,
  actionLabel = 'Customize Budget',
  className,
  onSync,
  isSyncing = false,
  hasPlaidItem = false,
}: BudgetOverviewCardProps) {

  const overviewQuery = useQuery({
    queryKey: ['budget', 'overview'],
    queryFn: async (): Promise<BudgetOverviewResponse> => {
      const response = await api.get('/budget/overview')
      return response.data
    },
    retry: false,
  })


  const getMonthName = (monthIso: string) => {
    try {
      const [year, month] = monthIso.split('-')
      const date = new Date(parseInt(year), parseInt(month) - 1)
      return date.toLocaleString('en-US', { month: 'long' })
    } catch {
      return 'This Month'
    }
  }

  const getCurrentDate = () => {
    const today = new Date()
    return today.toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'long',
      day: 'numeric'
    })
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
            Budget Snapshot
          </CardTitle>
          <CardDescription>
            {getCurrentDate()} month-to-date compared to your 3-month baseline
          </CardDescription>
        </div>
        <div className="flex gap-2">
          {onSync && (
            <Button variant="outline" onClick={onSync} disabled={isSyncing}>
              {isSyncing ? (
                <span className="flex items-center gap-2">
                  <Loader2 className="h-4 w-4 animate-spin" />
                  {hasPlaidItem ? 'Syncing' : 'Refreshing'}
                </span>
              ) : (
                hasPlaidItem ? 'Sync' : 'Refresh'
              )}
            </Button>
          )}
          {onAction && (
            <Button onClick={onAction}>
              {actionLabel}
            </Button>
          )}
        </div>
      </CardHeader>

      <CardContent className="space-y-6">
        {/* Main Financial Overview */}
        <div className="space-y-6">
          {/* Income */}
          <div>
            <h3 className="text-lg font-semibold mb-4">Income</h3>
            <ProgressPill
              label={`${monthName} to Date`}
              current={toNumber(data.incomeMTD)}
              target={toNumber(data.incomeTypical)}
              format="currency"
              color={data.incomeMTD > 0 ? "green" : "blue"}
              size="md"
              className="w-full"
            />
          </div>

          {/* Expenses */}
          <div>
            <h3 className="text-lg font-semibold mb-4">Expenses</h3>
            <ProgressPill
              label={`${monthName} to Date`}
              current={toNumber(data.expensesMTD)}
              target={toNumber(data.expensesTypical)}
              format="currency"
              color="red"
              size="md"
              className="w-full"
            />
          </div>

          {/* Net Cash Flow */}
          <div>
            <h3 className="text-lg font-semibold mb-4">Net Cash Flow</h3>
            <div className="bg-gray-100 dark:bg-gray-700 rounded-lg p-4">
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <div className="text-sm text-gray-600 dark:text-gray-400">{monthName} to Date</div>
                  <div className={`text-2xl font-bold ${data.netMTD >= 0 ? 'text-green-600' : 'text-red-600'}`}>
                    {formatCurrency(toNumber(data.netMTD))}
                  </div>
                </div>
                <div>
                  <div className="text-sm text-gray-600 dark:text-gray-400">Monthly Avg (past 3 months)</div>
                  <div className={`text-2xl font-bold ${data.netTypical >= 0 ? 'text-green-600' : 'text-red-600'}`}>
                    {formatCurrency(toNumber(data.netTypical))}
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        {/* Category Breakdown */}
        {data.categoriesMTD && data.categoriesMTD.length > 0 && (
          <div className="space-y-4">
            <h3 className="text-2xl font-semibold border-b border-gray-200 dark:border-gray-700 pb-2">Category Breakdown</h3>
            <div className="space-y-4">
              {data.categoriesMTD.map((category, index) => (
                <div key={category.key}>
                  <h4 className="text-lg font-semibold mb-3">{category.key}</h4>
                  <ProgressPill
                    label={`${monthName} to Date`}
                    current={toNumber(category.amountMTD)}
                    target={toNumber(category.amountTypical)}
                    format="currency"
                    color="auto"
                    size="md"
                    className="w-full"
                  />
                </div>
              ))}
            </div>
          </div>
        )}
      </CardContent>
    </Card>
  )
}