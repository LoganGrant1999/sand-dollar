import { createContext, useState, useEffect, type ReactNode } from 'react'
import { authClient } from '@/lib/auth'
import toast from 'react-hot-toast'
import type { User, AuthContextType } from '@/types/auth'

const AuthContext = createContext<AuthContextType | undefined>(undefined)


export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null)
  const [isLoading, setIsLoading] = useState(true)

  const checkAuth = async () => {
    try {
      if (!authClient.isAuthenticated()) {
        setUser(null)
        setIsLoading(false)
        return
      }
      const userData = await authClient.getCurrentUser()
      setUser(userData)
    } catch (error: unknown) {
      setUser(null)
    } finally {
      setIsLoading(false)
    }
  }

  const login = async (email: string, password: string): Promise<boolean> => {
    try {
      const response = await authClient.login(email, password)
      setUser(response.userInfo)
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
      toast.success('Registration successful!')
      return true
    } catch (error: unknown) {
      const message = (error as Error)?.message || 'Registration failed'
      toast.error(message)
      return false
    }
  }

  const logout = async () => {
    try {
      authClient.logout()
      setUser(null)
      toast.success('Logged out successfully')
    } catch {
      setUser(null)
      toast.error('Logout failed')
    }
  }

  useEffect(() => {
    checkAuth()
  }, [])

  const value = {
    user,
    isLoading,
    login,
    register,
    logout,
    checkAuth
  }

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  )
}

export { AuthContext }
