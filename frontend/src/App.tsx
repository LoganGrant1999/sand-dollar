import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { ToastProvider } from './components/ui/toast-provider'
import Navbar from './components/Navbar'
import Landing from './pages/Landing'
import Goals from './pages/Goals.tsx'
import Plan from './pages/Plan.tsx'
import Budget from './pages/Budget.tsx'
import Spending from './pages/Spending'
import Settings from './pages/Settings'
import Login from './pages/Login'
import Register from './pages/Register'
import OAuthTest from './pages/OAuthTest'
import OAuthSuccess from './pages/OAuthSuccess'
import PlaidOauthReturn from './pages/PlaidOauthReturn'
import OnboardingFlow from './components/OnboardingFlow'
import { AuthProvider } from './contexts/AuthProvider'
import { useAuth } from './hooks/useAuth'
import './App.css'

const queryClient = new QueryClient()

function ProtectedRoute({ children }: { children: React.ReactNode }) {
  const { user, isLoading, hasCompletedOnboarding, isCheckingOnboarding, completeOnboarding } = useAuth()

  console.log('🛡️ ProtectedRoute: isLoading:', isLoading, 'isCheckingOnboarding:', isCheckingOnboarding, 'user:', !!user)

  if (isLoading || isCheckingOnboarding) {
    return <div className="flex items-center justify-center h-screen">
      <div className="h-32 w-32 animate-spin rounded-full border-b-2 border-secondary"></div>
    </div>
  }

  if (!user) {
    return <Navigate to="/login" />
  }

  if (!hasCompletedOnboarding) {
    return <OnboardingFlow onComplete={completeOnboarding} />
  }

  return children
}

function PublicRoute({ children }: { children: React.ReactNode }) {
  const { user, isLoading } = useAuth()

  console.log('🌐 PublicRoute: isLoading:', isLoading, 'user:', !!user)

  if (isLoading) {
    return <div className="flex items-center justify-center h-screen">
      <div className="h-32 w-32 animate-spin rounded-full border-b-2 border-secondary"></div>
    </div>
  }

  // Only redirect if we have a verified user
  if (user) {
    return <Navigate to="/app" replace />
  }

  return children
}

function AppRoutes() {
  return (
    <Router>
      <div className="min-h-screen bg-[var(--color-bg-dark)]">
        <Routes>
          <Route path="/" element={
            <PublicRoute>
              <Landing />
            </PublicRoute>
          } />
          <Route path="/login" element={
            <PublicRoute>
              <Login />
            </PublicRoute>
          } />
          <Route path="/register" element={
            <PublicRoute>
              <Register />
            </PublicRoute>
          } />
          <Route path="/oauth-test" element={<OAuthTest />} />
          <Route path="/oauth-success" element={<OAuthSuccess />} />
          <Route path="/app/*" element={
            <ProtectedRoute>
              <div className="flex flex-col">
                <Navbar />
                <main className="flex-1 container mx-auto px-4 py-6">
                  <Routes>
                    <Route path="goals" element={<Goals />} />
                    <Route path="plan" element={<Plan />} />
                    <Route path="budget" element={<Budget />} />
                    <Route path="spending" element={<Spending />} />
                    <Route path="settings" element={<Settings />} />
                    <Route path="plaid/oauth-return" element={<PlaidOauthReturn />} />
                    <Route path="" element={<Navigate to="goals" replace />} />
                    <Route path="*" element={<Navigate to="goals" replace />} />
                  </Routes>
                </main>
              </div>
            </ProtectedRoute>
          } />
        </Routes>
      </div>
      <ToastProvider />
    </Router>
  )
}

function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <AuthProvider>
        <AppRoutes />
      </AuthProvider>
    </QueryClientProvider>
  )
}

export default App
