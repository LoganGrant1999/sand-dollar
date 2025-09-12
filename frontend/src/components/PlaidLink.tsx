import { usePlaidLink } from 'react-plaid-link'
import { Button } from '@/components/ui/button'
import { Plus } from 'lucide-react'

interface PlaidLinkProps {
  onSuccess: (public_token: string, metadata: any) => void
  onExit?: (err: any, metadata: any) => void
  isLoading?: boolean
}

export default function PlaidLink({ onSuccess, onExit, isLoading = false }: PlaidLinkProps) {
  const config = {
    token: null, // We'll need to get a link_token from the backend
    onSuccess,
    onExit,
    onEvent: (eventName: string, metadata: any) => {
      console.log('Plaid Link Event:', eventName, metadata)
    },
  }

  const { open, ready } = usePlaidLink(config)

  const handleClick = () => {
    // For now, we'll show a demo message since we need backend integration
    alert('Demo: This would open Plaid Link to connect your bank account securely. Backend integration needed for production.')
    
    // In production, this would call open() after getting a link_token
    // open()
  }

  return (
    <Button 
      onClick={handleClick}
      disabled={isLoading || !ready}
      className="flex items-center space-x-2"
    >
      <Plus className="h-4 w-4" />
      <span>{isLoading ? 'Connecting...' : 'Connect Bank'}</span>
    </Button>
  )
}