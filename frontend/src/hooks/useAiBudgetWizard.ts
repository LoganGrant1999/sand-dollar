import { useCallback, useEffect, useMemo, useState } from 'react'
import type { GoalFormData, GenerateBudgetResponse, CategoryActual } from '@/types'

const STORAGE_KEY = 'ai-budget-wizard'

export interface AiBudgetWizardState {
  month?: string
  goals?: GoalFormData
  generatedBudget?: GenerateBudgetResponse
  actuals?: CategoryActual[]
}

const defaultState: AiBudgetWizardState = {
  month: undefined,
  goals: undefined,
  generatedBudget: undefined,
  actuals: [],
}

function loadState(): AiBudgetWizardState {
  if (typeof window === 'undefined') {
    return defaultState
  }

  try {
    const raw = window.localStorage.getItem(STORAGE_KEY)
    if (!raw) return defaultState
    const parsed = JSON.parse(raw) as AiBudgetWizardState
    return {
      ...defaultState,
      ...parsed,
      actuals: parsed.actuals || [],
    }
  } catch (error) {
    console.warn('Failed to parse AI budget wizard state', error)
    return defaultState
  }
}

export function useAiBudgetWizardState() {
  const [state, setState] = useState<AiBudgetWizardState>(() => loadState())

  useEffect(() => {
    if (typeof window === 'undefined') return

    const isDefault =
      !state.month &&
      !state.goals &&
      !state.generatedBudget &&
      (!state.actuals || state.actuals.length === 0)

    if (isDefault) {
      window.localStorage.removeItem(STORAGE_KEY)
      return
    }

    window.localStorage.setItem(STORAGE_KEY, JSON.stringify(state))
  }, [state])

  const updateState = useCallback((updates: Partial<AiBudgetWizardState>) => {
    setState((prev) => ({
      ...prev,
      ...updates,
      actuals: updates.actuals ?? prev.actuals ?? [],
    }))
  }, [])

  const resetState = useCallback(() => {
    setState(defaultState)
  }, [])

  const snapshot = useMemo(() => state, [state])

  return {
    state: snapshot,
    updateState,
    resetState,
  }
}
