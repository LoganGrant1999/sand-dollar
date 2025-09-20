interface LoginRequest {
  email: string
  password: string
}

interface AuthResponse {
  access_token: string
  token_type: string
  expires_in: number
  user: {
    id: number
    email: string
    first_name: string
    last_name: string
  }
}

interface RegisterRequest {
  email: string
  password: string
  firstName: string
  lastName: string
}

const API_BASE_URL = import.meta.env.VITE_API_BASE || '/api'

class AuthClient {
  async login(email: string, password: string): Promise<AuthResponse> {
    const response = await fetch(`${API_BASE_URL}/auth/login`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      credentials: 'include', // Include cookies
      body: JSON.stringify({ email, password }),
    })

    if (!response.ok) {
      // Safely handle error response
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
    return authResponse
  }

  async register(data: RegisterRequest): Promise<{ message: string }> {
    const response = await fetch(`${API_BASE_URL}/auth/register`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      credentials: 'include', // Include cookies
      body: JSON.stringify(data),
    })

    if (!response.ok) {
      // Safely handle error response
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
      credentials: 'include', // Include cookies
    })

    if (!response.ok) {
      if (response.status === 401) {
        this.logout()
      }

      // Safely handle error response
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

  async logout(): Promise<void> {
    await fetch(`${API_BASE_URL}/auth/logout`, {
      method: 'POST',
      credentials: 'include',
    })
  }

  isAuthenticated(): boolean {
    // For cookie-based auth, we can't reliably determine this locally
    // Return true to allow AuthProvider to check with server
    // The server check will handle unauthenticated cases properly
    return true
  }
}

export const authClient = new AuthClient()
export type { AuthResponse, LoginRequest, RegisterRequest }