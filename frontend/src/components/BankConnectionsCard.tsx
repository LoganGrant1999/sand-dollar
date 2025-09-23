import React, { useState } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import toast from 'react-hot-toast'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import {
  Building2,
  Loader2,
  RefreshCw,
  CheckCircle,
  AlertCircle
} from 'lucide-react'
import {
  exchangePlaidPublicToken,
  triggerPlaidInitialSync,
  fetchPlaidStatus
} from '@/lib/api'
import type { PlaidItem } from '@/types'
import { isAxiosError } from 'axios'
import ConnectBank from './ConnectBank'

const PLAID_STATUS_QUERY_KEY = ['plaid', 'status'] as const

interface BankConnectionsCardProps {
  onConnectionChange?: () => void
}

export default function BankConnectionsCard({ onConnectionChange }: BankConnectionsCardProps) {
  const queryClient = useQueryClient()
  const [isSyncing, setIsSyncing] = useState(false)
  const [statusMessage, setStatusMessage] = useState<string | null>(null)
  const [connectError, setConnectError] = useState<string | null>(null)

  const plaidStatusQuery = useQuery({
    queryKey: PLAID_STATUS_QUERY_KEY,
    queryFn: fetchPlaidStatus,
    retry: false,
  })

  const connectedBanks = plaidStatusQuery.data?.items || []
  const hasConnections = connectedBanks.length > 0

  const handleBankConnectionSuccess = async (publicToken: string, metadata: any) => {
    try {
      setStatusMessage('Connecting your bank...')
      setConnectError(null)
      await exchangePlaidPublicToken(publicToken)
      toast.success('Bank connected!')
      await completeSync()
      toast.success('Transactions imported!')
      setStatusMessage(null)
      await queryClient.invalidateQueries({ queryKey: PLAID_STATUS_QUERY_KEY })
      onConnectionChange?.()
    } catch (error: any) {
      console.error('Plaid connection failed', error)
      let message = 'We could not complete the import. Please try again.'
      if (error && typeof error === 'object' && 'code' in error) {
        const typed = error as { code?: string; type?: string; message?: string }
        if (typed.type === 'RATE_LIMIT_EXCEEDED' || typed.code === 'RATE_LIMIT') {
          message = 'Rate limit exceeded. Please wait a few minutes and try again.'
        } else {
          message = `${typed.type ?? 'Plaid error'} (${typed.code ?? 'unknown'}): ${typed.message ?? 'Request failed.'}`
        }
      } else if (isAxiosError(error)) {
        const status = error.response?.status
        if (status === 401) {
          message = 'Your session expired. Please sign in again.'
        } else if (status === 429) {
          message = 'Rate limit exceeded. Please wait a few minutes and try again.'
        }
      } else if (error?.code === 'ERR_NETWORK') {
        message = 'Cannot reach the server. Please ensure the backend is running.'
      }
      setConnectError(message)
      toast.error(message)
      setStatusMessage(null)
    }
  }

  const completeSync = async () => {
    setStatusMessage('Importing transactions...')
    setIsSyncing(true)
    try {
      await triggerPlaidInitialSync()
    } finally {
      setIsSyncing(false)
    }
  }


  const handleRefreshAll = async () => {
    if (isSyncing) return
    setIsSyncing(true)
    try {
      await triggerPlaidInitialSync()
      toast.success('All accounts synced!')
      await queryClient.invalidateQueries({ queryKey: PLAID_STATUS_QUERY_KEY })
      onConnectionChange?.()
    } catch (error: any) {
      console.error('Sync failed:', error)
      toast.error('Failed to sync accounts. Please try again.')
    } finally {
      setIsSyncing(false)
    }
  }

  const getStatusColor = (status: string) => {
    switch (status.toLowerCase()) {
      case 'active':
        return 'bg-green-500/10 text-green-600 border-green-500/20'
      case 'error':
        return 'bg-red-500/10 text-red-600 border-red-500/20'
      default:
        return 'bg-yellow-500/10 text-yellow-600 border-yellow-500/20'
    }
  }

  const getStatusIcon = (status: string) => {
    switch (status.toLowerCase()) {
      case 'active':
        return <CheckCircle className="h-3 w-3" />
      case 'error':
        return <AlertCircle className="h-3 w-3" />
      default:
        return <AlertCircle className="h-3 w-3" />
    }
  }

  return (
    <>
      <Card>
        <CardHeader>
          <div className="flex items-center justify-between">
            <div>
              <CardTitle className="flex items-center gap-2">
                <Building2 className="h-5 w-5" />
                Connected Banks
              </CardTitle>
              <CardDescription>
                {hasConnections
                  ? `${connectedBanks.length} bank${connectedBanks.length !== 1 ? 's' : ''} connected`
                  : 'Connect your bank accounts to import transactions and track spending'
                }
              </CardDescription>
            </div>
            <div className="flex gap-2">
              {hasConnections && (
                <Button
                  variant="outline"
                  size="sm"
                  onClick={handleRefreshAll}
                  disabled={isSyncing}
                >
                  {isSyncing ? (
                    <Loader2 className="h-4 w-4 animate-spin" />
                  ) : (
                    <RefreshCw className="h-4 w-4" />
                  )}
                  {isSyncing ? 'Syncing...' : 'Sync All'}
                </Button>
              )}
              <ConnectBank
                onSuccess={handleBankConnectionSuccess}
                disabled={isSyncing}
                hasConnections={hasConnections}
              />
            </div>
          </div>
        </CardHeader>
        <CardContent className="space-y-4">
          {plaidStatusQuery.isLoading ? (
            <div className="flex items-center gap-2 text-[var(--color-text-secondary)]">
              <Loader2 className="h-4 w-4 animate-spin" />
              <span>Loading bank connections...</span>
            </div>
          ) : hasConnections ? (
            <div className="space-y-3">
              {connectedBanks.map((bank: PlaidItem) => (
                <div
                  key={bank.id}
                  className="flex items-center justify-between rounded-lg border border-[rgba(255,255,255,0.08)] bg-[rgba(255,255,255,0.02)] p-3"
                >
                  <div className="flex items-center gap-3">
                    <Building2 className="h-5 w-5 text-[var(--color-accent-teal)]" />
                    <div>
                      <p className="font-medium text-[var(--color-text-primary)]">
                        {bank.institutionName || 'Unknown Bank'}
                      </p>
                      <p className="text-sm text-[var(--color-text-secondary)]">
                        Connected {new Date(bank.createdAt).toLocaleDateString()}
                      </p>
                    </div>
                  </div>
                  <div className="flex items-center gap-2">
                    <Badge
                      variant="outline"
                      className={`text-xs ${getStatusColor(bank.status)}`}
                    >
                      {getStatusIcon(bank.status)}
                      {bank.status}
                    </Badge>
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <div className="text-center py-6">
              <Building2 className="h-12 w-12 text-[var(--color-text-secondary)] mx-auto mb-4" />
              <p className="text-[var(--color-text-secondary)] mb-4">
                No banks connected yet. Connect your accounts to get started.
              </p>
            </div>
          )}

          {(isSyncing || statusMessage) && (
            <div className="flex items-center gap-2 text-sm text-[var(--color-text-secondary)] p-3 bg-[rgba(255,255,255,0.02)] rounded-lg">
              {isSyncing ? <Loader2 className="h-4 w-4 animate-spin" /> : null}
              <span>{statusMessage ?? 'Finishing up...'}</span>
            </div>
          )}

          {connectError && (
            <div className="text-sm text-[var(--color-error)] p-3 bg-[var(--color-error)]/10 rounded-lg border border-[var(--color-error)]/20">
              {connectError}
            </div>
          )}
        </CardContent>
      </Card>

    </>
  )
}