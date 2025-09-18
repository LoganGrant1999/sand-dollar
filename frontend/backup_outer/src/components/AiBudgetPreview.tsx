import { useMemo, useState } from 'react'
import type { CategoryActual, CategoryTarget, GenerateBudgetResponse } from '@/types'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from './ui/card'
import { Button } from './ui/button'
import { Input } from './ui/input'
import { Badge } from './ui/badge'
import ProgressBar from './ui/ProgressBar'
import { Bot, Edit3, Target, Loader2 } from 'lucide-react'

interface AiBudgetPreviewProps {
  budgetData: GenerateBudgetResponse
  month: string
  onBack: () => void
  onAccept: (targets: CategoryTarget[]) => void
  accepting?: boolean
  actualsByCategory?: CategoryActual[]
}

export default function AiBudgetPreview({
  budgetData,
  month,
  onBack,
  onAccept,
  accepting = false,
  actualsByCategory = [],
}: AiBudgetPreviewProps) {
  const [editingTargets, setEditingTargets] = useState<Record<string, number>>({})
  const [isEditing, setIsEditing] = useState<Record<string, boolean>>({})

  const actualsMap = useMemo(
    () => new Map(actualsByCategory.map((item) => [item.category, item.actual])),
    [actualsByCategory]
  )

  const getFinalTargets = (): CategoryTarget[] => {
    return budgetData.targetsByCategory.map((target) => ({
      ...target,
      target: editingTargets[target.category] ?? target.target,
    }))
  }

  const totalTargets = getFinalTargets().reduce((sum, target) => sum + target.target, 0)
  const totalActuals = actualsByCategory.reduce((sum, item) => sum + item.actual, 0)

  const toggleEdit = (category: string) => {
    setIsEditing((prev) => ({
      ...prev,
      [category]: !prev[category],
    }))
    setEditingTargets((prev) => ({
      ...prev,
      [category]: prev[category] ?? budgetData.targetsByCategory.find((t) => t.category === category)?.target ?? 0,
    }))
  }

  const handleTargetChange = (category: string, value: number) => {
    if (Number.isNaN(value) || value < 0) return
    setEditingTargets((prev) => ({
      ...prev,
      [category]: value,
    }))
  }

  const handleAccept = () => {
    onAccept(getFinalTargets())
  }

  const categoryCards = useMemo(() => {
    return budgetData.targetsByCategory.map((target) => {
      const actual = actualsMap.get(target.category) ?? 0
      const finalTarget = editingTargets[target.category] ?? target.target
      const overBudget = actual > finalTarget
      const editing = isEditing[target.category]

    const baseline = actualsMap.get(target.category) ?? 0
    const changePercent = baseline > 0 ? Math.abs(finalTarget - baseline) / baseline : 0
    const highlight = changePercent > 0.1

    return (
      <div
        key={target.category}
        className={`rounded-[1rem] border p-4 transition-colors ${
          highlight
            ? 'border-[var(--color-accent-blue)]/60 bg-[var(--color-accent-blue)]/10'
            : 'border-[rgba(255,255,255,0.08)] bg-[rgba(255,255,255,0.02)]'
        }`}
      >
          <div className="flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
            <div>
              <p className="text-sm font-semibold text-[var(--color-text-primary)]">{target.category}</p>
              <p className="text-xs text-[var(--color-text-secondary)]">{target.reason}</p>
            </div>
            <Button
              type="button"
              variant="ghost"
              size="sm"
              className="self-start md:self-auto"
              onClick={() => toggleEdit(target.category)}
            >
              <Edit3 className="mr-2 h-4 w-4" />
              {editing ? 'Done' : 'Adjust'}
            </Button>
          </div>

          <div className="mt-4 space-y-3">
            <ProgressBar
              value={actual}
              max={finalTarget}
              label="Actual vs Target"
              variant={overBudget ? 'danger' : 'default'}
            />
            <div className="flex flex-wrap items-center justify-between gap-2 text-sm text-[var(--color-text-secondary)]">
              <span>Actual: {formatAmount(actual)}</span>
              <span>Target: {formatAmount(finalTarget)}</span>
              <span className={overBudget ? 'text-[var(--color-error)]' : ''}>
                Difference: {formatAmount(finalTarget - actual)}
              </span>
            </div>
            {highlight && (
              <p className="text-xs text-[var(--color-accent-blue)]">
                Target adjusted {Math.round(changePercent * 100)}% from recent average.
              </p>
            )}
          </div>

          {editing && (
            <div className="mt-3 flex flex-col gap-2 md:flex-row md:items-center">
              <label htmlFor={`target-${target.category}`} className="text-sm text-[var(--color-text-secondary)]">
                New target
              </label>
              <Input
                id={`target-${target.category}`}
                type="number"
                min={0}
                step={50}
                value={editingTargets[target.category] ?? target.target}
                onChange={(event) => handleTargetChange(target.category, Number(event.target.value))}
                className="md:w-48"
              />
            </div>
          )}
        </div>
      )
    })
  }, [actualsMap, budgetData.targetsByCategory, editingTargets, isEditing])

  return (
    <Card>
      <CardHeader>
        <div className="flex flex-col gap-2 md:flex-row md:items-center md:justify-between">
          <div>
            <CardTitle className="flex items-center gap-2">
              <Bot className="h-5 w-5 text-[var(--color-accent-teal)]" />
              AI Budget Recommendation
            </CardTitle>
            <CardDescription>
              Review and fine-tune your AI-generated targets for {month}
            </CardDescription>
          </div>
          <Badge variant="secondary" className="bg-[var(--color-accent-blue)]/15 text-[var(--color-accent-blue)]">
            Savings rate {(budgetData.summary.savingsRate * 100).toFixed(0)}%
          </Badge>
        </div>
      </CardHeader>
      <CardContent className="space-y-8">
        <div className="grid gap-4 md:grid-cols-3">
          <SummaryTile label="Total Targets" value={totalTargets} tone="accent" />
          <SummaryTile label="Current Actuals" value={totalActuals} tone="neutral" />
          <SummaryTile label="Categories" value={budgetData.targetsByCategory.length} tone="secondary" suffix="categories" />
        </div>

        {budgetData.summary.notes.length > 0 && (
          <div className="rounded-[1rem] border border-[var(--color-warning)]/50 bg-[var(--color-warning)]/15 p-4">
            <h3 className="mb-2 flex items-center gap-2 text-sm font-semibold text-[var(--color-warning)]">
              <Target className="h-4 w-4" />
              AI Insights
            </h3>
            <ul className="space-y-1 text-sm text-[var(--color-text-secondary)]">
              {budgetData.summary.notes.map((note, index) => (
                <li key={index} className="leading-relaxed">{note}</li>
              ))}
            </ul>
          </div>
        )}

        <div className="space-y-4">
          <div className="flex items-center justify-between">
            <h3 className="text-lg font-semibold text-[var(--color-text-primary)]">Category Targets</h3>
            <Badge variant="outline">Adjust any target inline</Badge>
          </div>

          <div className="grid gap-4">{categoryCards}</div>
        </div>

        <div className="flex flex-col gap-3 md:flex-row md:justify-between">
          <Button variant="outline" onClick={onBack} className="md:w-auto">
            Back to goals
          </Button>
          <Button onClick={handleAccept} disabled={accepting} className="md:w-auto">
            {accepting ? (
              <span className="flex items-center gap-2">
                <Loader2 className="h-4 w-4 animate-spin" />
                Saving...
              </span>
            ) : (
              'Accept AI Budget'
            )}
          </Button>
        </div>
      </CardContent>
    </Card>
  )
}

interface SummaryTileProps {
  label: string
  value: number
  tone: 'accent' | 'neutral' | 'secondary'
  suffix?: string
}

function SummaryTile({ label, value, tone, suffix }: SummaryTileProps) {
  const toneStyles = {
    accent: 'bg-[var(--color-accent-teal)]/15 text-[var(--color-accent-teal)]',
    neutral: 'bg-[rgba(255,255,255,0.06)] text-[var(--color-text-primary)]',
    secondary: 'bg-[var(--color-accent-blue)]/15 text-[var(--color-accent-blue)]',
  }[tone]

  return (
    <div className={`rounded-[1rem] border border-[rgba(255,255,255,0.08)] p-4 text-center ${toneStyles}`}>
      <div className="text-2xl font-bold leading-tight">
        {suffix ? `${value} ${suffix}` : formatAmount(value)}
      </div>
      <div className="text-sm text-[var(--color-text-secondary)]">{label}</div>
    </div>
  )
}

function formatAmount(value: number): string {
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'USD',
  }).format(value)
}
