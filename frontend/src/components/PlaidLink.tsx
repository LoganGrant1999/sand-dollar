import { useState, useEffect } from 'react'
import { usePlaidLink } from 'react-plaid-link'
import { Button } from '@/components/ui/button'
import { Plus } from 'lucide-react'
import { createPlaidLinkToken } from '@/lib/api'

interface PlaidLinkProps {
  onSuccess: (public_token: string, metadata: any) => void
  onExit?: (err: any, metadata: any) => void
  isLoading?: boolean
}

export default function PlaidLink({ onSuccess, onExit, isLoading = false }: PlaidLinkProps) {
  const [linkToken, setLinkToken] = useState<string | null>(null)
  const [isLoadingToken, setIsLoadingToken] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const config = {
    token: linkToken,
    onSuccess,
    onExit,
    onEvent: (eventName: string, metadata: any) => {
      console.log('Plaid Link Event:', eventName, metadata)
    },
  }

  const { open, ready } = usePlaidLink(config)

  const fetchLinkToken = async () => {
    setIsLoadingToken(true)
    setError(null)
    try {
      const response = await createPlaidLinkToken()
      setLinkToken(response.link_token)
    } catch (error) {
      console.error('Failed to fetch Plaid link token:', error)
      setError('Failed to initialize bank connection. Please try again.')
    } finally {
      setIsLoadingToken(false)
    }
  }

  useEffect(() => {
    fetchLinkToken()
  }, [])

  const handleClick = () => {
    if (error) {
      fetchLinkToken()
      return
    }

    if (linkToken && ready && open) {
      open()
    }
  }

  const buttonDisabled = isLoading || isLoadingToken || !ready || !linkToken
  const buttonText = error
    ? 'Retry Connection'
    : isLoadingToken
      ? 'Initializing...'
      : isLoading
        ? 'Connecting...'
        : 'Connect Bank'

  return (
    <Button
      onClick={handleClick}
      disabled={buttonDisabled}
      className="flex items-center space-x-2"
    >
      <Plus className="h-4 w-4" />
      <span>{buttonText}</span>
    </Button>
  )
}
