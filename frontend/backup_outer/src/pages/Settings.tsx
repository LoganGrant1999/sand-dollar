import { useState } from 'react'
import { useAuth } from '@/hooks/useAuth'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import PlaidLink from '@/components/PlaidLink'
import { exchangePlaidPublicToken } from '@/lib/api'
import { Settings as SettingsIcon, CreditCard, Trash2, AlertCircle } from 'lucide-react'

export default function Settings() {
  const { user } = useAuth()
  const [isConnecting, setIsConnecting] = useState(false)

  // Mock connected accounts - in real app this would come from API
  const [connectedAccounts] = useState([
    {
      id: '1',
      name: 'Chase Checking',
      type: 'checking',
      mask: '1234',
      status: 'connected'
    },
    {
      id: '2', 
      name: 'Chase Credit Card',
      type: 'credit',
      mask: '5678',
      status: 'connected'
    }
  ])

  const handlePlaidSuccess = async (public_token: string, metadata: any) => {
    setIsConnecting(true)
    try {
      console.log('Plaid Success:', { public_token, metadata })
      await exchangePlaidPublicToken(public_token)
      alert('Bank account connected successfully!')
    } catch (error) {
      console.error('Error connecting bank:', error)
      alert('Failed to connect bank account. Please try again.')
    } finally {
      setIsConnecting(false)
    }
  }

  const handlePlaidExit = (err: any, metadata: any) => {
    console.log('Plaid Exit:', { err, metadata })
    setIsConnecting(false)
  }

  const handleDisconnectAccount = (accountId: string) => {
    // TODO: Implement account disconnection
    console.log('Disconnecting account:', accountId)
    alert('Account disconnection coming soon!')
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center space-x-3">
        <SettingsIcon className="h-8 w-8 text-[var(--color-accent-blue)]" />
        <div>
          <h1 className="text-2xl font-bold text-[var(--color-text-primary)]">Settings</h1>
          <p className="text-[var(--color-text-secondary)]">Manage your account and connected services</p>
        </div>
      </div>

      {/* Account Information */}
      <Card>
        <CardHeader>
          <CardTitle>Account Information</CardTitle>
          <CardDescription>Your profile details</CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="text-sm font-medium text-[var(--color-text-primary)]0">First Name</label>
              <p className="mt-1 text-[var(--color-text-primary)]">{user?.firstName}</p>
            </div>
            <div>
              <label className="text-sm font-medium text-[var(--color-text-primary)]0">Last Name</label>
              <p className="mt-1 text-[var(--color-text-primary)]">{user?.lastName}</p>
            </div>
          </div>
          <div>
            <label className="text-sm font-medium text-[var(--color-text-primary)]0">Email</label>
            <p className="mt-1 text-[var(--color-text-primary)]">{user?.email}</p>
          </div>
        </CardContent>
      </Card>

      {/* Connected Accounts */}
      <Card>
        <CardHeader>
          <div className="flex items-center justify-between">
            <div>
              <CardTitle className="flex items-center space-x-2">
                <CreditCard className="h-5 w-5" />
                <span>Connected Accounts</span>
              </CardTitle>
              <CardDescription>
                Connect your bank accounts and credit cards to automatically sync transactions
              </CardDescription>
            </div>
            <PlaidLink
              onSuccess={handlePlaidSuccess}
              onExit={handlePlaidExit}
              isLoading={isConnecting}
            />
          </div>
        </CardHeader>
        <CardContent>
          {connectedAccounts.length === 0 ? (
            <div className="text-center py-8">
              <CreditCard className="h-12 w-12 text-[var(--color-text-secondary)]/80 mx-auto mb-4" />
              <p className="text-[var(--color-text-secondary)] mb-4">No accounts connected yet</p>
              <PlaidLink
                onSuccess={handlePlaidSuccess}
                onExit={handlePlaidExit}
                isLoading={isConnecting}
              />
            </div>
          ) : (
            <div className="space-y-4">
              {connectedAccounts.map((account) => (
                <div key={account.id} className="flex items-center justify-between p-4 border border-[rgba(255,255,255,0.08)] rounded-lg">
                  <div className="flex items-center space-x-3">
                    <CreditCard className="h-5 w-5 text-[var(--color-text-secondary)]" />
                    <div>
                      <p className="font-medium text-[var(--color-text-primary)]">{account.name}</p>
                      <p className="text-sm text-[var(--color-text-secondary)]">
                        {account.type === 'checking' ? 'Checking' : 'Credit Card'} •••• {account.mask}
                      </p>
                    </div>
                  </div>
                  <div className="flex items-center space-x-2">
                    <Badge variant="secondary" className="bg-[var(--color-success)]/15 text-[var(--color-success)]">
                      Connected
                    </Badge>
                    <Button
                      variant="ghost"
                      size="sm"
                      onClick={() => handleDisconnectAccount(account.id)}
                      className="text-[var(--color-error)] hover:text-[var(--color-error)]/80"
                    >
                      <Trash2 className="h-4 w-4" />
                    </Button>
                  </div>
                </div>
              ))}
              <div className="flex items-start space-x-2 rounded-lg bg-[var(--color-accent-blue)]/15 p-4">
                <AlertCircle className="mt-0.5 h-5 w-5 flex-shrink-0 text-[var(--color-accent-blue)]" />
                <div className="text-sm text-[var(--color-accent-blue)]">
                  <p className="font-medium">Demo Mode Active</p>
                  <p>These are sample accounts. In the full version, you'll connect your real bank accounts securely through Plaid.</p>
                </div>
              </div>
            </div>
          )}
        </CardContent>
      </Card>

      {/* Security */}
      <Card>
        <CardHeader>
          <CardTitle>Security</CardTitle>
          <CardDescription>Manage your account security settings</CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="flex items-center justify-between">
            <div>
              <p className="font-medium text-[var(--color-text-primary)]">Password</p>
              <p className="text-sm text-[var(--color-text-secondary)]">Change your account password</p>
            </div>
            <Button variant="outline" disabled>
              Change Password
            </Button>
          </div>
          <div className="flex items-center justify-between">
            <div>
              <p className="font-medium text-[var(--color-text-primary)]">Two-Factor Authentication</p>
              <p className="text-sm text-[var(--color-text-secondary)]">Add an extra layer of security</p>
            </div>
            <Button variant="outline" disabled>
              Enable 2FA
            </Button>
          </div>
        </CardContent>
      </Card>
    </div>
  )
}
