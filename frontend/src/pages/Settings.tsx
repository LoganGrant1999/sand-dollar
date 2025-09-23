import { useState } from 'react'
import { useAuth } from '@/hooks/useAuth'
import { useQueryClient, useQuery } from '@tanstack/react-query'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import BankConnectionsCard from '@/components/BankConnectionsCard'
import { Settings as SettingsIcon } from 'lucide-react'

export default function Settings() {
  const { user } = useAuth()
  const queryClient = useQueryClient()

  const handleConnectionChange = () => {
    // Invalidate all relevant queries when bank connections change
    queryClient.invalidateQueries({ queryKey: ['ai-budget', 'snapshot'] })
    queryClient.invalidateQueries({ queryKey: ['accounts'] })
    queryClient.invalidateQueries({ queryKey: ['plaid-items'] })
    queryClient.invalidateQueries({ queryKey: ['transactions', 'recent'] })
    queryClient.invalidateQueries({ queryKey: ['balances'] })
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

      {/* Bank Connections */}
      <BankConnectionsCard onConnectionChange={handleConnectionChange} />

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
