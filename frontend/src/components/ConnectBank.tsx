import { useState, useCallback, useMemo } from 'react'
import { usePlaidLink } from 'react-plaid-link'
import { Button } from '@/components/ui/button'
import { Loader2, Plus } from 'lucide-react'
import toast from 'react-hot-toast'

function sleep(ms: number): Promise<void> {
  return new Promise(r => setTimeout(r, ms))
}

async function backoff<T>(fn: () => Promise<T>, max = 5): Promise<T> {
  let lastErr: any
  for (let i = 0; i < max; i++) {
    try {
      return await fn()
    } catch (e: any) {
      const code = e?.response?.data?.error_code || e?.error_code
      if (code !== 'RATE_LIMIT_EXCEEDED' && code !== 'RATE_LIMIT') throw e
      const ms = 500 * Math.pow(2, i) + Math.floor(Math.random() * 250)
      await sleep(ms)
      lastErr = e
    }
  }
  throw lastErr
}

interface ConnectBankProps {
  onSuccess?: (publicToken: string, metadata: any) => void
  disabled?: boolean
  hasConnections?: boolean
}

export default function ConnectBank({ onSuccess, disabled = false, hasConnections = false }: ConnectBankProps) {
  const [linkToken, setLinkToken] = useState<string | null>(null)
  const [creating, setCreating] = useState(false)

  const createLinkToken = useCallback(async () => {
    if (creating || linkToken) return // debounce
    setCreating(true)
    try {
      const result = await backoff(async () => {
        const resp = await fetch('/api/plaid/link/token/create', {
          method: 'POST',
          credentials: 'include',
          headers: {
            'Content-Type': 'application/json'
          }
        })
        if (!resp.ok) {
          if (resp.status === 429) {
            const error = new Error('RATE_LIMIT_EXCEEDED')
            ;(error as any).error_code = 'RATE_LIMIT'
            throw error
          }
          throw new Error('LINK_TOKEN_CREATE_FAILED')
        }
        return resp.json()
      })
      setLinkToken(result.link_token)
    } catch (error: any) {
      console.error('Failed to create link token:', error)
      const code = error?.error_code || error?.code
      if (code === 'RATE_LIMIT' || code === 'RATE_LIMIT_EXCEEDED') {
        toast.error('Rate limit exceeded. Please wait a few minutes and try again.')
      } else {
        toast.error('Failed to prepare bank connection. Please try again.')
      }
    } finally {
      setCreating(false)
    }
  }, [creating, linkToken])

  const config = useMemo(() => linkToken ? ({
    token: linkToken,
    onSuccess: (public_token: string, metadata: any) => {
      setLinkToken(null) // Reset for next connection
      onSuccess?.(public_token, metadata)
    },
    onExit: () => {
      setLinkToken(null) // Reset on exit
    }
  }) : undefined, [linkToken, onSuccess])

  const { open, ready } = usePlaidLink(config || { token: '' })

  const handleConnect = useCallback(() => {
    if (ready && open) {
      open()
    }
  }, [ready, open])

  return (
    <div className="flex gap-2">
      {!linkToken && (
        <Button
          onClick={createLinkToken}
          disabled={creating || disabled}
          size="sm"
        >
          {creating ? (
            <>
              <Loader2 className="h-4 w-4 animate-spin mr-2" />
              Preparing...
            </>
          ) : (
            <>
              <Plus className="h-4 w-4 mr-2" />
              {hasConnections ? 'Add Bank' : 'Connect Bank'}
            </>
          )}
        </Button>
      )}
      {linkToken && (
        <Button
          onClick={handleConnect}
          disabled={!ready || disabled}
          size="sm"
        >
          {!ready ? (
            <>
              <Loader2 className="h-4 w-4 animate-spin mr-2" />
              Loading...
            </>
          ) : (
            <>
              <Plus className="h-4 w-4 mr-2" />
              Connect Accounts
            </>
          )}
        </Button>
      )}
    </div>
  )
}