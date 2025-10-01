import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { themePrimitives, Button, Card, Input, Badge } from '../../components/ui/ThemePrimitives';

interface TripDetails {
  destination: string;
  name: string;
  nights: number;
  partySize: number;
  targetDate: string;
  roughBudget: number;
}

interface PurchaseDetails {
  itemName: string;
  price: number;
  targetDate: string;
}

interface FeasibilityResult {
  feasible: boolean;
  monthlyRequired: number;
  monthlyAvailable: number;
  narrative: string;
  verdict: 'YES' | 'NO' | 'MAYBE';
}

type GoalType = 'TRIP' | 'PURCHASE';
type Step = 1 | 2 | 3;

export default function PlanWizard() {
  const navigate = useNavigate();
  const [step, setStep] = useState<Step>(1);
  const [goalType, setGoalType] = useState<GoalType | null>(null);
  const [tripDetails, setTripDetails] = useState<TripDetails>({
    destination: '',
    name: '',
    nights: 1,
    partySize: 1,
    targetDate: '',
    roughBudget: 0
  });
  const [purchaseDetails, setPurchaseDetails] = useState<PurchaseDetails>({
    itemName: '',
    price: 0,
    targetDate: ''
  });
  const [feasibilityResult, setFeasibilityResult] = useState<FeasibilityResult | null>(null);
  const [loading, setLoading] = useState(false);

  const handleGoalTypeSelect = (type: GoalType) => {
    setGoalType(type);
    setStep(2);
  };

  const handleDetailsSubmit = async () => {
    if (!goalType) return;

    setLoading(true);
    try {
      const payload = goalType === 'TRIP'
        ? {
            goalType: 'TRIP',
            name: tripDetails.name || `Trip to ${tripDetails.destination}`,
            targetAmount: tripDetails.roughBudget,
            targetDate: tripDetails.targetDate,
            tripMetadata: {
              destination: tripDetails.destination,
              nights: tripDetails.nights,
              partySize: tripDetails.partySize
            }
          }
        : {
            goalType: 'PURCHASE',
            name: purchaseDetails.itemName,
            targetAmount: purchaseDetails.price,
            targetDate: purchaseDetails.targetDate
          };

      const response = await fetch('/api/goals/feasibility', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(payload),
      });

      if (!response.ok) {
        throw new Error('Failed to check feasibility');
      }

      const result = await response.json();
      setFeasibilityResult({
        feasible: result.feasible,
        monthlyRequired: result.monthlyRequired,
        monthlyAvailable: result.monthlyAvailable,
        narrative: result.aiNarrative || 'Based on your spending patterns, this goal is achievable.',
        verdict: result.feasible ? 'YES' : result.monthlyRequired <= result.monthlyAvailable * 1.1 ? 'MAYBE' : 'NO'
      });
      setStep(3);
    } catch (error) {
      console.error('Error checking feasibility:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleStartPlan = async () => {
    if (!goalType || !feasibilityResult) return;

    setLoading(true);
    try {
      const payload = goalType === 'TRIP'
        ? {
            goalType: 'TRIP',
            name: tripDetails.name || `Trip to ${tripDetails.destination}`,
            targetAmount: tripDetails.roughBudget,
            targetDate: tripDetails.targetDate,
            planMonthlyContribution: feasibilityResult.monthlyRequired,
            tripMetadata: {
              destination: tripDetails.destination,
              nights: tripDetails.nights,
              partySize: tripDetails.partySize
            }
          }
        : {
            goalType: 'PURCHASE',
            name: purchaseDetails.itemName,
            targetAmount: purchaseDetails.price,
            targetDate: purchaseDetails.targetDate,
            planMonthlyContribution: feasibilityResult.monthlyRequired
          };

      const response = await fetch('/api/goals', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(payload),
      });

      if (!response.ok) {
        throw new Error('Failed to create goal');
      }

      const newGoal = await response.json();
      navigate(`/goals/${newGoal.id}`);
    } catch (error) {
      console.error('Error creating goal:', error);
    } finally {
      setLoading(false);
    }
  };

  const canProceedStep2 = () => {
    if (goalType === 'TRIP') {
      return tripDetails.destination && tripDetails.targetDate && tripDetails.roughBudget > 0;
    }
    return purchaseDetails.itemName && purchaseDetails.targetDate && purchaseDetails.price > 0;
  };

  const getVerdictColor = (verdict: string) => {
    switch (verdict) {
      case 'YES': return themePrimitives.colors.green[500];
      case 'MAYBE': return themePrimitives.colors.amber[500];
      case 'NO': return themePrimitives.colors.red[500];
      default: return themePrimitives.colors.slate[500];
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

  return (
    <div className="max-w-2xl mx-auto p-6 space-y-6">
      <div className="text-center mb-8">
        <h1 className="text-3xl font-bold mb-2" style={{ color: themePrimitives.colors.slate[900] }}>
          Plan Your Goal
        </h1>
        <div className="flex items-center justify-center space-x-4">
          {[1, 2, 3].map((stepNum) => (
            <div key={stepNum} className="flex items-center">
              <div
                className={`w-8 h-8 rounded-full flex items-center justify-center text-sm font-medium ${
                  step >= stepNum
                    ? 'text-white'
                    : 'text-slate-400'
                }`}
                style={{
                  backgroundColor: step >= stepNum
                    ? themePrimitives.colors.orange[500]
                    : themePrimitives.colors.slate[200]
                }}
              >
                {stepNum}
              </div>
              {stepNum < 3 && (
                <div
                  className="w-12 h-0.5 mx-2"
                  style={{
                    backgroundColor: step > stepNum
                      ? themePrimitives.colors.orange[500]
                      : themePrimitives.colors.slate[200]
                  }}
                />
              )}
            </div>
          ))}
        </div>
        <div className="mt-2 text-sm" style={{ color: themePrimitives.colors.slate[600] }}>
          {step === 1 && 'Choose Goal Type'}
          {step === 2 && 'Enter Details'}
          {step === 3 && 'Review Plan'}
        </div>
      </div>

      {step === 1 && (
        <div className="space-y-4">
          <h2 className="text-xl font-semibold text-center mb-6" style={{ color: themePrimitives.colors.slate[800] }}>
            What would you like to save for?
          </h2>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <Card
              className="p-6 cursor-pointer transition-all hover:shadow-lg border-2"
              style={{
                borderColor: goalType === 'TRIP' ? themePrimitives.colors.orange[500] : themePrimitives.colors.slate[200]
              }}
              onClick={() => handleGoalTypeSelect('TRIP')}
            >
              <div className="text-center">
                <div className="text-4xl mb-3">✈️</div>
                <h3 className="text-lg font-semibold mb-2" style={{ color: themePrimitives.colors.slate[900] }}>
                  Trip
                </h3>
                <p className="text-sm" style={{ color: themePrimitives.colors.slate[600] }}>
                  Plan and save for your dream vacation or travel adventure
                </p>
              </div>
            </Card>

            <Card
              className="p-6 cursor-pointer transition-all hover:shadow-lg border-2"
              style={{
                borderColor: goalType === 'PURCHASE' ? themePrimitives.colors.orange[500] : themePrimitives.colors.slate[200]
              }}
              onClick={() => handleGoalTypeSelect('PURCHASE')}
            >
              <div className="text-center">
                <div className="text-4xl mb-3">🛍️</div>
                <h3 className="text-lg font-semibold mb-2" style={{ color: themePrimitives.colors.slate[900] }}>
                  Purchase
                </h3>
                <p className="text-sm" style={{ color: themePrimitives.colors.slate[600] }}>
                  Save for a specific item or big purchase you want to make
                </p>
              </div>
            </Card>
          </div>
        </div>
      )}

      {step === 2 && goalType === 'TRIP' && (
        <Card className="p-6">
          <h2 className="text-xl font-semibold mb-6" style={{ color: themePrimitives.colors.slate[800] }}>
            Trip Details
          </h2>
          <div className="space-y-4">
            <div>
              <label className="block text-sm font-medium mb-2" style={{ color: themePrimitives.colors.slate[700] }}>
                Destination *
              </label>
              <Input
                value={tripDetails.destination}
                onChange={(e) => setTripDetails(prev => ({ ...prev, destination: e.target.value }))}
                placeholder="e.g., Paris, France"
              />
            </div>

            <div>
              <label className="block text-sm font-medium mb-2" style={{ color: themePrimitives.colors.slate[700] }}>
                Trip Name (optional)
              </label>
              <Input
                value={tripDetails.name}
                onChange={(e) => setTripDetails(prev => ({ ...prev, name: e.target.value }))}
                placeholder="e.g., Honeymoon in Paris"
              />
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-medium mb-2" style={{ color: themePrimitives.colors.slate[700] }}>
                  Nights
                </label>
                <Input
                  type="number"
                  min="1"
                  value={tripDetails.nights}
                  onChange={(e) => setTripDetails(prev => ({ ...prev, nights: parseInt(e.target.value) || 1 }))}
                />
              </div>

              <div>
                <label className="block text-sm font-medium mb-2" style={{ color: themePrimitives.colors.slate[700] }}>
                  Party Size
                </label>
                <Input
                  type="number"
                  min="1"
                  value={tripDetails.partySize}
                  onChange={(e) => setTripDetails(prev => ({ ...prev, partySize: parseInt(e.target.value) || 1 }))}
                />
              </div>
            </div>

            <div>
              <label className="block text-sm font-medium mb-2" style={{ color: themePrimitives.colors.slate[700] }}>
                Target Date *
              </label>
              <Input
                type="date"
                value={tripDetails.targetDate}
                onChange={(e) => setTripDetails(prev => ({ ...prev, targetDate: e.target.value }))}
              />
            </div>

            <div>
              <label className="block text-sm font-medium mb-2" style={{ color: themePrimitives.colors.slate[700] }}>
                Rough Budget *
              </label>
              <Input
                type="number"
                min="0"
                step="100"
                value={tripDetails.roughBudget || ''}
                onChange={(e) => setTripDetails(prev => ({ ...prev, roughBudget: parseFloat(e.target.value) || 0 }))}
                placeholder="0"
              />
            </div>
          </div>

          <div className="flex justify-between mt-8">
            <Button variant="outline" onClick={() => setStep(1)}>
              Back
            </Button>
            <Button
              onClick={handleDetailsSubmit}
              disabled={!canProceedStep2() || loading}
            >
              {loading ? 'Checking...' : 'Check Feasibility'}
            </Button>
          </div>
        </Card>
      )}

      {step === 2 && goalType === 'PURCHASE' && (
        <Card className="p-6">
          <h2 className="text-xl font-semibold mb-6" style={{ color: themePrimitives.colors.slate[800] }}>
            Purchase Details
          </h2>
          <div className="space-y-4">
            <div>
              <label className="block text-sm font-medium mb-2" style={{ color: themePrimitives.colors.slate[700] }}>
                Item Name *
              </label>
              <Input
                value={purchaseDetails.itemName}
                onChange={(e) => setPurchaseDetails(prev => ({ ...prev, itemName: e.target.value }))}
                placeholder="e.g., MacBook Pro, Wedding Ring"
              />
            </div>

            <div>
              <label className="block text-sm font-medium mb-2" style={{ color: themePrimitives.colors.slate[700] }}>
                Price *
              </label>
              <Input
                type="number"
                min="0"
                step="1"
                value={purchaseDetails.price || ''}
                onChange={(e) => setPurchaseDetails(prev => ({ ...prev, price: parseFloat(e.target.value) || 0 }))}
                placeholder="0"
              />
            </div>

            <div>
              <label className="block text-sm font-medium mb-2" style={{ color: themePrimitives.colors.slate[700] }}>
                Target Date *
              </label>
              <Input
                type="date"
                value={purchaseDetails.targetDate}
                onChange={(e) => setPurchaseDetails(prev => ({ ...prev, targetDate: e.target.value }))}
              />
            </div>
          </div>

          <div className="flex justify-between mt-8">
            <Button variant="outline" onClick={() => setStep(1)}>
              Back
            </Button>
            <Button
              onClick={handleDetailsSubmit}
              disabled={!canProceedStep2() || loading}
            >
              {loading ? 'Checking...' : 'Check Feasibility'}
            </Button>
          </div>
        </Card>
      )}

      {step === 3 && feasibilityResult && (
        <Card className="p-6">
          <div className="text-center mb-6">
            <Badge
              className="text-lg px-4 py-2 font-semibold"
              style={{
                backgroundColor: `${getVerdictColor(feasibilityResult.verdict)}20`,
                color: getVerdictColor(feasibilityResult.verdict),
                border: `1px solid ${getVerdictColor(feasibilityResult.verdict)}40`
              }}
            >
              {feasibilityResult.verdict}
            </Badge>
          </div>

          <div className="grid grid-cols-2 gap-6 mb-6">
            <Card className="p-4 text-center" style={{ backgroundColor: themePrimitives.colors.slate[50] }}>
              <div className="text-sm font-medium mb-1" style={{ color: themePrimitives.colors.slate[600] }}>
                Monthly Required
              </div>
              <div className="text-2xl font-bold" style={{ color: themePrimitives.colors.slate[900] }}>
                {formatCurrency(feasibilityResult.monthlyRequired)}
              </div>
            </Card>

            <Card className="p-4 text-center" style={{ backgroundColor: themePrimitives.colors.green[50] }}>
              <div className="text-sm font-medium mb-1" style={{ color: themePrimitives.colors.slate[600] }}>
                Monthly Available
              </div>
              <div className="text-2xl font-bold" style={{ color: themePrimitives.colors.green[700] }}>
                {formatCurrency(feasibilityResult.monthlyAvailable)}
              </div>
            </Card>
          </div>

          <div className="mb-6 p-4 rounded-lg" style={{ backgroundColor: themePrimitives.colors.orange[50] }}>
            <p className="text-sm leading-relaxed" style={{ color: themePrimitives.colors.slate[700] }}>
              {feasibilityResult.narrative}
            </p>
          </div>

          <div className="flex justify-between">
            <Button variant="outline" onClick={() => setStep(2)}>
              Back
            </Button>
            <Button
              onClick={handleStartPlan}
              disabled={loading}
              style={{
                backgroundColor: themePrimitives.colors.green[600],
                borderColor: themePrimitives.colors.green[600]
              }}
            >
              {loading ? 'Creating...' : 'Start Plan'}
            </Button>
          </div>
        </Card>
      )}
    </div>
  );
}