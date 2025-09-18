import { AlertCircle, DollarSign, TrendingDown, TrendingUp } from 'lucide-react'
import type { FinancialSnapshotResponse } from '@/types'
import ProgressBar from './ui/ProgressBar'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from './ui/card'
import { Button } from './ui/button'
import { Skeleton } from './ui/skeleton'
import { cn } from '@/lib/utils'

interface SnapshotCardProps {
  snapshot?: FinancialSnapshotResponse | null
  isLoading?: boolean
  error?: unknown
  onRetry?: () => void
  onAction?: () => void
  actionLabel?: string
  className?: string
}

export default function SnapshotCard({
  snapshot,
  isLoading,
  error,
  onRetry,
  onAction,
  actionLabel = 'Customize Budget',
  className,
}: SnapshotCardProps) {
  if (isLoading) {
    return (
      <Card className={className}>
        <CardHeader>
          <CardTitle>Current Month Snapshot</CardTitle>
          <CardDescription>Loading your financial overview...</CardDescription>
        </CardHeader>
        <CardContent className="space-y-6">
          <div className="grid gap-4 md:grid-cols-3">
            {[1, 2, 3].map((item) => (
              <div key={item} className="space-y-2">
                <Skeleton className="h-4 w-20" />
                <Skeleton className="h-8 w-full" />
              </div>
            ))}
          </div>
          <div className="space-y-4">
            {[1, 2, 3, 4].map((item) => (
              <div key={item} className="space-y-2">
                <Skeleton className="h-4 w-24" />
                <Skeleton className="h-3 w-full" />
                <div className="flex justify-between">
                  <Skeleton className="h-3 w-16" />
                  <Skeleton className="h-3 w-16" />
                </div>
              </div>
            ))}
          </div>
        </CardContent>
      </Card>
    )
  }

  if (error) {
    return (
      <Card className={className}>
        <CardHeader>
          <CardTitle className="flex items-center gap-2 text-[var(--color-error)]">
            <AlertCircle className="h-5 w-5" />
            Error Loading Snapshot
          </CardTitle>
          <CardDescription>
            We couldn’t load your financial snapshot. Please try again.
          </CardDescription>
        </CardHeader>
        <CardContent>
          {onRetry && (
            <Button variant="outline" onClick={onRetry}>
              Retry
            </Button>
          )}
        </CardContent>
      </Card>
    )
  }

  if (!snapshot) {
    return (
      <Card className={className}>
        <CardHeader>
          <CardTitle>No Data Available</CardTitle>
          <CardDescription>
            Connect your accounts to see a monthly spending snapshot.
          </CardDescription>
        </CardHeader>
        {onAction && (
          <CardContent>
            <Button onClick={onAction}>{actionLabel}</Button>
          </CardContent>
        )}
      </Card>
    )
  }

  const { month, income, actualsByCategory = [], totals } = snapshot
  const netCashFlowIsPositive = totals?.netCashFlow >= 0

  return (
    <Card className={className}>
      <CardHeader>
        <CardTitle className="flex items-center gap-2">
          <DollarSign className="h-5 w-5 text-[var(--color-accent-teal)]" />
          {month} Snapshot
        </CardTitle>
        <CardDescription>Your current month financial overview</CardDescription>
      </CardHeader>
      <CardContent className="space-y-6">
        <div className="grid gap-4 md:grid-cols-3">
          <SummaryTile
            label="Income"
            value={income}
            tone="accent"
          />
          <SummaryTile
            label="Expenses"
            value={totals?.expenses || 0}
            tone="error"
          />
          <SummaryTile
            label={netCashFlowIsPositive ? 'Surplus' : 'Deficit'}
            value={Math.abs(totals?.netCashFlow || 0)}
            tone={netCashFlowIsPositive ? 'success' : 'error'}
            icon={netCashFlowIsPositive ? TrendingUp : TrendingDown}
          />
        </div>

        <div className="space-y-4">
          <h3 className="font-semibold text-[var(--color-text-primary)]">
            Spending by Category
          </h3>

          {actualsByCategory.length === 0 ? (
            <div className="rounded-[1rem] border border-[rgba(255,255,255,0.08)] bg-[rgba(255,255,255,0.02)] p-6 text-center text-[var(--color-text-secondary)]">
              No spending data available for this month.
            </div>
          ) : (
            <div className="space-y-4">
              {actualsByCategory.map((category) => (
                <ProgressBar
                  key={category.category}
                  value={category.actual}
                  max={category.target ?? category.actual}
                  label={category.category}
                  variant={category.target && category.actual > category.target ? 'danger' : 'default'}
                  formatValue={(val) => `$${val.toLocaleString()}`}
                  showPercentage={Boolean(category.target)}
                />
              ))}
            </div>
          )}
        </div>

        {onAction && (
          <div className="rounded-[1rem] border border-[var(--color-accent-blue)]/40 bg-[var(--color-accent-blue)]/10 p-6">
            <div className="flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
              <div>
                <h4 className="text-lg font-semibold text-[var(--color-text-primary)]">Ready to let AI build your budget?</h4>
                <p className="text-sm text-[var(--color-text-secondary)]">
                  We’ll analyze your spending patterns and create personalized targets you can fine-tune.
                </p>
              </div>
              <Button onClick={onAction} className="md:w-auto">
                {actionLabel}
              </Button>
            </div>
          </div>
        )}
      </CardContent>
    </Card>
  )
}

interface SummaryTileProps {
  label: string
  value: number
  tone: 'accent' | 'success' | 'error'
  icon?: typeof TrendingUp
}

function SummaryTile({ label, value, tone, icon: Icon }: SummaryTileProps) {
  const toneClasses = {
    accent: 'bg-[var(--color-accent-teal)]/15 text-[var(--color-accent-teal)]',
    success: 'bg-[var(--color-success)]/15 text-[var(--color-success)]',
    error: 'bg-[var(--color-error)]/15 text-[var(--color-error)]',
  }[tone]

  return (
    <div className={cn('rounded-[1rem] border border-[rgba(255,255,255,0.08)] p-4 text-center', toneClasses)}>
      <div className="flex items-center justify-center gap-2 text-2xl font-bold">
        {Icon ? <Icon className="h-5 w-5" /> : null}
        ${value.toLocaleString()}
      </div>
      <div className="text-sm text-[var(--color-text-secondary)]">{label}</div>
    </div>
  )
}
