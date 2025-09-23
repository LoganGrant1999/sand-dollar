import { createContext, useState, useEffect, type ReactNode } from 'react'
import { authClient } from '@/lib/auth'
import { fetchPlaidStatus } from '@/lib/api'
import toast from 'react-hot-toast'
import type { User, AuthContextType } from '@/types/auth'

const AuthContext = createContext<AuthContextType | undefined>(undefined)


export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [hasCompletedOnboarding, setHasCompletedOnboarding] = useState(false)

  const checkOnboardingStatus = async () => {
    try {
      const plaidStatus = await fetchPlaidStatus()
      const onboardingComplete = plaidStatus.hasItem && plaidStatus.items.length > 0
      setHasCompletedOnboarding(onboardingComplete)
      return onboardingComplete
    } catch (error) {
      setHasCompletedOnboarding(false)
      return false
    }
  }

  const checkAuth = async () => {
    try {
      if (!authClient.isAuthenticated()) {
        setUser(null)
        setHasCompletedOnboarding(false)
        setIsLoading(false)
        return
      }
      const userData = await authClient.getCurrentUser()
      setUser({
        id: userData.id,
        email: userData.email,
        firstName: userData.first_name,
        lastName: userData.last_name,
        role: 'user'
      })

      // Check onboarding status for authenticated users
      await checkOnboardingStatus()
    } catch (error: unknown) {
      setUser(null)
      setHasCompletedOnboarding(false)
    } finally {
      setIsLoading(false)
    }
  }

  const login = async (email: string, password: string): Promise<boolean> => {
    try {
      const response = await authClient.login(email, password)
      setUser({
        id: response.user.id,
        email: response.user.email,
        firstName: response.user.first_name,
        lastName: response.user.last_name,
        role: 'user'
      })

      // Check onboarding status after login
      await checkOnboardingStatus()

      toast.success('Login successful!')
      return true
    } catch (error: unknown) {
      const message = (error as Error)?.message || 'Login failed'
      toast.error(message)
      return false
    }
  }

  const register = async (email: string, password: string, firstName: string, lastName: string): Promise<boolean> => {
    try {
      await authClient.register({
        email,
        password,
        firstName,
        lastName
      })
      // New users haven't completed onboarding
      setHasCompletedOnboarding(false)
      toast.success('Registration successful!')
      return true
    } catch (error: unknown) {
      const message = (error as Error)?.message || 'Registration failed'
      toast.error(message)
      return false
    }
  }

  const completeOnboarding = async () => {
    setHasCompletedOnboarding(true)
  }

  const logout = async () => {
    try {
      await authClient.logout()
      setUser(null)
      setHasCompletedOnboarding(false)
      toast.success('Logged out successfully')
    } catch {
      setUser(null)
      setHasCompletedOnboarding(false)
      toast.error('Logout failed')
    }
  }

  useEffect(() => {
    checkAuth()
  }, [])

  const value = {
    user,
    isLoading,
    hasCompletedOnboarding,
    login,
    register,
    logout,
    checkAuth,
    completeOnboarding
  }

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  )
}

export { AuthContext }
