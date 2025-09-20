import { useEffect, useMemo, useState } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { Navigate, Route, Routes, useLocation, useNavigate } from 'react-router-dom'
import toast from 'react-hot-toast'
import SnapshotCard from '@/components/SnapshotCard'
import GoalsForm from '@/components/GoalsForm'
import AiBudgetPreview from '@/components/AiBudgetPreview'
import BudgetBaselineCard from '@/components/BudgetBaselineCard'
import BudgetOverviewCard from '@/components/BudgetOverviewCard'
import ProgressBar from '@/components/ui/ProgressBar'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { useBudgetSnapshot, useGenerateAiBudget, useAcceptAiBudget, AI_BUDGET_QUERY_KEY } from '@/hooks/useAiBudget'
import { useAiBudgetWizardState } from '@/hooks/useAiBudgetWizard'
import { 
  createPlaidLinkToken,
  exchangePlaidPublicToken,
  triggerPlaidInitialSync,
  fetchPlaidStatus
} from '@/lib/api'
import type {
  GoalFormData,
  GenerateBudgetResponse,
  CategoryTarget,
  FinancialSnapshotResponse,
  CategoryActual as SnapshotCategoryActual,
} from '@/types'
import { Loader2 } from 'lucide-react'
import { isAxiosError } from 'axios'
import { usePlaidLink } from 'react-plaid-link'

const PLAID_STATUS_QUERY_KEY = ['plaid', 'status'] as const

function getAxiosStatus(error: unknown): number | null {
  if (isAxiosError(error)) {
    return error.response?.status ?? null
  }
  return null
}

export default function Budgeting() {
  return (
    <Routes>
      <Route index element={<BudgetOverview />} />
      <Route path="ai/*" element={<AiBudgetWizard />} />
      <Route path="*" element={<Navigate to="." replace />} />
    </Routes>
  )
}

function BudgetOverview() {
  const navigate = useNavigate()
  const { updateState, resetState } = useAiBudgetWizardState()
  const snapshotQuery = useBudgetSnapshot()
  const snapshot = snapshotQuery.data
  const hasTargets = snapshot?.targetsByCategory && snapshot.targetsByCategory.length > 0
  const isPlaidLinkEnabled = import.meta.env.VITE_ENABLE_PLAID_LINK === 'true'

  const plaidStatusQuery = useQuery({
    queryKey: PLAID_STATUS_QUERY_KEY,
    queryFn: fetchPlaidStatus,
    enabled: isPlaidLinkEnabled,
    retry: false,
  })

  const snapshotErrorStatus = getAxiosStatus(snapshotQuery.error)
  const plaidStatusErrorStatus = getAxiosStatus(plaidStatusQuery.error)
  const loginRequired = snapshotErrorStatus === 401

  const hasPlaidItem = plaidStatusQuery.data?.hasItem ?? false
  const showConnectSection =
    isPlaidLinkEnabled && !plaidStatusQuery.isLoading && !hasPlaidItem

  // Add sync state
  const [isSyncing, setIsSyncing] = useState(false)
  const queryClient = useQueryClient()

  // Handle sync with refresh combined
  const handleSync = async () => {
    const isRefreshing = snapshotQuery.isFetching
    const isSyncingTransactions = isSyncing

    if (isRefreshing || isSyncingTransactions) return

    setIsSyncing(true)
    try {
      // If we have Plaid connected, sync transactions first
      if (hasPlaidItem) {
        await triggerPlaidInitialSync()
        toast.success('Transactions synced successfully!')
      }
      // Always refresh the snapshot data
      await snapshotQuery.refetch()
    } catch (error) {
      console.error('Sync failed:', error)
      toast.error(hasPlaidItem ? 'Failed to sync transactions. Please try again.' : 'Failed to refresh data. Please try again.')
    } finally {
      setIsSyncing(false)
    }
  }

  const headerSubtitle = showConnectSection
    ? 'Connect your bank to import the last 90 days of activity for AI budgeting.'
    : hasTargets
        ? 'Your AI-managed targets help track progress every month.'
        : 'Review your snapshot and let AI build a personalized plan.'

  const targetCards = useMemo(() => {
    if (!snapshot?.targetsByCategory) return []

    const actualEntries = (snapshot.actualsByCategory ?? []).map(mapActualEntry)
    const actualMap = new Map(actualEntries.map((item) => [item.category, item]))
    const targets = snapshot.targetsByCategory.map(mapTargetEntry)

    return targets.map((target) => {
      const actualEntry = actualMap.get(target.category)
      const actual = actualEntry?.actual ?? 0
      const overBudget = target.target > 0 && actual > target.target
      return (
        <Card key={target.category} className="border border-[rgba(255,255,255,0.08)] bg-[rgba(255,255,255,0.02)]">
          <CardHeader>
            <CardTitle className="text-base">{target.category}</CardTitle>
            <CardDescription>{target.reason}</CardDescription>
          </CardHeader>
          <CardContent className="space-y-3">
            <ProgressBar
              value={actual}
              max={target.target}
              variant={overBudget ? 'danger' : 'default'}
              label="Actual vs Target"
              showPercentage
            />
            <div className="flex flex-wrap items-center justify-between gap-2 text-sm text-[var(--color-text-secondary)]">
              <span>Actual: {formatAmount(actual)}</span>
              <span>Target: {formatAmount(target.target)}</span>
              <span className={overBudget ? 'text-[var(--color-error)]' : ''}>
                Diff: {formatAmount(target.target - actual)}
              </span>
            </div>
          </CardContent>
        </Card>
      )
    })
  }, [snapshot])

  const incomeNumber = snapshot ? toNumber(snapshot.income) : 0
  const savingsValue = snapshot ? toNumber(snapshot.totals?.savings) : 0
  const expenseValue = snapshot ? toNumber(snapshot.totals?.expenses) : 0
  const fallbackSavings = Math.max(0, incomeNumber - expenseValue)
  const shouldShowActions = !showConnectSection

  if (loginRequired) {
    return <BudgetLoginRequiredCard />
  }

  const handleCustomizeBudget = () => {
    const normalizedMonth = normalizeMonth(snapshot?.month)
    resetState()
    updateState({
      month: normalizedMonth,
      actuals: (snapshot?.actualsByCategory ?? []).map(mapActualEntry),
    })
    navigate('/budgeting/ai/goals')
  }

  const handleAdjustTargets = () => {
    if (!snapshot || !snapshot.targetsByCategory) {
      handleCustomizeBudget()
      return
    }

    const normalizedMonth = normalizeMonth(snapshot.month)
    const actuals = (snapshot.actualsByCategory ?? []).map(mapActualEntry)
    const targets = (snapshot.targetsByCategory ?? []).map(mapTargetEntry)
    const income = toNumber(snapshot.income)
    const plannedTotal = targets.reduce((sum, target) => sum + target.target, 0)
    const savingsRate = income > 0 ? Math.max(0, (income - plannedTotal) / income) : 0

    const generatedBudget: GenerateBudgetResponse = {
      month: normalizedMonth,
      targetsByCategory: targets.map(({ category, target, reason }) => ({ category, target, reason })),
      summary: {
        savingsRate,
        notes: ['Loaded from saved AI budget targets.'],
      },
      promptTokens: 0,
      completionTokens: 0,
    }

    updateState({
      month: normalizedMonth,
      generatedBudget,
      actuals,
    })
    navigate('/budgeting/ai/review')
  }

  const handleConnected = () => {
    plaidStatusQuery.refetch()
    snapshotQuery.refetch()
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
        <div>
          <h1 className="text-3xl font-bold text-[var(--color-text-primary)]">Budgeting</h1>
          <p className="text-sm text-[var(--color-text-secondary)]">{headerSubtitle}</p>
        </div>
        {shouldShowActions && (
          <div className="flex gap-2">
            <Button variant="outline" onClick={handleSync} disabled={isSyncing || snapshotQuery.isFetching}>
              {(isSyncing || snapshotQuery.isFetching) ? (
                <span className="flex items-center gap-2">
                  <Loader2 className="h-4 w-4 animate-spin" />
                  {hasPlaidItem ? 'Syncing' : 'Refreshing'}
                </span>
              ) : (
                hasPlaidItem ? 'Sync' : 'Refresh'
              )}
            </Button>
            {hasTargets ? (
              <Button onClick={handleAdjustTargets}>Adjust Targets</Button>
            ) : (
              <Button onClick={handleCustomizeBudget}>Customize Budget</Button>
            )}
          </div>
        )}
      </div>

      {isPlaidLinkEnabled && plaidStatusQuery.isLoading ? (
        <Card>
          <CardHeader>
            <CardTitle>Checking bank connections…</CardTitle>
            <CardDescription>Hang tight while we verify your Plaid status.</CardDescription>
          </CardHeader>
          <CardContent>
            <div className="flex items-center gap-2 text-[var(--color-text-secondary)]">
              <Loader2 className="h-4 w-4 animate-spin" />
              <span>Loading Plaid status…</span>
            </div>
          </CardContent>
        </Card>
      ) : showConnectSection ? (
        <BudgetPlaidConnectSection onConnected={handleConnected} />
      ) : hasTargets ? (
        <div className="space-y-6">
          <Card>
            <CardHeader className="flex flex-col gap-2 md:flex-row md:items-center md:justify-between">
              <div>
                <CardTitle>AI Budget for {snapshot?.month}</CardTitle>
                <CardDescription>
                  Track how your actual spending compares to the AI plan.
                </CardDescription>
              </div>
              <BadgeTone
                savingsAmount={savingsValue > 0 ? savingsValue : fallbackSavings}
                income={incomeNumber}
              />
            </CardHeader>
          </Card>
          <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">{targetCards}</div>
        </div>
      ) : (
        <BudgetOverviewCard
          onAction={handleCustomizeBudget}
          actionLabel="Customize Budget"
        />
      )}
    </div>
  )
}

interface BudgetPlaidConnectSectionProps {
  onConnected: () => void
}

function BudgetPlaidConnectSection({ onConnected }: BudgetPlaidConnectSectionProps) {
  const queryClient = useQueryClient()
  const plaidEnv = (import.meta.env.VITE_PLAID_ENV as string | undefined) ?? 'sandbox'
  const [linkToken, setLinkToken] = useState<string | null>(null)
  const [isPreparing, setIsPreparing] = useState(false)
  const [isSyncing, setIsSyncing] = useState(false)
  const [statusMessage, setStatusMessage] = useState<string | null>(null)
  const [linkSessionActive, setLinkSessionActive] = useState(false)
  const [connectError, setConnectError] = useState<string | null>(null)

  const startLinkFlow = async () => {
    if (isPreparing || isSyncing || linkSessionActive) return
    try {
      setIsPreparing(true)
      setStatusMessage(null)
      setConnectError(null)
      const { link_token } = await createPlaidLinkToken()
      setLinkToken(link_token)
      setLinkSessionActive(true)
    } catch (error: any) {
      console.error('Failed to create Plaid link token', error)
      let message = 'Unable to start Plaid Link. Please try again.'
      if (error && typeof error === 'object' && 'code' in error) {
        const typed = error as { code?: string; type?: string; message?: string }
        message = `${typed.type ?? 'Plaid error'} (${typed.code ?? 'unknown'}): ${typed.message ?? 'Request failed.'}`
      } else if (isAxiosError(error)) {
        const status = error.response?.status
        if (status === 401) {
          message = 'Please sign in to connect your bank.'
        }
      } else if (error?.code === 'ERR_NETWORK') {
        message = 'Cannot reach the server. Please ensure the backend is running.'
      }
      setConnectError(message)
      toast.error(message)
      setLinkToken(null)
      setLinkSessionActive(false)
    } finally {
      setIsPreparing(false)
    }
  }

  const completeSync = async () => {
    setStatusMessage('Importing last 90 days…')
    setIsSyncing(true)
    try {
      await triggerPlaidInitialSync()
    } finally {
      setIsSyncing(false)
    }
  }

  const handleLinkSuccess = async (publicToken: string) => {
    try {
      setStatusMessage('Connecting your bank…')
      await exchangePlaidPublicToken(publicToken)
      toast.success('Bank connected!')
      setConnectError(null)
      await completeSync()
      toast.success('Transactions imported!')
      setStatusMessage(null)
      await queryClient.invalidateQueries({ queryKey: PLAID_STATUS_QUERY_KEY })
      await queryClient.invalidateQueries({ queryKey: AI_BUDGET_QUERY_KEY })
      onConnected()
    } catch (error: any) {
      console.error('Plaid connection failed', error)
      let message = 'We could not complete the import. Please try again.'
      if (error && typeof error === 'object' && 'code' in error) {
        const typed = error as { code?: string; type?: string; message?: string }
        message = `${typed.type ?? 'Plaid error'} (${typed.code ?? 'unknown'}): ${typed.message ?? 'Request failed.'}`
      } else if (isAxiosError(error)) {
        const status = error.response?.status
        if (status === 401) {
          message = 'Your session expired. Please sign in again.'
        }
      } else if (error?.code === 'ERR_NETWORK') {
        message = 'Cannot reach the server. Please ensure the backend is running.'
      }
      setConnectError(message)
      toast.error(message)
      setStatusMessage(null)
    } finally {
      setLinkSessionActive(false)
      setLinkToken(null)
    }
  }

  const handleLinkExit = () => {
    setLinkSessionActive(false)
    setLinkToken(null)
    setStatusMessage(null)
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle>Connect your bank</CardTitle>
        <CardDescription>
          We’ll securely link your accounts with Plaid and import the last 90 days of activity.
        </CardDescription>
      </CardHeader>
      <CardContent className="space-y-4">
        <Button onClick={startLinkFlow} disabled={isPreparing || isSyncing}>
          {isPreparing ? (
            <span className="flex items-center gap-2">
              <Loader2 className="h-4 w-4 animate-spin" /> Preparing Link
            </span>
          ) : (
            'Connect your bank'
          )}
        </Button>
        {(isSyncing || statusMessage) && (
          <div className="flex items-center gap-2 text-sm text-[var(--color-text-secondary)]">
            {isSyncing ? <Loader2 className="h-4 w-4 animate-spin" /> : null}
            <span>{statusMessage ?? 'Finishing up…'}</span>
          </div>
        )}
        {connectError ? (
          <p className="text-sm text-[var(--color-error)]">{connectError}</p>
        ) : null}
      </CardContent>
      {linkToken && linkSessionActive ? (
        <PlaidLinkFlow
          token={linkToken}
          openWhenReady={linkSessionActive}
          onSuccess={handleLinkSuccess}
          onExit={handleLinkExit}
          env={plaidEnv}
        />
      ) : null}
    </Card>
  )
}

function BudgetLoginRequiredCard() {
  const navigate = useNavigate()

  return (
    <Card>
      <CardHeader>
        <CardTitle>Sign in to continue</CardTitle>
        <CardDescription>
          Your session has expired or you are not signed in. Please log in to connect your bank and view your spending snapshot.
        </CardDescription>
      </CardHeader>
      <CardContent>
        <Button onClick={() => navigate('/login')}>Go to login</Button>
      </CardContent>
    </Card>
  )
}

interface PlaidLinkFlowProps {
  token: string
  openWhenReady: boolean
  onSuccess: (public_token: string) => void
  onExit: () => void
  env: string
}

function PlaidLinkFlow({ token, openWhenReady, onSuccess, onExit, env }: PlaidLinkFlowProps) {
  const config = {
    token,
    onSuccess: (publicToken: string) => {
      onSuccess(publicToken)
    },
    onExit: () => {
      onExit()
    },
    onEvent: (eventName: string) => {
      if (eventName === 'EXIT') {
        onExit()
      }
    },
    env,
  } as Parameters<typeof usePlaidLink>[0] & { env?: string }

  const { open, ready } = usePlaidLink(config as any)

  useEffect(() => {
    if (openWhenReady && ready) {
      open()
    }
  }, [openWhenReady, ready, open])

  return null
}

interface WizardErrorState {
  message: string
  originalData: GoalFormData
  fallback?: GenerateBudgetResponse
}

function AiBudgetWizard() {
  const navigate = useNavigate()
  const location = useLocation()
  const { state, updateState, resetState } = useAiBudgetWizardState()
  const snapshotQuery = useBudgetSnapshot()
  const generateMutation = useGenerateAiBudget()
  const acceptMutation = useAcceptAiBudget()
  const [wizardError, setWizardError] = useState<WizardErrorState | null>(null)

  useEffect(() => {
    if (snapshotQuery.data) {
      const normalizedMonth = normalizeMonth(snapshotQuery.data.month)
      if (!state.month) {
        updateState({ month: normalizedMonth })
      }
      if (!state.actuals || state.actuals.length === 0) {
        updateState({ actuals: snapshotQuery.data.actualsByCategory?.map(mapActualEntry) })
      }
    }
  }, [snapshotQuery.data, state.month, state.actuals, updateState])

  const steps = [
    { path: '/budgeting/ai/snapshot', title: 'Snapshot' },
    { path: '/budgeting/ai/goals', title: 'Goals' },
    { path: '/budgeting/ai/review', title: 'Review & Accept' },
  ]

  const currentStepIndex = steps.findIndex((step) => location.pathname.startsWith(step.path))

  const handleGoalsSubmit = (data: GoalFormData) => {
    const month = state.month ?? normalizeMonth(snapshotQuery.data?.month)
    if (!month) {
      toast.error('Unable to determine budget month. Please refresh and try again.')
      return
    }

    const payload = {
      month,
      goals: data.goals,
      style: data.style,
      constraints: {
        mustKeepCategories: data.mustKeepCategories || undefined,
        categoryCaps: data.categoryCaps || undefined,
      },
      notes: data.notes || undefined,
    }

    setWizardError(null)
    updateState({ goals: data })

    generateMutation.mutate(payload, {
      onSuccess: (response) => {
        updateState({ goals: data, generatedBudget: response })
        navigate('/budgeting/ai/review')
      },
      onError: (error: any) => {
        const message = error?.response?.status === 429
          ? 'You have requested several AI budgets. Please wait a minute before trying again.'
          : 'We could not reach the AI service right now. You can retry or continue with a heuristic budget.'

        const fallback = snapshotQuery.data
          ? buildHeuristicBudget(snapshotQuery.data, data)
          : undefined

        setWizardError({
          message,
          originalData: data,
          fallback,
        })
      },
    })
  }

  const handleAccept = (targets: CategoryTarget[]) => {
    const month = state.month ?? normalizeMonth(snapshotQuery.data?.month)
    if (!month) {
      toast.error('Unable to determine budget month. Please refresh and try again.')
      return
    }

    acceptMutation.mutate(
      {
        month,
        targetsByCategory: targets,
      },
      {
        onSuccess: () => {
          resetState()
          navigate('/budgeting')
        },
      }
    )
  }

  const useFallback = (fallback: GenerateBudgetResponse, goals: GoalFormData) => {
    setWizardError(null)
    const actuals = snapshotQuery.data?.actualsByCategory?.map(mapActualEntry) ?? state.actuals
    updateState({
      goals,
      month: state.month ?? normalizeMonth(snapshotQuery.data?.month),
      actuals,
      generatedBudget: fallback,
    })
    navigate('/budgeting/ai/review')
  }

  const retry = (formData: GoalFormData) => {
    setWizardError(null)
    handleGoalsSubmit(formData)
  }

  return (
    <div className="space-y-6">
      {wizardError && (
        <div className="rounded-[1rem] border border-[var(--color-warning)]/60 bg-[var(--color-warning)]/15 p-4 space-y-3">
          <p className="text-sm text-[var(--color-warning)]">{wizardError.message}</p>
          <div className="flex flex-wrap gap-2">
            <Button variant="outline" onClick={() => retry(wizardError.originalData)}>
              Try again
            </Button>
            {wizardError.fallback && (
              <Button
                onClick={() => {
                  const fallbackBudget = wizardError.fallback
                  if (fallbackBudget) {
                    useFallback(fallbackBudget, wizardError.originalData)
                  }
                }}
              >
                Use heuristic budget instead
              </Button>
            )}
          </div>
        </div>
      )}

      <div className="flex flex-wrap items-center gap-2">
        {steps.map((step, index) => (
          <div
            key={step.path}
            className={`rounded-full px-4 py-2 text-sm transition-colors ${
              index === currentStepIndex
                ? 'bg-[var(--color-accent-teal)] text-[var(--color-bg-dark)]'
                : 'bg-[rgba(255,255,255,0.08)] text-[var(--color-text-secondary)]'
            }`}
          >
            {step.title}
          </div>
        ))}
      </div>

      <Routes>
        <Route index element={<Navigate to="snapshot" replace />} />
        <Route
          path="snapshot"
          element={
            <SnapshotStep
              snapshot={snapshotQuery.data}
              isLoading={snapshotQuery.isLoading}
              error={snapshotQuery.error as Error | undefined}
              onRetry={snapshotQuery.refetch}
              onContinue={() => {
                updateState({
                  month: state.month ?? normalizeMonth(snapshotQuery.data?.month),
                  actuals: snapshotQuery.data?.actualsByCategory?.map(mapActualEntry) ?? state.actuals,
                })
                navigate('/budgeting/ai/goals')
              }}
            />
          }
        />
        <Route
          path="goals"
          element={
            <GoalsForm
              onNext={handleGoalsSubmit}
              initialData={state.goals}
              onChange={(data) => updateState({ goals: data })}
            />
          }
        />
        <Route
          path="review"
          element={
            state.generatedBudget ? (
              <AiBudgetPreview
                budgetData={state.generatedBudget}
                month={state.month ?? normalizeMonth(snapshotQuery.data?.month)}
                onBack={() => navigate('/budgeting/ai/goals')}
                onAccept={handleAccept}
                accepting={acceptMutation.isPending}
                actualsByCategory={state.actuals}
              />
            ) : (
              <Navigate to="../goals" replace />
            )
          }
        />
        <Route path="*" element={<Navigate to="snapshot" replace />} />
      </Routes>
    </div>
  )
}

interface SnapshotStepProps {
  snapshot?: FinancialSnapshotResponse | null
  isLoading: boolean
  error?: Error
  onRetry?: () => void
  onContinue: () => void
}

function SnapshotStep({ snapshot, isLoading, error, onRetry, onContinue }: SnapshotStepProps) {
  return (
    <div className="space-y-6">
      <SnapshotCard
        snapshot={snapshot}
        isLoading={isLoading}
        error={error}
        onRetry={onRetry}
      />
      <div className="flex justify-end">
        <Button onClick={onContinue} disabled={isLoading || Boolean(error)}>
          Customize Budget
        </Button>
      </div>
    </div>
  )
}

interface BadgeToneProps {
  savingsAmount: number
  income: number
}

function BadgeTone({ savingsAmount, income }: BadgeToneProps) {
  const rate = income > 0 ? Math.max(0, savingsAmount / income) : 0
  const percentage = (rate * 100).toFixed(0)
  return (
    <div className="rounded-full border border-[var(--color-accent-teal)]/40 bg-[var(--color-accent-teal)]/10 px-4 py-1 text-sm text-[var(--color-accent-teal)]">
      Savings rate {percentage}%
    </div>
  )
}

function normalizeMonth(rawMonth?: string): string {
  if (!rawMonth) {
    return new Date().toISOString().slice(0, 7)
  }

  if (/^\d{4}-\d{2}$/.test(rawMonth)) {
    return rawMonth
  }

  const parsed = new Date(rawMonth)
  if (!Number.isNaN(parsed.getTime())) {
    const month = String(parsed.getMonth() + 1).padStart(2, '0')
    return `${parsed.getFullYear()}-${month}`
  }

  return new Date().toISOString().slice(0, 7)
}

function formatAmount(value: number) {
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'USD',
  }).format(value)
}

function mapActualEntry(entry: SnapshotCategoryActual) {
  return {
    category: entry.category,
    actual: toNumber(entry.actual),
    target: entry.target !== undefined ? toNumber(entry.target) : undefined,
  }
}

function mapTargetEntry(entry: CategoryTarget) {
  return {
    category: entry.category,
    target: toNumber(entry.target),
    reason: entry.reason ?? 'AI recommended target',
  }
}

function toNumber(value: unknown): number {
  if (typeof value === 'number') return value
  if (typeof value === 'string') {
    const parsed = parseFloat(value)
    return Number.isNaN(parsed) ? 0 : parsed
  }
  if (value instanceof Object && value !== null && 'toString' in value) {
    const parsed = parseFloat((value as { toString(): string }).toString())
    return Number.isNaN(parsed) ? 0 : parsed
  }
  return 0
}

function buildHeuristicBudget(snapshot: FinancialSnapshotResponse, goals: GoalFormData): GenerateBudgetResponse {
  const month = normalizeMonth(snapshot.month)
  const income = toNumber(snapshot.income)
  const actualEntries = (snapshot.actualsByCategory ?? []).map(mapActualEntry)

  const caps = goals.categoryCaps ?? {}
  const mustKeep = new Set((goals.mustKeepCategories ?? []).map((c) => c.toLowerCase()))
  const styleFactor = getStyleFactor(goals.style)

  const initialTargets = actualEntries.map((entry) => {
    const lower = entry.category.toLowerCase()
    const capValue = (caps as Record<string, number | undefined>)[entry.category] ?? (caps as Record<string, number | undefined>)[lower]
    const cap = capValue !== undefined ? toNumber(capValue) : undefined

    let proposed = mustKeep.has(lower) ? entry.actual : entry.actual * styleFactor
    if (cap !== undefined && cap >= 0) {
      proposed = Math.min(proposed, cap)
    }

    const rounded = Math.max(0, Math.round(proposed))
    const reason = mustKeep.has(lower)
      ? 'Kept close to recent spend per constraint'
      : `Based on recent average with ${goals.style} adjustments`

    return { category: entry.category, target: rounded, reason }
  })

  const targets = [...initialTargets]
  const existing = new Set(targets.map((t) => t.category.toLowerCase()))

  if (targets.length === 0 && income > 0) {
    targets.push(
      { category: 'Essentials', target: Math.round(income * 0.5), reason: '50% baseline for needs' },
      { category: 'Lifestyle', target: Math.round(income * 0.3), reason: '30% for wants and flexibility' }
    )
    existing.add('essentials')
    existing.add('lifestyle')
  }

  for (const goal of goals.goals) {
    const lower = goal.toLowerCase()
    if (lower.includes('emergency') && !existing.has('emergency fund')) {
      targets.push({ category: 'Emergency Fund', target: 500, reason: 'Reserve for upcoming emergency goal' })
      existing.add('emergency fund')
    }
    if ((lower.includes('debt') || lower.includes('card')) && !existing.has('card paydown')) {
      targets.push({ category: 'Card Paydown', target: 200, reason: 'Monthly contribution toward debt reduction' })
      existing.add('card paydown')
    }
  }

  if (income > 0 && !existing.has('savings')) {
    const savingsTarget = Math.max(100, Math.round(income * 0.2))
    targets.push({ category: 'Savings', target: savingsTarget, reason: 'Allocate ~20% toward savings goals' })
    existing.add('savings')
  }

  let normalizedTargets = targets;
  if (income > 0) {
    const total = targets.reduce((sum, target) => sum + target.target, 0)
    if (total > income) {
      const ratio = income / total
      normalizedTargets = targets.map((target) => ({
        ...target,
        target: Math.max(0, Math.round(target.target * ratio)),
      }))
    }
  }

  const totalTargets = normalizedTargets.reduce((sum, target) => sum + target.target, 0)
  const savingsRate = income > 0 ? Math.max(0, (income - totalTargets) / income) : 0

  return {
    month,
    targetsByCategory: normalizedTargets,
    summary: {
      savingsRate,
      notes: ['Heuristic budget generated locally after AI error.'],
    },
    promptTokens: 0,
    completionTokens: 0,
  }
}

function getStyleFactor(style: GoalFormData['style']): number {
  switch (style) {
    case 'aggressive':
      return 0.85
    case 'flexible':
      return 1.05
    default:
      return 1.0
  }
}
