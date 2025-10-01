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

export default function Budget() {
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
  const connectedBanks = plaidStatusQuery.data?.items || []
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
    ? 'Connect your bank to import the last 90 days of activity for smart budgeting.'
    : hasTargets
        ? 'Your monthly budget helps track progress and manage spending.'
        : connectedBanks.length > 0
          ? `${connectedBanks.length} bank${connectedBanks.length !== 1 ? 's' : ''} connected. Review your snapshot and create your budget.`
          : 'Review your snapshot and create your budget.'

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
        <Card key={target.category} className="border border-border bg-card">
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
            <div className="flex flex-wrap items-center justify-between gap-2 text-sm text-muted-foreground">
              <span>Actual: {formatAmount(actual)}</span>
              <span>Target: {formatAmount(target.target)}</span>
              <span className={overBudget ? 'text-destructive' : ''}>
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
    navigate('/plan')
  }

  const handleConnected = () => {
    plaidStatusQuery.refetch()
    snapshotQuery.refetch()
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold text-foreground">Monthly Snapshot</h1>
          <p className="text-muted-foreground">{headerSubtitle}</p>
        </div>
        {isPlaidLinkEnabled && connectedBanks.length > 0 && (
          <div className="text-right">
            <p className="text-sm text-muted-foreground">
              {connectedBanks.map((bank: any) => bank.institutionName).join(', ')}
            </p>
            <p className="text-xs text-muted-foreground">
              Connected banks • <a href="/settings" className="text-primary hover:underline">Manage</a>
            </p>
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
            <div className="flex items-center gap-2 text-muted-foreground">
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
                <CardTitle>Budget for {snapshot?.month}</CardTitle>
                <CardDescription>
                  Track how your actual spending compares to your budget plan.
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
          actionLabel="Plan a Goal"
          onSync={shouldShowActions ? handleSync : undefined}
          isSyncing={isSyncing || snapshotQuery.isFetching}
          hasPlaidItem={hasPlaidItem}
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
          We'll securely link your accounts with Plaid and import the last 90 days of activity.
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
          <div className="flex items-center gap-2 text-sm text-muted-foreground">
            {isSyncing ? <Loader2 className="h-4 w-4 animate-spin" /> : null}
            <span>{statusMessage ?? 'Finishing up…'}</span>
          </div>
        )}
        {connectError ? (
          <p className="text-sm text-destructive">{connectError}</p>
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

interface BadgeToneProps {
  savingsAmount: number
  income: number
}

function BadgeTone({ savingsAmount, income }: BadgeToneProps) {
  const rate = income > 0 ? Math.max(0, savingsAmount / income) : 0
  const percentage = (rate * 100).toFixed(0)
  return (
    <div className="rounded-full border border-primary/40 bg-primary/10 px-4 py-1 text-sm text-primary">
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
    reason: entry.reason ?? 'Budget target',
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