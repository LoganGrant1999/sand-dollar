import { useEffect } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'

export default function PlaidOauthReturn() {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()

  useEffect(() => {
    const publicToken = searchParams.get('public_token')
    const error = searchParams.get('error')
    const errorDescription = searchParams.get('error_description')

    if (error) {
      console.error('Plaid OAuth error:', error, errorDescription)
      navigate('/settings?plaid_error=' + encodeURIComponent(error))
      return
    }

    if (publicToken) {
      console.log('Received Plaid public token:', publicToken)
      navigate('/settings?public_token=' + encodeURIComponent(publicToken))
      return
    }

    navigate('/settings')
  }, [searchParams, navigate])

  return (
    <div className="flex items-center justify-center min-h-[50vh]">
      <div className="text-center">
        <div className="h-12 w-12 animate-spin rounded-full border-b-2 border-[var(--color-accent-blue)] mx-auto mb-4"></div>
        <h2 className="text-lg font-semibold text-[var(--color-text-primary)] mb-2">
          Processing Plaid Connection
        </h2>
        <p className="text-[var(--color-text-secondary)]">
          Please wait while we complete your bank account connection...
        </p>
      </div>
    </div>
  )
}