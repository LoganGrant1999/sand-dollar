import React, { useState, useEffect } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { Progress } from '@/components/ui/progress'
import {
  Building2,
  CheckCircle,
  ArrowRight,
  Loader2,
  CreditCard,
  PiggyBank,
  TrendingUp,
  Sparkles
} from 'lucide-react'
import BankConnectionsCard from './BankConnectionsCard'
import { fetchPlaidStatus, triggerPlaidInitialSync } from '@/lib/api'
import toast from 'react-hot-toast'

interface OnboardingFlowProps {
  onComplete: () => void
}

export default function OnboardingFlow({ onComplete }: OnboardingFlowProps) {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const [currentStep, setCurrentStep] = useState(1)
  const [isFinalizingSetup, setIsFinalizingSetup] = useState(false)

  const plaidStatusQuery = useQuery({
    queryKey: ['plaid', 'status'],
    queryFn: fetchPlaidStatus,
    refetchInterval: 2000, // Poll every 2 seconds during onboarding
  })

  const connectedBanks = plaidStatusQuery.data?.items || []
  const hasConnections = connectedBanks.length > 0

  // Auto-advance to step 2 when banks are connected, but only if user hasn't manually navigated
  const [manualStepControl, setManualStepControl] = useState(false)

  useEffect(() => {
    if (hasConnections && currentStep === 1 && !manualStepControl) {
      setCurrentStep(2)
    }
  }, [hasConnections, currentStep, manualStepControl])

  const handleConnectionChange = () => {
    queryClient.invalidateQueries({ queryKey: ['plaid', 'status'] })
  }

  const handleFinishOnboarding = async () => {
    if (!hasConnections) {
      toast.error('Please connect at least one bank account to continue')
      return
    }

    setIsFinalizingSetup(true)
    try {
      // Ensure transactions are synced
      await triggerPlaidInitialSync()

      // Invalidate all relevant caches
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ['accounts'] }),
        queryClient.invalidateQueries({ queryKey: ['transactions'] }),
        queryClient.invalidateQueries({ queryKey: ['ai-budget', 'snapshot'] }),
        queryClient.invalidateQueries({ queryKey: ['budget', 'overview'] }),
        queryClient.invalidateQueries({ queryKey: ['balances'] }),
        queryClient.invalidateQueries({ queryKey: ['spending'] })
      ])

      toast.success('Setup complete! Welcome to Sand Dollar!')
      onComplete()
    } catch (error) {
      console.error('Failed to finalize setup:', error)
      toast.error('Failed to finalize setup. Please try again.')
    } finally {
      setIsFinalizingSetup(false)
    }
  }

  const handleAddMoreBanks = () => {
    setManualStepControl(true)
    setCurrentStep(1)
  }

  const handleContinueToReview = () => {
    setManualStepControl(true)
    setCurrentStep(2)
  }

  const steps = [
    {
      number: 1,
      title: 'Connect Your Banks',
      description: 'Securely link your bank accounts and credit cards',
      icon: Building2,
      completed: hasConnections
    },
    {
      number: 2,
      title: 'Review Connections',
      description: 'Verify your accounts and add more if needed',
      icon: CheckCircle,
      completed: false
    }
  ]

  const progress = hasConnections ? 100 : 50

  return (
    <div className="min-h-screen bg-[var(--color-background)] flex items-center justify-center p-4">
      <div className="w-full max-w-4xl space-y-8">
        {/* Header */}
        <div className="text-center space-y-4">
          <div className="flex items-center justify-center gap-3 mb-6">
            <Sparkles className="h-10 w-10 text-[var(--color-accent-teal)]" />
            <h1 className="text-4xl font-bold text-[var(--color-text-primary)]">
              Welcome to Sand Dollar
            </h1>
          </div>
          <p className="text-xl text-[var(--color-text-secondary)] max-w-2xl mx-auto">
            Let's get your finances organized! We'll start by connecting your bank accounts
            to give you a complete picture of your financial life.
          </p>
        </div>

        {/* Progress */}
        <div className="space-y-4">
          <div className="flex justify-between items-center">
            <span className="text-sm font-medium text-[var(--color-text-secondary)]">
              Setup Progress
            </span>
            <span className="text-sm font-medium text-[var(--color-text-secondary)]">
              {Math.round(progress)}% Complete
            </span>
          </div>
          <Progress value={progress} className="h-2" />
        </div>

        {/* Steps */}
        <div className="flex justify-center mb-8">
          <div className="flex items-center space-x-8">
            {steps.map((step, index) => (
              <div key={step.number} className="flex items-center">
                <div className="flex items-center space-x-3">
                  <div className={`
                    flex items-center justify-center w-10 h-10 rounded-full border-2 transition-colors
                    ${step.completed
                      ? 'bg-[var(--color-success)] border-[var(--color-success)] text-white'
                      : currentStep === step.number
                        ? 'border-[var(--color-accent-teal)] bg-[var(--color-accent-teal)] text-white'
                        : 'border-[var(--color-text-secondary)] text-[var(--color-text-secondary)]'
                    }
                  `}>
                    {step.completed ? (
                      <CheckCircle className="h-5 w-5" />
                    ) : (
                      <step.icon className="h-5 w-5" />
                    )}
                  </div>
                  <div className="text-left">
                    <div className={`font-medium ${
                      currentStep === step.number ? 'text-[var(--color-accent-teal)]' : 'text-[var(--color-text-primary)]'
                    }`}>
                      {step.title}
                    </div>
                    <div className="text-sm text-[var(--color-text-secondary)]">
                      {step.description}
                    </div>
                  </div>
                </div>
                {index < steps.length - 1 && (
                  <ArrowRight className="h-5 w-5 text-[var(--color-text-secondary)] mx-4" />
                )}
              </div>
            ))}
          </div>
        </div>

        {/* Step Content */}
        {currentStep === 1 && (
          <div className="space-y-6">
            <Card className="border-[var(--color-accent-teal)]/20">
              <CardHeader className="text-center">
                <CardTitle className="flex items-center justify-center gap-2">
                  <Building2 className="h-6 w-6 text-[var(--color-accent-teal)]" />
                  Connect Your Bank Accounts
                </CardTitle>
                <CardDescription>
                  Securely connect your checking, savings, and credit card accounts
                  to get started with automated expense tracking and budgeting.
                </CardDescription>
              </CardHeader>
              <CardContent>
                <BankConnectionsCard onConnectionChange={handleConnectionChange} />
              </CardContent>
            </Card>

            {/* Benefits */}
            <div className="grid md:grid-cols-3 gap-4">
              <Card className="text-center">
                <CardContent className="pt-6">
                  <CreditCard className="h-8 w-8 text-[var(--color-accent-blue)] mx-auto mb-3" />
                  <h3 className="font-semibold mb-2">Automatic Sync</h3>
                  <p className="text-sm text-[var(--color-text-secondary)]">
                    Your transactions update automatically so you always have current data
                  </p>
                </CardContent>
              </Card>
              <Card className="text-center">
                <CardContent className="pt-6">
                  <PiggyBank className="h-8 w-8 text-[var(--color-success)] mx-auto mb-3" />
                  <h3 className="font-semibold mb-2">Smart Budgeting</h3>
                  <p className="text-sm text-[var(--color-text-secondary)]">
                    AI-powered budget recommendations based on your spending patterns
                  </p>
                </CardContent>
              </Card>
              <Card className="text-center">
                <CardContent className="pt-6">
                  <TrendingUp className="h-8 w-8 text-[var(--color-accent-teal)] mx-auto mb-3" />
                  <h3 className="font-semibold mb-2">Spending Insights</h3>
                  <p className="text-sm text-[var(--color-text-secondary)]">
                    Detailed analytics to help you understand and optimize your finances
                  </p>
                </CardContent>
              </Card>
            </div>

            {/* Continue Button */}
            {hasConnections && (
              <div className="flex justify-center mt-6">
                <Button
                  onClick={handleContinueToReview}
                  className="flex items-center gap-2"
                  size="lg"
                >
                  Continue to Review
                  <ArrowRight className="h-4 w-4" />
                </Button>
              </div>
            )}
          </div>
        )}

        {currentStep === 2 && (
          <Card>
            <CardHeader className="text-center">
              <CardTitle className="flex items-center justify-center gap-2">
                <CheckCircle className="h-6 w-6 text-[var(--color-success)]" />
                Your Connected Accounts
              </CardTitle>
              <CardDescription>
                Great! You've successfully connected your accounts. Review them below
                and add more if needed, then finish setup to start using Sand Dollar.
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-6">
              {/* Connected Banks Summary */}
              <div className="space-y-3">
                {connectedBanks.map((bank: any) => (
                  <div
                    key={bank.id}
                    className="flex items-center justify-between p-4 bg-[var(--color-success)]/10 border border-[var(--color-success)]/20 rounded-lg"
                  >
                    <div className="flex items-center gap-3">
                      <CheckCircle className="h-5 w-5 text-[var(--color-success)]" />
                      <div>
                        <p className="font-medium text-[var(--color-text-primary)]">
                          {bank.institutionName || 'Unknown Bank'}
                        </p>
                        <p className="text-sm text-[var(--color-text-secondary)]">
                          Connected {new Date(bank.createdAt).toLocaleDateString()}
                        </p>
                      </div>
                    </div>
                    <Badge variant="outline" className="bg-[var(--color-success)]/10 text-[var(--color-success)] border-[var(--color-success)]/20">
                      {bank.status}
                    </Badge>
                  </div>
                ))}
              </div>

              {/* Actions */}
              <div className="flex justify-between pt-4">
                <Button
                  variant="outline"
                  onClick={handleAddMoreBanks}
                  className="flex items-center gap-2"
                >
                  <Building2 className="h-4 w-4" />
                  Add More Banks
                </Button>

                <Button
                  onClick={handleFinishOnboarding}
                  disabled={isFinalizingSetup}
                  className="flex items-center gap-2"
                >
                  {isFinalizingSetup ? (
                    <>
                      <Loader2 className="h-4 w-4 animate-spin" />
                      Setting Up Your Dashboard...
                    </>
                  ) : (
                    <>
                      Complete Setup
                      <ArrowRight className="h-4 w-4" />
                    </>
                  )}
                </Button>
              </div>
            </CardContent>
          </Card>
        )}
      </div>
    </div>
  )
}