import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { themePrimitives, Button, Card, Badge } from '../../components/ui/ThemePrimitives';

interface BudgetBaselines {
  incomeAvg: number;
  fixedAvg: number;
  variableAvg: number;
  availableForGoals: number;
  categoryBreakdown: Array<{
    category: string;
    amount: number;
    percentage: number;
  }>;
  provisional: boolean;
  lastUpdated: string;
}

interface ActiveGoal {
  id: number;
  name: string;
  targetAmount: number;
  savedAmount: number;
  planMonthlyContribution: number;
  percentComplete: number;
  goalType: 'TRIP' | 'PURCHASE';
}

export default function BudgetSnapshot() {
  const [baselines, setBaselines] = useState<BudgetBaselines | null>(null);
  const [activeGoals, setActiveGoals] = useState<ActiveGoal[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchBudgetData();
    fetchActiveGoals();
  }, []);

  const fetchBudgetData = async () => {
    try {
      const response = await fetch('/api/budget/baselines');
      if (!response.ok) throw new Error('Failed to fetch budget baselines');
      const data = await response.json();
      setBaselines(data);
    } catch (error) {
      console.error('Error fetching budget data:', error);
    }
  };

  const fetchActiveGoals = async () => {
    try {
      const response = await fetch('/api/goals');
      if (!response.ok) throw new Error('Failed to fetch goals');
      const data = await response.json();
      const active = data.filter((goal: ActiveGoal) => goal.percentComplete < 100);
      setActiveGoals(active.slice(0, 3)); // Show top 3 active goals
    } catch (error) {
      console.error('Error fetching goals:', error);
    } finally {
      setLoading(false);
    }
  };

  const formatCurrency = (amount: number) => {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD',
      minimumFractionDigits: 0,
      maximumFractionDigits: 0,
    }).format(amount);
  };

  const formatPercentage = (value: number) => {
    return `${Math.round(value)}%`;
  };

  const getProgressColor = (percentage: number) => {
    if (percentage >= 100) return themePrimitives.colors.green[500];
    if (percentage >= 75) return themePrimitives.colors.blue[500];
    if (percentage >= 50) return themePrimitives.colors.orange[500];
    return themePrimitives.colors.slate[400];
  };

  const getCategoryColor = (index: number) => {
    const colors = [
      themePrimitives.colors.blue[500],
      themePrimitives.colors.green[500],
      themePrimitives.colors.orange[500],
      themePrimitives.colors.purple[500],
      themePrimitives.colors.pink[500],
      themePrimitives.colors.indigo[500],
      themePrimitives.colors.teal[500],
    ];
    return colors[index % colors.length];
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-64">
        <div className="text-lg" style={{ color: themePrimitives.colors.slate[600] }}>
          Loading budget snapshot...
        </div>
      </div>
    );
  }

  return (
    <div className="max-w-6xl mx-auto p-6 space-y-6">
      {/* Header */}
      <div className="flex justify-between items-center">
        <div>
          <h1 className="text-3xl font-bold" style={{ color: themePrimitives.colors.slate[900] }}>
            Budget Snapshot
          </h1>
          <p className="text-lg mt-2" style={{ color: themePrimitives.colors.slate[600] }}>
            Your financial overview based on recent transactions
          </p>
        </div>
        {baselines?.provisional && (
          <Badge
            variant="outline"
            className="px-3 py-1"
            style={{
              borderColor: themePrimitives.colors.orange[300],
              color: themePrimitives.colors.orange[700],
              backgroundColor: themePrimitives.colors.orange[50]
            }}
          >
            Provisional Data
          </Badge>
        )}
      </div>

      {baselines?.provisional && (
        <Card
          className="p-4 border-l-4"
          style={{
            backgroundColor: themePrimitives.colors.blue[50],
            borderLeftColor: themePrimitives.colors.blue[500]
          }}
        >
          <div className="flex items-center justify-between">
            <div>
              <div className="font-medium" style={{ color: themePrimitives.colors.blue[800] }}>
                📊 Enhance Your Budget Analysis
              </div>
              <div className="text-sm" style={{ color: themePrimitives.colors.blue[700] }}>
                Connect your bank accounts to get personalized insights and automated goal tracking
              </div>
            </div>
            <Link to="/connect">
              <Button
                style={{
                  backgroundColor: themePrimitives.colors.blue[500],
                  borderColor: themePrimitives.colors.blue[500]
                }}
              >
                Connect Accounts
              </Button>
            </Link>
          </div>
        </Card>
      )}

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Income & Expenses Overview */}
        <div className="lg:col-span-2 space-y-6">
          {baselines && (
            <Card className="p-6">
              <h2 className="text-xl font-semibold mb-6" style={{ color: themePrimitives.colors.slate[900] }}>
                Monthly Overview
              </h2>
              <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                <div className="text-center p-4 rounded-lg" style={{ backgroundColor: themePrimitives.colors.green[50] }}>
                  <div className="text-2xl font-bold mb-1" style={{ color: themePrimitives.colors.green[700] }}>
                    {formatCurrency(baselines.incomeAvg)}
                  </div>
                  <div className="text-sm font-medium" style={{ color: themePrimitives.colors.slate[600] }}>
                    Average Income
                  </div>
                </div>

                <div className="text-center p-4 rounded-lg" style={{ backgroundColor: themePrimitives.colors.red[50] }}>
                  <div className="text-2xl font-bold mb-1" style={{ color: themePrimitives.colors.red[700] }}>
                    {formatCurrency(baselines.fixedAvg + baselines.variableAvg)}
                  </div>
                  <div className="text-sm font-medium" style={{ color: themePrimitives.colors.slate[600] }}>
                    Total Expenses
                  </div>
                </div>

                <div className="text-center p-4 rounded-lg" style={{ backgroundColor: themePrimitives.colors.blue[50] }}>
                  <div className="text-2xl font-bold mb-1" style={{ color: themePrimitives.colors.blue[700] }}>
                    {formatCurrency(baselines.availableForGoals)}
                  </div>
                  <div className="text-sm font-medium" style={{ color: themePrimitives.colors.slate[600] }}>
                    Available for Goals
                  </div>
                </div>
              </div>

              {/* Expense Breakdown */}
              <div className="mt-6 pt-6 border-t" style={{ borderColor: themePrimitives.colors.slate[200] }}>
                <h3 className="text-lg font-medium mb-4" style={{ color: themePrimitives.colors.slate[900] }}>
                  Expense Breakdown
                </h3>
                <div className="grid grid-cols-2 gap-4 text-sm">
                  <div className="flex justify-between">
                    <span style={{ color: themePrimitives.colors.slate[600] }}>Fixed Expenses</span>
                    <span className="font-medium" style={{ color: themePrimitives.colors.slate[900] }}>
                      {formatCurrency(baselines.fixedAvg)}
                    </span>
                  </div>
                  <div className="flex justify-between">
                    <span style={{ color: themePrimitives.colors.slate[600] }}>Variable Expenses</span>
                    <span className="font-medium" style={{ color: themePrimitives.colors.slate[900] }}>
                      {formatCurrency(baselines.variableAvg)}
                    </span>
                  </div>
                </div>
              </div>
            </Card>
          )}

          {/* Category Spending */}
          {baselines?.categoryBreakdown && (
            <Card className="p-6">
              <h2 className="text-xl font-semibold mb-6" style={{ color: themePrimitives.colors.slate[900] }}>
                Spending by Category
              </h2>
              <div className="space-y-4">
                {baselines.categoryBreakdown.slice(0, 8).map((category, index) => (
                  <div key={category.category} className="flex items-center justify-between">
                    <div className="flex items-center space-x-3">
                      <div
                        className="w-3 h-3 rounded-full"
                        style={{ backgroundColor: getCategoryColor(index) }}
                      />
                      <span className="font-medium" style={{ color: themePrimitives.colors.slate[700] }}>
                        {category.category}
                      </span>
                    </div>
                    <div className="text-right">
                      <div className="font-semibold" style={{ color: themePrimitives.colors.slate[900] }}>
                        {formatCurrency(category.amount)}
                      </div>
                      <div className="text-xs" style={{ color: themePrimitives.colors.slate[500] }}>
                        {formatPercentage(category.percentage)}
                      </div>
                    </div>
                  </div>
                ))}
              </div>

              {baselines.categoryBreakdown.length > 8 && (
                <div className="mt-4 text-center">
                  <Button variant="outline" size="sm">
                    View All Categories
                  </Button>
                </div>
              )}
            </Card>
          )}
        </div>

        {/* Active Goals Sidebar */}
        <div className="space-y-6">
          <Card className="p-6">
            <div className="flex justify-between items-center mb-4">
              <h2 className="text-lg font-semibold" style={{ color: themePrimitives.colors.slate[900] }}>
                Active Goals
              </h2>
              <Link to="/goals">
                <Button variant="outline" size="sm">
                  View All
                </Button>
              </Link>
            </div>

            {activeGoals.length === 0 ? (
              <div className="text-center py-6">
                <div className="text-4xl mb-3">🎯</div>
                <p className="text-sm mb-4" style={{ color: themePrimitives.colors.slate[600] }}>
                  No active goals yet
                </p>
                <Link to="/goals/new">
                  <Button
                    size="sm"
                    style={{
                      backgroundColor: themePrimitives.colors.orange[500],
                      borderColor: themePrimitives.colors.orange[500]
                    }}
                  >
                    Create Goal
                  </Button>
                </Link>
              </div>
            ) : (
              <div className="space-y-4">
                {activeGoals.map((goal) => (
                  <Link key={goal.id} to={`/goals/${goal.id}`}>
                    <div
                      className="p-3 rounded-lg border cursor-pointer transition-all hover:shadow-md"
                      style={{ borderColor: themePrimitives.colors.slate[200] }}
                    >
                      <div className="flex items-start justify-between mb-2">
                        <div className="flex items-center space-x-2">
                          <span>{goal.goalType === 'TRIP' ? '✈️' : '🛍️'}</span>
                          <span className="font-medium text-sm" style={{ color: themePrimitives.colors.slate[900] }}>
                            {goal.name}
                          </span>
                        </div>
                        <span className="text-xs" style={{ color: themePrimitives.colors.slate[500] }}>
                          {formatPercentage(goal.percentComplete)}
                        </span>
                      </div>

                      <div className="mb-2">
                        <div
                          className="w-full h-1.5 rounded-full"
                          style={{ backgroundColor: themePrimitives.colors.slate[200] }}
                        >
                          <div
                            className="h-1.5 rounded-full transition-all"
                            style={{
                              width: `${Math.min(goal.percentComplete, 100)}%`,
                              backgroundColor: getProgressColor(goal.percentComplete)
                            }}
                          />
                        </div>
                      </div>

                      <div className="flex justify-between text-xs" style={{ color: themePrimitives.colors.slate[600] }}>
                        <span>{formatCurrency(goal.savedAmount)}</span>
                        <span>{formatCurrency(goal.targetAmount)}</span>
                      </div>
                    </div>
                  </Link>
                ))}

                <div className="pt-2">
                  <Link to="/goals/new">
                    <Button
                      variant="outline"
                      size="sm"
                      className="w-full"
                    >
                      + Add New Goal
                    </Button>
                  </Link>
                </div>
              </div>
            )}
          </Card>

          {/* Quick Actions */}
          <Card className="p-6">
            <h3 className="text-lg font-semibold mb-4" style={{ color: themePrimitives.colors.slate[900] }}>
              Quick Actions
            </h3>
            <div className="space-y-3">
              <Link to="/goals/new">
                <Button
                  variant="outline"
                  className="w-full text-left justify-start"
                >
                  🎯 Create New Goal
                </Button>
              </Link>
              <Link to="/transactions">
                <Button
                  variant="outline"
                  className="w-full text-left justify-start"
                >
                  📊 View Transactions
                </Button>
              </Link>
              {baselines?.provisional && (
                <Link to="/connect">
                  <Button
                    variant="outline"
                    className="w-full text-left justify-start"
                  >
                    🔗 Connect Bank
                  </Button>
                </Link>
              )}
            </div>
          </Card>

          {/* Last Updated */}
          {baselines?.lastUpdated && (
            <div className="text-center text-xs" style={{ color: themePrimitives.colors.slate[500] }}>
              Last updated: {new Date(baselines.lastUpdated).toLocaleDateString()}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}