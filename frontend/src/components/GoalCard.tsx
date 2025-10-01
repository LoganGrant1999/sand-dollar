import React from 'react';
import { themePrimitives, Card, Badge } from './ui/ThemePrimitives';

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
  onClick?: () => void;
  variant?: 'default' | 'compact';
  showNudge?: boolean;
}

export default function GoalCard({ goal, onClick, variant = 'default', showNudge = true }: GoalCardProps) {
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

  if (variant === 'compact') {
    return (
      <Card
        className={`p-4 ${onClick ? 'cursor-pointer transition-all hover:shadow-md' : ''} relative overflow-hidden`}
        onClick={onClick}
      >
        {goal.nudgeSuggested && showNudge && (
          <div
            className="absolute top-0 right-0 w-2 h-2 rounded-full"
            style={{ backgroundColor: themePrimitives.colors.red[500] }}
          />
        )}

        <div className="flex items-center justify-between mb-3">
          <div className="flex items-center space-x-2">
            <span className="text-lg">{getGoalIcon()}</span>
            <div>
              <h4 className="font-medium text-sm" style={{ color: themePrimitives.colors.slate[900] }}>
                {goal.name}
              </h4>
              {goal.goalType === 'TRIP' && goal.tripMetadata?.destination && (
                <p className="text-xs" style={{ color: themePrimitives.colors.slate[500] }}>
                  {goal.tripMetadata.destination}
                </p>
              )}
            </div>
          </div>
          <span className="text-xs font-medium" style={{ color: themePrimitives.colors.slate[600] }}>
            {Math.round(goal.percentComplete)}%
          </span>
        </div>

        <div className="mb-2">
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

        <div className="flex justify-between text-xs" style={{ color: themePrimitives.colors.slate[600] }}>
          <span>{formatCurrency(goal.savedAmount)}</span>
          <span>{formatCurrency(goal.targetAmount)}</span>
        </div>
      </Card>
    );
  }

  return (
    <Card
      className={`p-6 ${onClick ? 'cursor-pointer transition-all hover:shadow-lg' : ''} relative overflow-hidden`}
      onClick={onClick}
    >
      {goal.nudgeSuggested && showNudge && (
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
                {goal.tripMetadata.nights && ` • ${goal.tripMetadata.nights} nights`}
                {goal.tripMetadata.partySize && goal.tripMetadata.partySize > 1 && ` • ${goal.tripMetadata.partySize} people`}
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

      {goal.nudgeSuggested && goal.nudgeAmount && showNudge && (
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