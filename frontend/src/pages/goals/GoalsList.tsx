import React, { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { themePrimitives, Button, Card, Badge } from '../../components/ui/ThemePrimitives';

interface Goal {
  id: number;
  goalType: 'TRIP' | 'PURCHASE';
  name: string;
  targetAmount: number;
  targetDate: string;
  planMonthlyContribution: number;
  status: 'ACTIVE' | 'COMPLETED' | 'PAUSED';
  tripMetadata?: {
    destination?: string;
    nights?: number;
    partySize?: number;
  };
  savedAmount: number;
  percentComplete: number;
  nudgeSuggested: boolean;
  nudgeAmount?: number;
  createdAt: string;
  updatedAt: string;
}

interface GoalCardProps {
  goal: Goal;
  onClick: () => void;
}

function GoalCard({ goal, onClick }: GoalCardProps) {
  const formatCurrency = (amount: number) => {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD',
      minimumFractionDigits: 0,
      maximumFractionDigits: 0,
    }).format(amount);
  };

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString('en-US', {
      month: 'short',
      day: 'numeric',
      year: 'numeric'
    });
  };

  const getDaysUntilTarget = () => {
    const today = new Date();
    const target = new Date(goal.targetDate);
    const diffTime = target.getTime() - today.getTime();
    const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
    return diffDays;
  };

  const getProgressColor = () => {
    if (goal.percentComplete >= 100) return themePrimitives.colors.green[500];
    if (goal.percentComplete >= 75) return themePrimitives.colors.blue[500];
    if (goal.percentComplete >= 50) return themePrimitives.colors.orange[500];
    return themePrimitives.colors.slate[400];
  };

  const getGoalIcon = () => {
    return goal.goalType === 'TRIP' ? '✈️' : '🛍️';
  };

  const daysUntilTarget = getDaysUntilTarget();
  const isOverdue = daysUntilTarget < 0;
  const isUpcoming = daysUntilTarget <= 30 && daysUntilTarget > 0;

  return (
    <Card
      className="p-6 cursor-pointer transition-all hover:shadow-lg relative overflow-hidden"
      onClick={onClick}
    >
      {goal.nudgeSuggested && (
        <div
          className="absolute top-0 right-0 w-3 h-3 rounded-full"
          style={{ backgroundColor: themePrimitives.colors.red[500] }}
        />
      )}

      <div className="flex items-start justify-between mb-4">
        <div className="flex items-center space-x-3">
          <div className="text-2xl">{getGoalIcon()}</div>
          <div>
            <h3 className="font-semibold text-lg" style={{ color: themePrimitives.colors.slate[900] }}>
              {goal.name}
            </h3>
            {goal.goalType === 'TRIP' && goal.tripMetadata?.destination && (
              <p className="text-sm" style={{ color: themePrimitives.colors.slate[600] }}>
                {goal.tripMetadata.destination}
              </p>
            )}
          </div>
        </div>
        <Badge
          variant={goal.status === 'ACTIVE' ? 'default' : 'secondary'}
          className="text-xs"
        >
          {goal.status}
        </Badge>
      </div>

      <div className="mb-4">
        <div className="flex justify-between items-center mb-2">
          <span className="text-sm font-medium" style={{ color: themePrimitives.colors.slate[700] }}>
            Progress
          </span>
          <span className="text-sm font-medium" style={{ color: themePrimitives.colors.slate[700] }}>
            {Math.round(goal.percentComplete)}%
          </span>
        </div>
        <div
          className="w-full h-2 rounded-full"
          style={{ backgroundColor: themePrimitives.colors.slate[200] }}
        >
          <div
            className="h-2 rounded-full transition-all duration-300"
            style={{
              width: `${Math.min(goal.percentComplete, 100)}%`,
              backgroundColor: getProgressColor()
            }}
          />
        </div>
      </div>

      <div className="grid grid-cols-2 gap-4 mb-4 text-sm">
        <div>
          <div className="font-medium" style={{ color: themePrimitives.colors.slate[700] }}>
            Saved
          </div>
          <div className="font-semibold" style={{ color: themePrimitives.colors.slate[900] }}>
            {formatCurrency(goal.savedAmount)}
          </div>
        </div>
        <div>
          <div className="font-medium" style={{ color: themePrimitives.colors.slate[700] }}>
            Target
          </div>
          <div className="font-semibold" style={{ color: themePrimitives.colors.slate[900] }}>
            {formatCurrency(goal.targetAmount)}
          </div>
        </div>
      </div>

      <div className="flex justify-between items-center text-sm">
        <div>
          <span className="font-medium" style={{ color: themePrimitives.colors.slate[700] }}>
            Target: {formatDate(goal.targetDate)}
          </span>
        </div>
        <div>
          {isOverdue ? (
            <span style={{ color: themePrimitives.colors.red[600] }}>
              {Math.abs(daysUntilTarget)} days overdue
            </span>
          ) : isUpcoming ? (
            <span style={{ color: themePrimitives.colors.orange[600] }}>
              {daysUntilTarget} days left
            </span>
          ) : (
            <span style={{ color: themePrimitives.colors.slate[600] }}>
              {daysUntilTarget} days left
            </span>
          )}
        </div>
      </div>

      {goal.nudgeSuggested && goal.nudgeAmount && (
        <div
          className="mt-4 p-3 rounded-lg border-l-4"
          style={{
            backgroundColor: themePrimitives.colors.orange[50],
            borderLeftColor: themePrimitives.colors.orange[500]
          }}
        >
          <div className="text-sm" style={{ color: themePrimitives.colors.orange[800] }}>
            💡 Consider contributing {formatCurrency(goal.nudgeAmount)} this month
          </div>
        </div>
      )}
    </Card>
  );
}

export default function GoalsList() {
  const navigate = useNavigate();
  const [goals, setGoals] = useState<Goal[]>([]);
  const [loading, setLoading] = useState(true);
  const [filter, setFilter] = useState<'all' | 'active' | 'completed'>('all');

  useEffect(() => {
    fetchGoals();
  }, []);

  const fetchGoals = async () => {
    try {
      const response = await fetch('/api/goals');
      if (!response.ok) {
        throw new Error('Failed to fetch goals');
      }
      const data = await response.json();
      setGoals(data);
    } catch (error) {
      console.error('Error fetching goals:', error);
    } finally {
      setLoading(false);
    }
  };

  const filteredGoals = goals.filter(goal => {
    if (filter === 'all') return true;
    if (filter === 'active') return goal.status === 'ACTIVE';
    if (filter === 'completed') return goal.status === 'COMPLETED';
    return true;
  });

  const handleGoalClick = (goalId: number) => {
    navigate(`/goals/${goalId}`);
  };

  const getFilterButtonStyle = (currentFilter: string) => ({
    backgroundColor: filter === currentFilter
      ? themePrimitives.colors.orange[500]
      : 'transparent',
    color: filter === currentFilter
      ? 'white'
      : themePrimitives.colors.slate[600],
    borderColor: filter === currentFilter
      ? themePrimitives.colors.orange[500]
      : themePrimitives.colors.slate[300]
  });

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-64">
        <div className="text-lg" style={{ color: themePrimitives.colors.slate[600] }}>
          Loading goals...
        </div>
      </div>
    );
  }

  return (
    <div className="max-w-6xl mx-auto p-6">
      <div className="flex justify-between items-center mb-8">
        <div>
          <h1 className="text-3xl font-bold" style={{ color: themePrimitives.colors.slate[900] }}>
            Your Goals
          </h1>
          <p className="text-lg mt-2" style={{ color: themePrimitives.colors.slate[600] }}>
            Track your savings progress and stay motivated
          </p>
        </div>
        <Link to="/goals/new">
          <Button
            style={{
              backgroundColor: themePrimitives.colors.orange[500],
              borderColor: themePrimitives.colors.orange[500]
            }}
          >
            + New Goal
          </Button>
        </Link>
      </div>

      <div className="flex space-x-2 mb-6">
        <Button
          variant="outline"
          onClick={() => setFilter('all')}
          style={getFilterButtonStyle('all')}
        >
          All ({goals.length})
        </Button>
        <Button
          variant="outline"
          onClick={() => setFilter('active')}
          style={getFilterButtonStyle('active')}
        >
          Active ({goals.filter(g => g.status === 'ACTIVE').length})
        </Button>
        <Button
          variant="outline"
          onClick={() => setFilter('completed')}
          style={getFilterButtonStyle('completed')}
        >
          Completed ({goals.filter(g => g.status === 'COMPLETED').length})
        </Button>
      </div>

      {filteredGoals.length === 0 ? (
        <Card className="p-12 text-center">
          <div className="text-6xl mb-4">🎯</div>
          <h3 className="text-xl font-semibold mb-2" style={{ color: themePrimitives.colors.slate[900] }}>
            {filter === 'all' ? 'No goals yet' : `No ${filter} goals`}
          </h3>
          <p className="text-lg mb-6" style={{ color: themePrimitives.colors.slate[600] }}>
            {filter === 'all'
              ? 'Start saving for something you care about'
              : `You don't have any ${filter} goals right now`
            }
          </p>
          {filter === 'all' && (
            <Link to="/goals/new">
              <Button
                style={{
                  backgroundColor: themePrimitives.colors.orange[500],
                  borderColor: themePrimitives.colors.orange[500]
                }}
              >
                Create Your First Goal
              </Button>
            </Link>
          )}
        </Card>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {filteredGoals.map((goal) => (
            <GoalCard
              key={goal.id}
              goal={goal}
              onClick={() => handleGoalClick(goal.id)}
            />
          ))}
        </div>
      )}

      {filteredGoals.length > 0 && (
        <div className="mt-8 text-center">
          <p className="text-sm" style={{ color: themePrimitives.colors.slate[500] }}>
            Showing {filteredGoals.length} of {goals.length} goals
          </p>
        </div>
      )}
    </div>
  );
}