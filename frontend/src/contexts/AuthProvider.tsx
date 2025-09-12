import { createContext, useState, useEffect, type ReactNode } from 'react'
import { api } from '@/lib/api'
import toast from 'react-hot-toast'
import type { User, AuthContextType } from '@/types/auth'

const AuthContext = createContext<AuthContextType | undefined>(undefined)

interface ErrorResponse {
  response?: {
    data?: {
      message?: string
    }
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null)
  const [isLoading, setIsLoading] = useState(true)

  const checkAuth = async () => {
    try {
      const response = await api.get('/auth/me')
      setUser(response.data)
    } catch {
      setUser(null)
    } finally {
      setIsLoading(false)
    }
  }

  const login = async (email: string, password: string): Promise<boolean> => {
    try {
      const response = await api.post('/auth/login', { email, password })
      // Backend returns AuthResponse with user field
      if (response.data.user) {
        setUser(response.data.user)
      } else {
        setUser(response.data)
      }
      toast.success('Login successful!')
      return true
    } catch (error: unknown) {
      const message = (error as ErrorResponse)?.response?.data?.message || 'Login failed'
      toast.error(message)
      return false
    }
  }

  const register = async (email: string, password: string, firstName: string, lastName: string): Promise<boolean> => {
    try {
      await api.post('/auth/register', {
        email,
        password,
        first_name: firstName,
        last_name: lastName
      })
      toast.success('Registration successful!')
      return true
    } catch (error: unknown) {
      const message = (error as ErrorResponse)?.response?.data?.message || 'Registration failed'
      toast.error(message)
      return false
    }
  }

  const logout = async () => {
    try {
      await api.post('/auth/logout')
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