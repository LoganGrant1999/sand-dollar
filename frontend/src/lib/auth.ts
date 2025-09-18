interface LoginRequest {
  email: string
  password: string
}

interface AuthResponse {
  accessToken: string
  tokenType: string
  expiresIn: number
  userInfo: {
    id: number
    email: string
    firstName: string
    lastName: string
  }
}

interface RegisterRequest {
  email: string
  password: string
  firstName: string
  lastName: string
}

const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api'
const TOKEN_KEY = 'auth_token'

class AuthClient {
  private token: string | null = null

  constructor() {
    this.token = localStorage.getItem(TOKEN_KEY)
  }

  async login(email: string, password: string): Promise<AuthResponse> {
    const response = await fetch(`${API_BASE_URL}/auth/login`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ email, password }),
    })

    if (!response.ok) {
      const error = await response.json()
      throw new Error(error.message || 'Login failed')
    }

    const authResponse: AuthResponse = await response.json()
    this.token = authResponse.accessToken
    localStorage.setItem(TOKEN_KEY, this.token)

    return authResponse
  }

  async register(data: RegisterRequest): Promise<{ message: string }> {
    const response = await fetch(`${API_BASE_URL}/auth/register`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(data),
    })

    if (!response.ok) {
      const error = await response.json()
      throw new Error(error.message || 'Registration failed')
    }

    return response.json()
  }

  async getCurrentUser(): Promise<AuthResponse['userInfo']> {
    if (!this.token) {
      throw new Error('No authentication token')
    }

    const response = await fetch(`${API_BASE_URL}/auth/me`, {
      headers: this.getAuthHeaders(),
    })

    if (!response.ok) {
      if (response.status === 401) {
        this.logout()
      }
      const error = await response.json()
      throw new Error(error.message || 'Failed to get user info')
    }

    return response.json()
  }

  logout(): void {
    this.token = null
    localStorage.removeItem(TOKEN_KEY)
  }

  getAuthHeaders(): Record<string, string> {
    const headers: Record<string, string> = {
      'Content-Type': 'application/json',
    }

    if (this.token) {
      headers['Authorization'] = `Bearer ${this.token}`
    }

    return headers
  }

  isAuthenticated(): boolean {
    return !!this.token
  }

  getToken(): string | null {
    return this.token
  }
}

export const authClient = new AuthClient()
export type { AuthResponse, LoginRequest, RegisterRequest }