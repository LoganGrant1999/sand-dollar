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
  const [isCheckingOnboarding, setIsCheckingOnboarding] = useState(false)

  const checkOnboardingStatus = async () => {
    setIsCheckingOnboarding(true)
    try {
      const plaidStatus = await fetchPlaidStatus()
      const onboardingComplete = plaidStatus.hasItem && plaidStatus.items.length > 0
      setHasCompletedOnboarding(onboardingComplete)
      return onboardingComplete
    } catch (error) {
      setHasCompletedOnboarding(false)
      return false
    } finally {
      setIsCheckingOnboarding(false)
    }
  }

  const checkAuth = async () => {
    console.log('🔍 AuthProvider: Starting auth check...')
    try {
      // Check if any token exists (localStorage or cookie)
      const token = authClient.getToken()
      console.log('🔍 AuthProvider: Token found:', !!token)

      if (!token) {
        console.log('🔍 AuthProvider: No token found, setting user to null')
        setUser(null)
        setHasCompletedOnboarding(false)
        return
      }

      // Try to get current user data
      try {
        // Check if we have OAuth token vs regular token
        const localToken = localStorage.getItem('sd_auth_token')
        const userData = localToken
          ? await authClient.getCurrentUser()  // Regular JWT in localStorage
          : await authClient.getOAuthCurrentUser()  // OAuth2 cookie

        setUser({
          id: userData.id,
          email: userData.email,
          firstName: userData.firstName,
          lastName: userData.lastName,
          role: 'user'
        })
        
        // Check onboarding status for authenticated users
        await checkOnboardingStatus()
      } catch (error) {
        console.log('🔍 AuthProvider: Failed to get user data, trying token refresh...')
        
        // Try to refresh token
        const refreshResult = await authClient.refreshToken()
        
        if (refreshResult) {
          // Retry getting user data after refresh
          try {
            const localToken = localStorage.getItem('sd_auth_token')
            const userData = localToken
              ? await authClient.getCurrentUser()
              : await authClient.getOAuthCurrentUser()

            setUser({
              id: userData.id,
              email: userData.email,
              firstName: userData.firstName,
              lastName: userData.lastName,
              role: 'user'
            })
            
            await checkOnboardingStatus()
          } catch {
            // Still failed after refresh, clear tokens
            authClient.clearToken()
            setUser(null)
            setHasCompletedOnboarding(false)
          }
        } else {
          // Refresh failed, clear tokens
          authClient.clearToken()
          setUser(null)
          setHasCompletedOnboarding(false)
        }
      }
    } catch (error: unknown) {
      console.log('🔍 AuthProvider: Auth check error:', error)
      authClient.clearToken()
      setUser(null)
      setHasCompletedOnboarding(false)
    } finally {
      console.log('🔍 AuthProvider: Auth check complete, setting loading to false')
      setIsLoading(false)
    }
  }

  const login = async (email: string, password: string): Promise<boolean> => {
    try {
      const response = await authClient.login(email, password)
      // Token is already stored by authClient.login()
      setUser({
        id: response.user.id,
        email: response.user.email,
        firstName: response.user.firstName,
        lastName: response.user.lastName,
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
      // Token is already cleared by authClient.logout()
      setUser(null)
      setHasCompletedOnboarding(false)
      toast.success('Logged out successfully')
    } catch {
      // Clear token even if logout request fails
      authClient.clearToken()
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
    isCheckingOnboarding,
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
