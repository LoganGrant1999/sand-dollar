import React from 'react'
import { Link, useLocation } from 'react-router-dom'
import { useAuth } from '@/hooks/useAuth'
import { Button } from '@/components/ui/button'
import {
  Target,
  MapPin,
  Calendar,
  TrendingUp,
  Settings,
  LogOut
} from 'lucide-react'
import circleLogo from '@/assets/circle_logo.png'
import { isMvpFocus } from '../utils/featureFlags'

export default function Navbar() {
  const { user, logout } = useAuth()
  const location = useLocation()

  const allNavItems = [
    { path: '/app/goals', label: 'Goals', icon: Target, mvp: true },
    { path: '/app/plan', label: 'Plan', icon: MapPin, mvp: true },
    { path: '/app/budget', label: 'Budget', icon: Calendar, mvp: true },
    { path: '/app/spending', label: 'Spending', icon: TrendingUp, mvp: false },
    { path: '/app/settings', label: 'Settings', icon: Settings, mvp: true }
  ]

  const navItems = isMvpFocus()
    ? allNavItems.filter(item => item.mvp)
    : allNavItems

  const handleLogout = async () => {
    await logout()
  }

  return (
    <>
      <nav className="border-b border-border bg-card shadow-lg">
        <div className="container mx-auto px-4">
          <div className="flex justify-between items-center h-16">
            <div className="flex items-center space-x-8">
              <Link to="/app/goals" className="flex items-center space-x-2">
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
                          ? 'border border-primary bg-primary/20 text-foreground shadow-glow-primary'
                          : 'text-muted-foreground hover:text-foreground hover:bg-primary/10'
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
              <span className="text-sm text-muted-foreground">
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
