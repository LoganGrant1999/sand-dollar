import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '@/hooks/useAuth'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Eye, EyeOff, LogIn } from 'lucide-react'
import circleLogo from '@/assets/circle_logo.png'

export default function Login() {
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const [isLoading, setIsLoading] = useState(false)
  const [error, setError] = useState('')

  const { login } = useAuth()
  const navigate = useNavigate()

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError('')
    
    if (!email || !password) {
      setError('Please fill in all fields')
      return
    }

    setIsLoading(true)
    try {
      const success = await login(email, password)
      if (success) {
        navigate('/')
      }
    } catch {
      setError('Login failed. Please try again.')
    } finally {
      setIsLoading(false)
    }
  }

  return (
    <div className="flex min-h-screen flex-col justify-center bg-[var(--color-bg-dark)] pt-8 pb-4 sm:px-6 lg:px-8">
      <div className="sm:mx-auto sm:w-full sm:max-w-md">
        <div className="flex justify-center items-center">
          <img 
            src={circleLogo} 
            alt="Sand Dollar Logo" 
            className="h-80 w-80 rounded-full object-cover object-center"
          />
        </div>
        <h2 className="-mt-18 text-center text-3xl font-bold text-[var(--color-text-primary)]">
          Welcome back to Sand Dollar
        </h2>
        <p className="mt-2 text-center text-sm text-[var(--color-text-secondary)]">
          Don't have an account?{' '}
          <Link to="/register" className="font-medium text-[var(--color-accent-blue)] transition-colors hover:text-[var(--color-accent-teal)]">
            Sign up here
          </Link>
        </p>
      </div>

      <div className="mt-4 sm:mx-auto sm:w-full sm:max-w-md">
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center space-x-2">
              <LogIn className="h-5 w-5" />
              <span>Sign in to your account</span>
            </CardTitle>
            <CardDescription>
              Enter your credentials to access your financial dashboard
            </CardDescription>
          </CardHeader>
          <CardContent>
            <form onSubmit={handleSubmit} className="space-y-6">
              {error && (
                <div className="rounded-[1rem] border border-[var(--color-error)]/60 bg-[var(--color-error)]/10 px-4 py-3 text-sm text-[var(--color-error)]">
                  {error}
                </div>
              )}

              <div>
                <Label htmlFor="email">Email address</Label>
                <Input
                  id="email"
                  type="email"
                  autoComplete="email"
                  required
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  className="mt-2"
                  placeholder="Enter your email"
                />
              </div>

              <div>
                <Label htmlFor="password">Password</Label>
                <div className="relative mt-1">
                  <Input
                    id="password"
                    type={showPassword ? 'text' : 'password'}
                    autoComplete="current-password"
                    required
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    className="mt-2 pr-12"
                    placeholder="Enter your password"
                  />
                  <button
                    type="button"
                    className="absolute inset-y-0 right-0 flex items-center pr-3 text-[var(--color-text-secondary)] transition-colors hover:text-[var(--color-text-primary)]"
                    onClick={() => setShowPassword(!showPassword)}
                  >
                    {showPassword ? (
                      <EyeOff className="h-4 w-4" />
                    ) : (
                      <Eye className="h-4 w-4" />
                    )}
                  </button>
                </div>
              </div>

              <Button 
                type="submit" 
                className="w-full"
                disabled={isLoading}
              >
                {isLoading ? 'Signing in...' : 'Sign in'}
              </Button>
            </form>

            <div className="mt-6">
              <div className="relative">
                <div className="absolute inset-0 flex items-center">
                  <div className="w-full border-t border-[rgba(255,255,255,0.08)]" />
                </div>
                <div className="relative flex justify-center text-sm">
                  <span className="bg-[var(--color-panel-dark)] px-2 text-[var(--color-text-secondary)]">Demo Account</span>
                </div>
              </div>

              <div className="mt-4 text-center">
                <p className="text-sm text-[var(--color-text-secondary)]">
                  Try the demo: <strong>demo@sanddollar.app</strong> / <strong>demopassword</strong>
                </p>
                <Button
                  variant="outline"
                  className="mt-2"
                  onClick={() => {
                    setEmail('demo@sanddollar.app')
                    setPassword('demopassword')
                  }}
                >
                  Fill Demo Credentials
                </Button>
              </div>
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  )
}
