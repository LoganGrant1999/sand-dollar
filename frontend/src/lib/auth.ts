interface LoginRequest {
  email: string
  password: string
}

interface AuthResponse {
  token: string
  user: {
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

const API_BASE_URL = import.meta.env.VITE_API_BASE || '/api'
const TOKEN_KEY = 'sd_auth_token'

class AuthClient {
  private getAuthHeaders(): Record<string, string> {
    const token = this.getToken()
    return token ? { Authorization: `Bearer ${token}` } : {}
  }

  getToken(): string | null {
    // First check localStorage (for regular login)
    const localToken = localStorage.getItem(TOKEN_KEY)
    if (localToken) {
      return localToken
    }

    // Then check for OAuth cookie
    return this.getOAuthTokenFromCookie()
  }

  private getOAuthTokenFromCookie(): string | null {
    const cookies = document.cookie.split(';')
    for (let cookie of cookies) {
      const [name, value] = cookie.trim().split('=')
      if (name === TOKEN_KEY) {
        return value
      }
    }
    return null
  }

  setToken(token: string): void {
    localStorage.setItem(TOKEN_KEY, token)
  }

  clearToken(): void {
    localStorage.removeItem(TOKEN_KEY)
    this.clearOAuthToken()
  }

  private clearOAuthToken(): void {
    // Clear OAuth cookie by setting it to expired
    document.cookie = `${TOKEN_KEY}=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/;`
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
      let errorMessage = 'Login failed'
      try {
        const contentType = response.headers.get('content-type')
        if (contentType && contentType.includes('application/json')) {
          const error = await response.json()
          errorMessage = error.message || errorMessage
        }
      } catch {
        // If we can't parse JSON, use default message
      }
      throw new Error(errorMessage)
    }

    const authResponse: AuthResponse = await response.json()
    this.setToken(authResponse.token)
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
      let errorMessage = 'Registration failed'
      try {
        const contentType = response.headers.get('content-type')
        if (contentType && contentType.includes('application/json')) {
          const error = await response.json()
          errorMessage = error.message || errorMessage
        }
      } catch {
        // If we can't parse JSON, use default message
      }
      throw new Error(errorMessage)
    }

    return response.json()
  }

  async getCurrentUser(): Promise<AuthResponse['user']> {
    const response = await fetch(`${API_BASE_URL}/auth/me`, {
      headers: this.getAuthHeaders(),
      credentials: 'include',
    })

    if (!response.ok) {
      if (response.status === 401) {
        this.clearToken()
      }

      let errorMessage = 'Failed to get user info'
      try {
        const contentType = response.headers.get('content-type')
        if (contentType && contentType.includes('application/json')) {
          const error = await response.json()
          errorMessage = error.message || errorMessage
        }
      } catch {
        // If we can't parse JSON, use default message
      }
      throw new Error(errorMessage)
    }

    return response.json()
  }

  async getOAuthCurrentUser(): Promise<AuthResponse['user']> {
    const response = await fetch(`${API_BASE_URL}/auth/oauth2/check`, {
      credentials: 'include',
    })

    if (!response.ok) {
      if (response.status === 401) {
        this.clearOAuthToken()
      }

      let errorMessage = 'Failed to get OAuth user info'
      try {
        const contentType = response.headers.get('content-type')
        if (contentType && contentType.includes('application/json')) {
          const error = await response.json()
          errorMessage = error.message || errorMessage
        }
      } catch {
        // If we can't parse JSON, use default message
      }
      throw new Error(errorMessage)
    }

    return response.json()
  }

  async refreshToken(): Promise<{ token: string } | null> {
    try {
      // Check if we have an OAuth token in cookie
      const oauthToken = this.getOAuthTokenFromCookie()
      if (oauthToken) {
        return this.refreshOAuthToken()
      }

      const response = await fetch(`${API_BASE_URL}/auth/refresh`, {
        method: 'GET',
        credentials: 'include',
      })

      if (!response.ok) {
        if (response.status === 401) {
          this.clearToken()
        }
        return null
      }

      const data = await response.json()
      if (data.token) {
        this.setToken(data.token)
        return data
      }

      return null
    } catch {
      this.clearToken()
      return null
    }
  }

  async refreshOAuthToken(): Promise<{ token: string } | null> {
    try {
      const response = await fetch(`${API_BASE_URL}/auth/oauth2/refresh`, {
        method: 'POST',
        credentials: 'include',
      })

      if (!response.ok) {
        if (response.status === 401) {
          this.clearOAuthToken()
        }
        return null
      }

      // OAuth refresh updates the cookie, no need to handle response
      return { token: 'oauth-refreshed' }
    } catch {
      this.clearOAuthToken()
      return null
    }
  }

  async logout(): Promise<void> {
    try {
      // Check if we have an OAuth token in cookie
      const oauthToken = this.getOAuthTokenFromCookie()
      if (oauthToken) {
        await fetch(`${API_BASE_URL}/auth/oauth2/logout`, {
          method: 'POST',
          credentials: 'include',
        })
      } else {
        await fetch(`${API_BASE_URL}/auth/logout`, {
          method: 'POST',
          headers: this.getAuthHeaders(),
        })
      }
    } finally {
      this.clearToken()
    }
  }

  isAuthenticated(): boolean {
    return !!this.getToken()
  }
}

export const authClient = new AuthClient()
export type { AuthResponse, LoginRequest, RegisterRequest }