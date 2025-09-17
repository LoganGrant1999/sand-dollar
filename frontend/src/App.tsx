import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { ToastProvider } from './components/ui/toast-provider'
import Navbar from './components/Navbar'
import Dashboard from './pages/Dashboard'
import Budgeting from './pages/Budgeting'
import Spending from './pages/Spending'
import Assistant from './pages/Assistant'
import Settings from './pages/Settings'
import Login from './pages/Login'
import Register from './pages/Register'
import PlaidOauthReturn from './pages/PlaidOauthReturn'
import { AuthProvider } from './contexts/AuthProvider'
import { useAuth } from './hooks/useAuth'
import './App.css'

const queryClient = new QueryClient()

function ProtectedRoute({ children }: { children: React.ReactNode }) {
  const { user, isLoading } = useAuth()
  
  if (isLoading) {
    return <div className="flex items-center justify-center h-screen">
      <div className="h-32 w-32 animate-spin rounded-full border-b-2 border-[var(--color-accent-blue)]"></div>
    </div>
  }
  
  return user ? children : <Navigate to="/login" />
}

function PublicRoute({ children }: { children: React.ReactNode }) {
  const { user, isLoading } = useAuth()
  
  if (isLoading) {
    return <div className="flex items-center justify-center h-screen">
      <div className="h-32 w-32 animate-spin rounded-full border-b-2 border-[var(--color-accent-blue)]"></div>
    </div>
  }
  
  return user ? <Navigate to="/" /> : children
}

function AppRoutes() {
  return (
    <Router>
      <div className="min-h-screen bg-background">
        <Routes>
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
          <Route path="/*" element={
            <ProtectedRoute>
              <div className="flex flex-col">
                <Navbar />
                <main className="flex-1 container mx-auto px-4 py-6">
                  <Routes>
                    <Route path="/" element={<Dashboard />} />
                    <Route path="/budgeting/*" element={<Budgeting />} />
                    <Route path="/spending" element={<Spending />} />
                    <Route path="/assistant" element={<Assistant />} />
                    <Route path="/settings" element={<Settings />} />
                    <Route path="/plaid/oauth-return" element={<PlaidOauthReturn />} />
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
