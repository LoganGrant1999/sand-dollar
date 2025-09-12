import { useState } from 'react'
import { useAuth } from '@/hooks/useAuth'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import PlaidLink from '@/components/PlaidLink'
import { Settings as SettingsIcon, CreditCard, Plus, Trash2, AlertCircle } from 'lucide-react'

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
      // TODO: Send public_token to backend to exchange for access_token
      // const response = await fetch('/api/plaid/exchange_public_token', {
      //   method: 'POST',
      //   headers: { 'Content-Type': 'application/json' },
      //   body: JSON.stringify({ public_token })
      // })
      alert('Bank account connected successfully! (Demo mode)')
    } catch (error) {
      console.error('Error connecting bank:', error)
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
        <SettingsIcon className="h-8 w-8 text-blue-600" />
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Settings</h1>
          <p className="text-gray-600">Manage your account and connected services</p>
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
              <label className="text-sm font-medium text-gray-700">First Name</label>
              <p className="mt-1 text-gray-900">{user?.firstName}</p>
            </div>
            <div>
              <label className="text-sm font-medium text-gray-700">Last Name</label>
              <p className="mt-1 text-gray-900">{user?.lastName}</p>
            </div>
          </div>
          <div>
            <label className="text-sm font-medium text-gray-700">Email</label>
            <p className="mt-1 text-gray-900">{user?.email}</p>
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
              <CreditCard className="h-12 w-12 text-gray-400 mx-auto mb-4" />
              <p className="text-gray-500 mb-4">No accounts connected yet</p>
              <PlaidLink
                onSuccess={handlePlaidSuccess}
                onExit={handlePlaidExit}
                isLoading={isConnecting}
              />
            </div>
          ) : (
            <div className="space-y-4">
              {connectedAccounts.map((account) => (
                <div key={account.id} className="flex items-center justify-between p-4 border border-gray-200 rounded-lg">
                  <div className="flex items-center space-x-3">
                    <CreditCard className="h-5 w-5 text-gray-500" />
                    <div>
                      <p className="font-medium text-gray-900">{account.name}</p>
                      <p className="text-sm text-gray-500">
                        {account.type === 'checking' ? 'Checking' : 'Credit Card'} •••• {account.mask}
                      </p>
                    </div>
                  </div>
                  <div className="flex items-center space-x-2">
                    <Badge variant="secondary" className="bg-green-100 text-green-800">
                      Connected
                    </Badge>
                    <Button
                      variant="ghost"
                      size="sm"
                      onClick={() => handleDisconnectAccount(account.id)}
                      className="text-red-600 hover:text-red-700"
                    >
                      <Trash2 className="h-4 w-4" />
                    </Button>
                  </div>
                </div>
              ))}
              <div className="flex items-start space-x-2 p-4 bg-blue-50 rounded-lg">
                <AlertCircle className="h-5 w-5 text-blue-600 flex-shrink-0 mt-0.5" />
                <div className="text-sm text-blue-800">
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
              <p className="font-medium text-gray-900">Password</p>
              <p className="text-sm text-gray-500">Change your account password</p>
            </div>
            <Button variant="outline" disabled>
              Change Password
            </Button>
          </div>
          <div className="flex items-center justify-between">
            <div>
              <p className="font-medium text-gray-900">Two-Factor Authentication</p>
              <p className="text-sm text-gray-500">Add an extra layer of security</p>
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