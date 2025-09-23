import React from 'react'
import { Link, useLocation } from 'react-router-dom'
import { useAuth } from '@/hooks/useAuth'
import { Button } from '@/components/ui/button'
import { 
  Home, 
  Calculator, 
  TrendingUp, 
  Bot, 
  Settings,
  LogOut,
 
} from 'lucide-react'
import circleLogo from '@/assets/circle_logo.png'

export default function Navbar() {
  const { user, logout } = useAuth()
  const location = useLocation()

  const navItems = [
    { path: '/', label: 'Dashboard', icon: Home },
    { path: '/budgeting', label: 'Budgeting', icon: Calculator },
    { path: '/spending', label: 'Spending', icon: TrendingUp },
    { path: '/assistant', label: 'Assistant', icon: Bot },
    { path: '/settings', label: 'Settings', icon: Settings }
  ]

  const handleLogout = async () => {
    await logout()
  }

  return (
    <>
      <nav className="border-b border-[rgba(255,255,255,0.08)] bg-[var(--color-panel-dark)] shadow-[0px_2px_10px_rgba(0,0,0,0.4)]">
        <div className="container mx-auto px-4">
          <div className="flex justify-between items-center h-16">
            <div className="flex items-center space-x-8">
              <Link to="/" className="flex items-center space-x-2">
                <img 
                  src={circleLogo} 
                  alt="Sand Dollar Logo" 
                  className="h-14 w-14 rounded-full object-cover object-center"
                />
                <span className="gradient-text text-xl font-bold">Sand Dollar</span>
              </Link>
              
              <div className="flex space-x-1">
                {navItems.map(({ path, label, icon: Icon }) => {
                  const isActive = location.pathname === path
                  return (
                    <Link
                      key={path}
                      to={path}
                      className={`flex items-center space-x-2 rounded-lg px-3 py-2 text-sm font-medium transition-colors ${
                        isActive
                          ? 'border border-[var(--color-accent-teal)] bg-[var(--color-accent-teal)]/20 text-[var(--color-text-primary)] shadow-[0px_2px_10px_rgba(0,0,0,0.4)]'
                          : 'text-[var(--color-text-secondary)] hover:text-[var(--color-text-primary)] hover:bg-[var(--color-accent-teal)]/10'
                      }`}
                    >
                      <Icon size={18} />
                      <span>{label}</span>
                    </Link>
                  )
                })}
              </div>
            </div>

            <div className="flex items-center space-x-4">
              <span className="text-sm text-[var(--color-text-secondary)]">
                Welcome, {user?.firstName}
              </span>
              <Button
                variant="ghost"
                size="sm"
                onClick={handleLogout}
                className="flex items-center space-x-1"
              >
                <LogOut size={16} />
                <span>Logout</span>
              </Button>
            </div>
          </div>
        </div>
      </nav>
    </>
  )
}
