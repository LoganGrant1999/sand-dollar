import React, { useState, useEffect, useRef } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { themePrimitives, Button, Card, Input, Badge } from '../../components/ui/ThemePrimitives';
import AssignTransfersModal from '../../components/AssignTransfersModal';

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

interface Contribution {
  id: number;
  amount: number;
  contributionDate: string;
  description?: string;
  source: 'MANUAL' | 'AUTO';
  createdAt: string;
}

interface FeasibilityData {
  feasible: boolean;
  monthlyRequired: number;
  monthlyAvailable: number;
  incomeAvg: number;
  fixedAvg: number;
  variableAvg: number;
  aiNarrative?: string;
}

interface BudgetPlan {
  monthlyTarget: number;
  categories: Array<{
    category: string;
    currentAvg: number;
    suggestedMax: number;
    trim: number;
    trimPercent: number;
  }>;
  rationale: string;
}

export default function GoalDetail() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [goal, setGoal] = useState<Goal | null>(null);
  const [contributions, setContributions] = useState<Contribution[]>([]);
  const [feasibility, setFeasibility] = useState<FeasibilityData | null>(null);
  const [budgetPlan, setBudgetPlan] = useState<BudgetPlan | null>(null);
  const [loading, setLoading] = useState(true);
  const [contributionAmount, setContributionAmount] = useState('');
  const [contributionDescription, setContributionDescription] = useState('');
  const [addingContribution, setAddingContribution] = useState(false);
  const [showAddContribution, setShowAddContribution] = useState(false);
  const [showAssignModal, setShowAssignModal] = useState(false);
  const pollingRef = useRef<NodeJS.Timeout | null>(null);

  useEffect(() => {
    if (id) {
      fetchGoalDetails();
      fetchContributions();
      fetchFeasibilityData();
      startPolling();
    }

    return () => {
      stopPolling();
    };
  }, [id]);

  // Poll for goal updates every 60 seconds
  const startPolling = () => {
    stopPolling(); // Clear any existing polling
    pollingRef.current = setInterval(() => {
      if (id) {
        fetchGoalDetails();
      }
    }, 60000); // 60 seconds
  };

  const stopPolling = () => {
    if (pollingRef.current) {
      clearInterval(pollingRef.current);
      pollingRef.current = null;
    }
  };

  // Stop polling when the page becomes hidden, restart when visible
  useEffect(() => {
    const handleVisibilityChange = () => {
      if (document.hidden) {
        stopPolling();
      } else if (id) {
        fetchGoalDetails(); // Refresh immediately when returning
        startPolling();
      }
    };

    document.addEventListener('visibilitychange', handleVisibilityChange);
    return () => {
      document.removeEventListener('visibilitychange', handleVisibilityChange);
    };
  }, [id]);

  const fetchGoalDetails = async () => {
    try {
      const response = await fetch(`/api/goals/${id}`);
      if (!response.ok) throw new Error('Failed to fetch goal');
      const data = await response.json();
      setGoal(data);
    } catch (error) {
      console.error('Error fetching goal:', error);
    }
  };

  const fetchContributions = async () => {
    try {
      const response = await fetch(`/api/goals/${id}/contributions`);
      if (!response.ok) throw new Error('Failed to fetch contributions');
      const data = await response.json();
      setContributions(data);
    } catch (error) {
      console.error('Error fetching contributions:', error);
    }
  };

  const fetchFeasibilityData = async () => {
    try {
      const response = await fetch(`/api/goals/${id}/feasibility`);
      if (response.ok) {
        const feasibilityData = await response.json();
        setFeasibility(feasibilityData);
      }

      const budgetResponse = await fetch(`/api/goals/${id}/budget-plan`);
      if (budgetResponse.ok) {
        const budgetData = await budgetResponse.json();
        setBudgetPlan(budgetData);
      }
    } catch (error) {
      console.error('Error fetching feasibility data:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleAddContribution = async () => {
    if (!contributionAmount || parseFloat(contributionAmount) <= 0) return;

    setAddingContribution(true);
    try {
      const response = await fetch(`/api/goals/${id}/contributions`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          amount: parseFloat(contributionAmount),
          description: contributionDescription || undefined,
        }),
      });

      if (!response.ok) throw new Error('Failed to add contribution');

      setContributionAmount('');
      setContributionDescription('');
      setShowAddContribution(false);
      await fetchGoalDetails();
      await fetchContributions();
    } catch (error) {
      console.error('Error adding contribution:', error);
    } finally {
      setAddingContribution(false);
    }
  };

  const handleAssignmentCompleted = async () => {
    // Refresh goal data after assignments
    await fetchGoalDetails();
    await fetchContributions();
  };

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
    if (!goal) return 0;
    const today = new Date();
    const target = new Date(goal.targetDate);
    const diffTime = target.getTime() - today.getTime();
    return Math.ceil(diffTime / (1000 * 60 * 60 * 24));
  };

  const getProgressColor = () => {
    if (!goal) return themePrimitives.colors.slate[400];
    if (goal.percentComplete >= 100) return themePrimitives.colors.green[500];
    if (goal.percentComplete >= 75) return themePrimitives.colors.blue[500];
    if (goal.percentComplete >= 50) return themePrimitives.colors.orange[500];
    return themePrimitives.colors.slate[400];
  };

  if (loading || !goal) {
    return (
      <div className="flex items-center justify-center min-h-64">
        <div className="text-lg" style={{ color: themePrimitives.colors.slate[600] }}>
          Loading goal details...
        </div>
      </div>
    );
  }

  const daysUntilTarget = getDaysUntilTarget();
  const isOverdue = daysUntilTarget < 0;
  const remainingAmount = goal.targetAmount - goal.savedAmount;

  return (
    <div className="max-w-4xl mx-auto p-6 space-y-6">
      {/* Header */}
      <div className="flex items-start justify-between">
        <div>
          <div className="flex items-center space-x-3 mb-2">
            <Button
              variant="outline"
              onClick={() => navigate('/goals')}
              className="text-sm"
            >
              ← Back to Goals
            </Button>
          </div>
          <h1 className="text-3xl font-bold flex items-center space-x-3" style={{ color: themePrimitives.colors.slate[900] }}>
            <span className="text-2xl">{goal.goalType === 'TRIP' ? '✈️' : '🛍️'}</span>
            <span>{goal.name}</span>
          </h1>
          {goal.goalType === 'TRIP' && goal.tripMetadata?.destination && (
            <p className="text-lg mt-1" style={{ color: themePrimitives.colors.slate[600] }}>
              {goal.tripMetadata.destination}
              {goal.tripMetadata.nights && ` • ${goal.tripMetadata.nights} nights`}
              {goal.tripMetadata.partySize && goal.tripMetadata.partySize > 1 && ` • ${goal.tripMetadata.partySize} people`}
            </p>
          )}
        </div>
        <Badge
          variant={goal.status === 'ACTIVE' ? 'default' : 'secondary'}
          className="text-sm px-3 py-1"
        >
          {goal.status}
        </Badge>
      </div>

      {/* Progress Overview */}
      <Card className="p-6">
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-6">
          <div className="text-center">
            <div className="text-3xl font-bold mb-1" style={{ color: themePrimitives.colors.green[600] }}>
              {formatCurrency(goal.savedAmount)}
            </div>
            <div className="text-sm" style={{ color: themePrimitives.colors.slate[600] }}>
              Saved
            </div>
          </div>
          <div className="text-center">
            <div className="text-3xl font-bold mb-1" style={{ color: themePrimitives.colors.slate[900] }}>
              {formatCurrency(goal.targetAmount)}
            </div>
            <div className="text-sm" style={{ color: themePrimitives.colors.slate[600] }}>
              Target
            </div>
          </div>
          <div className="text-center">
            <div className="text-3xl font-bold mb-1" style={{ color: remainingAmount > 0 ? themePrimitives.colors.orange[600] : themePrimitives.colors.green[600] }}>
              {formatCurrency(Math.max(0, remainingAmount))}
            </div>
            <div className="text-sm" style={{ color: themePrimitives.colors.slate[600] }}>
              {remainingAmount > 0 ? 'Remaining' : 'Complete!'}
            </div>
          </div>
        </div>

        <div className="mb-4">
          <div className="flex justify-between items-center mb-2">
            <span className="font-medium" style={{ color: themePrimitives.colors.slate[700] }}>
              Progress
            </span>
            <span className="font-medium" style={{ color: themePrimitives.colors.slate[700] }}>
              {Math.round(goal.percentComplete)}%
            </span>
          </div>
          <div
            className="w-full h-3 rounded-full"
            style={{ backgroundColor: themePrimitives.colors.slate[200] }}
          >
            <div
              className="h-3 rounded-full transition-all duration-300"
              style={{
                width: `${Math.min(goal.percentComplete, 100)}%`,
                backgroundColor: getProgressColor()
              }}
            />
          </div>
        </div>

        <div className="flex justify-between items-center text-sm">
          <div>
            <span className="font-medium" style={{ color: themePrimitives.colors.slate[700] }}>
              Target Date: {formatDate(goal.targetDate)}
            </span>
          </div>
          <div>
            {isOverdue ? (
              <span style={{ color: themePrimitives.colors.red[600] }}>
                {Math.abs(daysUntilTarget)} days overdue
              </span>
            ) : (
              <span style={{ color: daysUntilTarget <= 30 ? themePrimitives.colors.orange[600] : themePrimitives.colors.slate[600] }}>
                {daysUntilTarget} days left
              </span>
            )}
          </div>
        </div>
      </Card>

      {/* Nudge Alert */}
      {goal.nudgeSuggested && goal.nudgeAmount && (
        <Card
          className="p-4 border-l-4"
          style={{
            backgroundColor: themePrimitives.colors.green[50],
            borderLeftColor: themePrimitives.colors.green[500]
          }}
        >
          <div className="flex items-center justify-between">
            <div>
              <div className="font-medium" style={{ color: themePrimitives.colors.green[800] }}>
                🎉 Your paycheck posted! Want to add {formatCurrency(goal.nudgeAmount)} toward your plan?
              </div>
              <div className="text-sm mt-1" style={{ color: themePrimitives.colors.green[700] }}>
                We detected recent income deposits that could help you reach your goal faster.
              </div>
            </div>
            <div className="flex space-x-2">
              <Button
                size="sm"
                variant="outline"
                onClick={() => setShowAssignModal(true)}
                style={{
                  borderColor: themePrimitives.colors.green[500],
                  color: themePrimitives.colors.green[700]
                }}
              >
                Assign from recent transfers
              </Button>
              <Button
                size="sm"
                onClick={() => setShowAddContribution(true)}
                style={{
                  backgroundColor: themePrimitives.colors.green[600],
                  borderColor: themePrimitives.colors.green[600]
                }}
              >
                Add manual contribution
              </Button>
            </div>
          </div>
        </Card>
      )}

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Feasibility Analysis */}
        {feasibility && (
          <Card className="p-6">
            <h3 className="text-lg font-semibold mb-4" style={{ color: themePrimitives.colors.slate[900] }}>
              Feasibility Analysis
            </h3>
            <div className="space-y-4">
              <div className="flex justify-between items-center">
                <span className="text-sm" style={{ color: themePrimitives.colors.slate[600] }}>
                  Monthly Required
                </span>
                <span className="font-semibold" style={{ color: themePrimitives.colors.slate[900] }}>
                  {formatCurrency(feasibility.monthlyRequired)}
                </span>
              </div>
              <div className="flex justify-between items-center">
                <span className="text-sm" style={{ color: themePrimitives.colors.slate[600] }}>
                  Monthly Available
                </span>
                <span
                  className="font-semibold"
                  style={{
                    color: feasibility.monthlyAvailable >= feasibility.monthlyRequired
                      ? themePrimitives.colors.green[600]
                      : themePrimitives.colors.red[600]
                  }}
                >
                  {formatCurrency(feasibility.monthlyAvailable)}
                </span>
              </div>
              <div
                className="p-3 rounded-lg text-center font-medium"
                style={{
                  backgroundColor: feasibility.feasible
                    ? themePrimitives.colors.green[50]
                    : themePrimitives.colors.red[50],
                  color: feasibility.feasible
                    ? themePrimitives.colors.green[700]
                    : themePrimitives.colors.red[700]
                }}
              >
                {feasibility.feasible ? '✅ Feasible' : '⚠️ Challenging'}
              </div>
              {feasibility.aiNarrative && (
                <div
                  className="p-3 rounded-lg text-sm"
                  style={{
                    backgroundColor: themePrimitives.colors.slate[50],
                    color: themePrimitives.colors.slate[700]
                  }}
                >
                  {feasibility.aiNarrative}
                </div>
              )}
            </div>
          </Card>
        )}

        {/* Budget Plan */}
        {budgetPlan && (
          <Card className="p-6">
            <h3 className="text-lg font-semibold mb-4" style={{ color: themePrimitives.colors.slate[900] }}>
              Budget Recommendations
            </h3>
            <div className="space-y-3">
              {budgetPlan.categories.slice(0, 5).map((category, index) => (
                <div key={index} className="flex justify-between items-center text-sm">
                  <span style={{ color: themePrimitives.colors.slate[600] }}>
                    {category.category}
                  </span>
                  <div className="text-right">
                    <div className="font-medium" style={{ color: themePrimitives.colors.slate[900] }}>
                      {formatCurrency(category.suggestedMax)}
                    </div>
                    {category.trim > 0 && (
                      <div style={{ color: themePrimitives.colors.orange[600] }}>
                        -{formatCurrency(category.trim)}
                      </div>
                    )}
                  </div>
                </div>
              ))}
            </div>
            <div
              className="mt-4 p-3 rounded-lg text-sm"
              style={{
                backgroundColor: themePrimitives.colors.blue[50],
                color: themePrimitives.colors.blue[700]
              }}
            >
              {budgetPlan.rationale}
            </div>
          </Card>
        )}
      </div>

      {/* Contributions Section */}
      <Card className="p-6">
        <div className="flex justify-between items-center mb-4">
          <h3 className="text-lg font-semibold" style={{ color: themePrimitives.colors.slate[900] }}>
            Contributions ({contributions.length})
          </h3>
          <Button
            onClick={() => setShowAddContribution(true)}
            style={{
              backgroundColor: themePrimitives.colors.green[500],
              borderColor: themePrimitives.colors.green[500]
            }}
          >
            + Add Contribution
          </Button>
        </div>

        {showAddContribution && (
          <Card className="p-4 mb-4" style={{ backgroundColor: themePrimitives.colors.slate[50] }}>
            <h4 className="font-medium mb-3" style={{ color: themePrimitives.colors.slate[900] }}>
              Add New Contribution
            </h4>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mb-4">
              <div>
                <label className="block text-sm font-medium mb-1" style={{ color: themePrimitives.colors.slate[700] }}>
                  Amount *
                </label>
                <Input
                  type="number"
                  min="0"
                  step="0.01"
                  value={contributionAmount}
                  onChange={(e) => setContributionAmount(e.target.value)}
                  placeholder="0.00"
                />
              </div>
              <div>
                <label className="block text-sm font-medium mb-1" style={{ color: themePrimitives.colors.slate[700] }}>
                  Description (optional)
                </label>
                <Input
                  value={contributionDescription}
                  onChange={(e) => setContributionDescription(e.target.value)}
                  placeholder="e.g., Monthly savings"
                />
              </div>
            </div>
            <div className="flex space-x-2">
              <Button
                onClick={handleAddContribution}
                disabled={!contributionAmount || parseFloat(contributionAmount) <= 0 || addingContribution}
                style={{
                  backgroundColor: themePrimitives.colors.green[500],
                  borderColor: themePrimitives.colors.green[500]
                }}
              >
                {addingContribution ? 'Adding...' : 'Add Contribution'}
              </Button>
              <Button
                variant="outline"
                onClick={() => setShowAddContribution(false)}
              >
                Cancel
              </Button>
            </div>
          </Card>
        )}

        {contributions.length === 0 ? (
          <div className="text-center py-8" style={{ color: themePrimitives.colors.slate[500] }}>
            No contributions yet. Add your first contribution to get started!
          </div>
        ) : (
          <div className="space-y-3">
            {contributions.map((contribution) => (
              <div
                key={contribution.id}
                className="flex justify-between items-center p-3 rounded-lg"
                style={{ backgroundColor: themePrimitives.colors.slate[50] }}
              >
                <div>
                  <div className="font-medium" style={{ color: themePrimitives.colors.slate[900] }}>
                    {formatCurrency(contribution.amount)}
                  </div>
                  {contribution.description && (
                    <div className="text-sm" style={{ color: themePrimitives.colors.slate[600] }}>
                      {contribution.description}
                    </div>
                  )}
                </div>
                <div className="text-right">
                  <div className="text-sm" style={{ color: themePrimitives.colors.slate[600] }}>
                    {formatDate(contribution.contributionDate)}
                  </div>
                  <Badge
                    variant={contribution.source === 'AUTO' ? 'default' : 'secondary'}
                    className="text-xs"
                  >
                    {contribution.source === 'AUTO' ? 'Auto-detected' : 'Manual'}
                  </Badge>
                </div>
              </div>
            ))}
          </div>
        )}
      </Card>

      {/* Assign Transfers Modal */}
      <AssignTransfersModal
        isOpen={showAssignModal}
        onClose={() => setShowAssignModal(false)}
        goalId={parseInt(id!)}
        onAssigned={handleAssignmentCompleted}
      />
    </div>
  );
}