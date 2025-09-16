import { useCallback, useEffect, useState } from 'react'
import type { GoalFormData } from '@/types'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from './ui/card'
import { Button } from './ui/button'
import { Input } from './ui/input'
import { Label } from './ui/label'
import { Textarea } from './ui/textarea'
import { Badge } from './ui/badge'
import { Plus, Target, Settings, X, Info } from 'lucide-react'

interface GoalsFormProps {
  onNext: (data: GoalFormData) => void
  initialData?: Partial<GoalFormData>
  onChange?: (data: GoalFormData) => void
}

const COMMON_GOALS = [
  'Build emergency fund',
  'Pay off debt',
  'Save for vacation',
  'Save for house down payment',
  'Increase retirement savings',
  'Build investment portfolio',
  'Save for car',
  'Reduce monthly expenses',
  'Create passive income',
  'Start a business fund',
]

const STYLE_OPTIONS: GoalFormData['style'][] = ['aggressive', 'balanced', 'flexible']

const STYLE_COPY: Record<GoalFormData['style'], string> = {
  aggressive: 'Cut expenses sharply to maximize savings momentum.',
  balanced: 'Blend savings progress with steady lifestyle spending.',
  flexible: 'Gentle course-correct with room for small splurges.',
}

export default function GoalsForm({ onNext, initialData, onChange }: GoalsFormProps) {
  const [formData, setFormData] = useState<GoalFormData>({
    goals: initialData?.goals ?? [],
    style: initialData?.style ?? 'balanced',
    mustKeepCategories: initialData?.mustKeepCategories ?? [],
    categoryCaps: initialData?.categoryCaps ?? {},
    notes: initialData?.notes ?? '',
  })
  const [customGoal, setCustomGoal] = useState('')
  const [mustKeepInput, setMustKeepInput] = useState('')
  const [capCategoryInput, setCapCategoryInput] = useState('')
  const [capAmountInput, setCapAmountInput] = useState('')
  const [errors, setErrors] = useState<{ goals?: string; notes?: string; caps?: string }>({})

  useEffect(() => {
    onChange?.(formData)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  const emit = useCallback(
    (updater: (prev: GoalFormData) => GoalFormData) => {
      setFormData((prev) => {
        const next = updater(prev)
        onChange?.(next)
        return next
      })
    },
    [onChange]
  )

  const goalsValid = formData.goals.length > 0
  const notesValid = !formData.notes || formData.notes.length <= 500
  const capsValid = Object.values(formData.categoryCaps || {}).every((value) => value > 0)
  const isValid = goalsValid && notesValid && capsValid

  const addGoal = (goal: string) => {
    const trimmed = goal.trim()
    if (!trimmed || formData.goals.includes(trimmed)) return
    emit((prev) => ({ ...prev, goals: [...prev.goals, trimmed] }))
  }

  const removeGoal = (index: number) => {
    emit((prev) => ({
      ...prev,
      goals: prev.goals.filter((_, i) => i !== index),
    }))
  }

  const addMustKeepCategory = () => {
    const trimmed = mustKeepInput.trim()
    if (!trimmed) return
    emit((prev) => ({
      ...prev,
      mustKeepCategories: [...(prev.mustKeepCategories ?? []), trimmed],
    }))
    setMustKeepInput('')
  }

  const removeMustKeepCategory = (index: number) => {
    emit((prev) => {
      const current = prev.mustKeepCategories ?? []
      const updated = current.filter((_, i) => i !== index)
      return { ...prev, mustKeepCategories: updated }
    })
  }

  const addCategoryCap = () => {
    const category = capCategoryInput.trim()
    const amount = parseFloat(capAmountInput)
    if (!category || Number.isNaN(amount) || amount <= 0) return
    emit((prev) => ({
      ...prev,
      categoryCaps: {
        ...(prev.categoryCaps ?? {}),
        [category]: amount,
      },
    }))
    setCapCategoryInput('')
    setCapAmountInput('')
  }

  const removeCategoryCap = (category: string) => {
    emit((prev) => {
      const updated = { ...(prev.categoryCaps ?? {}) }
      delete updated[category]
      return { ...prev, categoryCaps: updated }
    })
  }

  const handleSubmit = () => {
    const nextErrors: typeof errors = {}
    if (!goalsValid) {
      nextErrors.goals = 'Select at least one goal to get personal recommendations.'
    }
    if (!notesValid) {
      nextErrors.notes = 'Notes must be 500 characters or less.'
    }
    if (!capsValid) {
      nextErrors.caps = 'Category caps must be positive amounts.'
    }
    setErrors(nextErrors)

    if (Object.keys(nextErrors).length > 0) return

    onNext({
      ...formData,
      mustKeepCategories: formData.mustKeepCategories?.filter(Boolean),
      categoryCaps:
        formData.categoryCaps && Object.keys(formData.categoryCaps).length > 0
          ? Object.fromEntries(
              Object.entries(formData.categoryCaps).map(([key, value]) => [key, Number(value)])
            )
          : undefined,
      notes: formData.notes?.trim() ? formData.notes.trim() : undefined,
    })
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle className="flex items-center gap-2">
          <Target className="h-5 w-5 text-[var(--color-accent-teal)]" />
          Tell us your goals
        </CardTitle>
        <CardDescription>
          Help our AI understand what you want from this month’s budget.
        </CardDescription>
      </CardHeader>
      <CardContent className="space-y-6">
        <section className="space-y-4">
          <Label>What are your financial goals? *</Label>
          <div className="grid grid-cols-2 gap-2 md:grid-cols-3">
            {COMMON_GOALS.map((goal) => (
              <Button
                key={goal}
                type="button"
                variant={formData.goals.includes(goal) ? 'default' : 'outline'}
                size="sm"
                className="h-auto justify-start py-2 text-xs"
                onClick={() => addGoal(goal)}
              >
                {goal}
              </Button>
            ))}
          </div>

          <div className="flex gap-2">
            <Input
              placeholder="Add custom goal..."
              value={customGoal}
              onChange={(event) => setCustomGoal(event.target.value)}
              onKeyDown={(event) => {
                if (event.key === 'Enter') {
                  event.preventDefault()
                  addGoal(customGoal)
                  setCustomGoal('')
                }
              }}
            />
            <Button
              type="button"
              variant="outline"
              size="icon"
              onClick={() => {
                addGoal(customGoal)
                setCustomGoal('')
              }}
            >
              <Plus className="h-4 w-4" />
            </Button>
          </div>

          {formData.goals.length > 0 && (
            <div className="flex flex-wrap gap-2">
              {formData.goals.map((goal, index) => (
                <Badge key={goal} variant="default" className="gap-1">
                  {goal}
                  <Button
                    type="button"
                    variant="ghost"
                    size="sm"
                    className="h-auto p-0 text-[var(--color-text-primary)] hover:text-[var(--color-error)]"
                    onClick={() => removeGoal(index)}
                  >
                    <X className="h-3 w-3" />
                  </Button>
                </Badge>
              ))}
            </div>
          )}

          {errors.goals && <p className="text-sm text-[var(--color-error)]">{errors.goals}</p>}
        </section>

        <section className="space-y-4">
          <Label className="flex items-center gap-2">
            What's your budgeting style? *
            <Info className="h-4 w-4 text-[var(--color-text-secondary)]" />
          </Label>
          <div className="grid gap-4 md:grid-cols-3">
            {STYLE_OPTIONS.map((option) => (
              <label key={option} htmlFor={option} className="block" title={STYLE_COPY[option]}>
                <input
                  type="radio"
                  id={option}
                  name="budget-style"
                  value={option}
                  checked={formData.style === option}
                  onChange={() => emit((prev) => ({ ...prev, style: option }))}
                  className="peer sr-only"
                />
                <div className="rounded-[1rem] border border-[rgba(255,255,255,0.08)] p-4 transition-colors peer-checked:border-[var(--color-accent-teal)] peer-checked:bg-[var(--color-accent-teal)]/15 hover:border-[var(--color-accent-teal)]/60">
                  <div className="font-semibold text-[var(--color-text-primary)]">{option.charAt(0).toUpperCase() + option.slice(1)}</div>
                  <div className="mt-1 text-sm text-[var(--color-text-secondary)]">{STYLE_COPY[option]}</div>
                </div>
              </label>
            ))}
          </div>
        </section>

        <section className="space-y-4">
          <Label className="flex items-center gap-2 text-[var(--color-text-primary)]">
            <Settings className="h-4 w-4" />
            Advanced Constraints (Optional)
          </Label>

          <div className="space-y-2">
            <Label className="text-sm text-[var(--color-text-secondary)]">Categories to keep unchanged</Label>
            <div className="flex gap-2">
              <Input
                placeholder="e.g., Rent, Childcare"
                value={mustKeepInput}
                onChange={(event) => setMustKeepInput(event.target.value)}
                onKeyDown={(event) => {
                  if (event.key === 'Enter') {
                    event.preventDefault()
                    addMustKeepCategory()
                  }
                }}
              />
              <Button type="button" variant="outline" size="icon" onClick={addMustKeepCategory}>
                <Plus className="h-4 w-4" />
              </Button>
            </div>
            {formData.mustKeepCategories && formData.mustKeepCategories.length > 0 && (
              <div className="flex flex-wrap gap-2">
                {formData.mustKeepCategories.map((category, index) => (
                  <Badge key={`${category}-${index}`} variant="secondary" className="gap-1">
                    {category}
                    <Button
                      type="button"
                      variant="ghost"
                      size="sm"
                      className="h-auto p-0"
                      onClick={() => removeMustKeepCategory(index)}
                    >
                      <X className="h-3 w-3" />
                    </Button>
                  </Badge>
                ))}
              </div>
            )}
          </div>

          <div className="space-y-2">
            <Label className="text-sm text-[var(--color-text-secondary)]">Maximum spending limits</Label>
            <div className="flex flex-col gap-2 md:flex-row">
              <Input
                placeholder="Category name"
                value={capCategoryInput}
                onChange={(event) => setCapCategoryInput(event.target.value)}
              />
              <Input
                placeholder="Max amount"
                type="number"
                min="0"
                step="0.01"
                value={capAmountInput}
                onChange={(event) => setCapAmountInput(event.target.value)}
              />
              <Button type="button" variant="outline" size="icon" onClick={addCategoryCap}>
                <Plus className="h-4 w-4" />
              </Button>
            </div>
            {formData.categoryCaps && Object.keys(formData.categoryCaps).length > 0 && (
              <div className="space-y-2">
                {Object.entries(formData.categoryCaps).map(([category, amount]) => (
                  <div
                    key={category}
                    className="flex items-center justify-between rounded-[1rem] border border-[rgba(255,255,255,0.08)] bg-[rgba(255,255,255,0.02)] p-3"
                  >
                    <span className="text-sm text-[var(--color-text-secondary)]">
                      {category}: {formatAmount(amount)}
                    </span>
                    <Button type="button" variant="ghost" size="sm" onClick={() => removeCategoryCap(category)}>
                      <X className="h-3 w-3" />
                    </Button>
                  </div>
                ))}
              </div>
            )}
            {errors.caps && <p className="text-sm text-[var(--color-error)]">{errors.caps}</p>}
          </div>
        </section>

        <section className="space-y-2">
          <Label htmlFor="notes">Additional notes (Optional)</Label>
          <Textarea
            id="notes"
            value={formData.notes ?? ''}
            onChange={(event) => emit((prev) => ({ ...prev, notes: event.target.value }))}
            placeholder="Any specific preferences or requirements for your budget..."
          />
          {errors.notes && <p className="text-sm text-[var(--color-error)]">{errors.notes}</p>}
        </section>

        <Button type="button" className="w-full" onClick={handleSubmit} disabled={!isValid}>
          Generate AI Budget →
        </Button>
      </CardContent>
    </Card>
  )
}

function formatAmount(value: number) {
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'USD',
    maximumFractionDigits: 2,
  }).format(value)
}
