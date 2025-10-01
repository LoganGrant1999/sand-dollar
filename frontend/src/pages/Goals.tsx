import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { api } from '@/lib/api'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Progress } from '@/components/ui/progress'
import {
  Target,
  Plus,
  Calendar,
  DollarSign,
  TrendingUp,
  MapPin
} from 'lucide-react'
import { formatCurrency, formatDate } from '@/lib/utils'
import { Link } from 'react-router-dom'

interface Goal {
  id: number
  name: string
  targetAmount: number
  currentAmount: number
  targetDate: string
  category: string
  description?: string
  createdAt: string
}

export default function Goals() {
  const { data: goals, isLoading, error } = useQuery<Goal[]>({
    queryKey: ['goals'],
    queryFn: async () => {
      const response = await api.get('/goals')
      return response.data
    }
  })

  if (isLoading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="h-8 w-8 animate-spin rounded-full border-b-2 border-primary"></div>
      </div>
    )
  }

  if (error) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="text-center">
          <p className="text-red-500">Failed to load goals</p>
          <p className="text-sm text-muted-foreground">Error: {error.message}</p>
        </div>
      </div>
    )
  }

  // Ensure goals is an array before using filter
  const goalsArray = Array.isArray(goals) ? goals : []
  const activeGoals = goalsArray.filter(goal => goal.currentAmount < goal.targetAmount)
  const completedGoals = goalsArray.filter(goal => goal.currentAmount >= goal.targetAmount)

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <div>
          <h1 className="text-3xl font-bold text-foreground">Your Goals</h1>
          <p className="text-muted-foreground mt-1">Track your progress toward financial milestones</p>
        </div>
        <Link to="/plan">
          <Button className="flex items-center gap-2">
            <Plus className="h-4 w-4" />
            Plan a Goal
          </Button>
        </Link>
      </div>

      {/* Active Goals */}
      {activeGoals.length > 0 && (
        <div className="space-y-4">
          <h2 className="text-xl font-semibold text-foreground">Active Goals</h2>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {activeGoals.map((goal) => {
              const progress = (goal.currentAmount / goal.targetAmount) * 100
              const remaining = goal.targetAmount - goal.currentAmount
              const daysRemaining = Math.ceil((new Date(goal.targetDate).getTime() - new Date().getTime()) / (1000 * 60 * 60 * 24))

              return (
                <Card key={goal.id} className="bg-card border-border hover:shadow-lg transition-shadow">
                  <CardHeader>
                    <div className="flex items-center justify-between">
                      <div className="flex items-center space-x-2">
                        <Target className="h-5 w-5 text-primary" />
                        <CardTitle className="text-lg">{goal.name}</CardTitle>
                      </div>
                      <span className="text-sm font-medium text-primary">{Math.round(progress)}%</span>
                    </div>
                    <CardDescription>{goal.category}</CardDescription>
                  </CardHeader>
                  <CardContent className="space-y-4">
                    <div>
                      <div className="flex justify-between text-sm mb-2">
                        <span className="text-muted-foreground">Progress</span>
                        <span className="font-medium">{formatCurrency(goal.currentAmount)} of {formatCurrency(goal.targetAmount)}</span>
                      </div>
                      <Progress value={progress} className="h-2" />
                    </div>

                    <div className="grid grid-cols-2 gap-4 text-sm">
                      <div className="space-y-1">
                        <div className="flex items-center space-x-1">
                          <DollarSign className="h-3 w-3 text-muted-foreground" />
                          <span className="text-muted-foreground">Remaining</span>
                        </div>
                        <div className="font-medium">{formatCurrency(remaining)}</div>
                      </div>
                      <div className="space-y-1">
                        <div className="flex items-center space-x-1">
                          <Calendar className="h-3 w-3 text-muted-foreground" />
                          <span className="text-muted-foreground">Target</span>
                        </div>
                        <div className="font-medium">{formatDate(goal.targetDate)}</div>
                      </div>
                    </div>

                    {daysRemaining > 0 && (
                      <div className="text-xs text-muted-foreground">
                        {daysRemaining} days remaining
                      </div>
                    )}
                  </CardContent>
                </Card>
              )
            })}
          </div>
        </div>
      )}

      {/* Completed Goals */}
      {completedGoals.length > 0 && (
        <div className="space-y-4">
          <h2 className="text-xl font-semibold text-foreground">Completed Goals</h2>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {completedGoals.map((goal) => (
              <Card key={goal.id} className="bg-card border-border opacity-75">
                <CardHeader>
                  <div className="flex items-center space-x-2">
                    <Target className="h-5 w-5 text-green-500" />
                    <CardTitle className="text-lg">{goal.name}</CardTitle>
                  </div>
                  <CardDescription>{goal.category} • Completed</CardDescription>
                </CardHeader>
                <CardContent>
                  <div className="flex justify-between text-sm">
                    <span className="text-muted-foreground">Amount</span>
                    <span className="font-medium text-green-500">{formatCurrency(goal.targetAmount)}</span>
                  </div>
                  <Progress value={100} className="h-2 mt-2" />
                </CardContent>
              </Card>
            ))}
          </div>
        </div>
      )}

      {/* Empty State */}
      {goalsArray.length === 0 && (
        <Card className="bg-card border-border">
          <CardContent className="text-center py-12">
            <Target className="h-12 w-12 text-muted-foreground mx-auto mb-4" />
            <h3 className="text-lg font-semibold text-foreground mb-2">No goals yet</h3>
            <p className="text-muted-foreground mb-6">
              Start planning your first financial goal to track your progress toward what matters most.
            </p>
            <Link to="/plan">
              <Button className="flex items-center gap-2">
                <MapPin className="h-4 w-4" />
                Plan Your First Goal
              </Button>
            </Link>
          </CardContent>
        </Card>
      )}
    </div>
  )
}