export interface User {
  id: number
  email: string
  firstName: string
  lastName: string
  role: string
  hasCompletedOnboarding?: boolean
}

export interface AuthContextType {
  user: User | null
  isLoading: boolean
  hasCompletedOnboarding: boolean
  login: (email: string, password: string) => Promise<boolean>
  register: (email: string, password: string, firstName: string, lastName: string) => Promise<boolean>
  logout: () => Promise<void>
  checkAuth: () => Promise<void>
  completeOnboarding: () => Promise<void>
}