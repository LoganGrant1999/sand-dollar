import React, { useState, useEffect, useRef } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { themePrimitives, Button, Card, Input } from '../../components/ui/ThemePrimitives';
import BudgetPlanView from '../../components/BudgetPlanView';

interface Message {
  id: string;
  role: 'assistant' | 'user';
  content: string;
  timestamp: string;
}

interface TripBrief {
  destination?: string;
  name?: string;
  nights?: number;
  party_size?: number;
  target_date?: string;
  rough_budget?: number;
  [key: string]: any;
}

interface TripCostEstimate {
  destination: string;
  total_cost: number;
  target_date: string;
  breakdown: Record<string, number>;
}

interface FeasibilityResult {
  feasible: boolean;
  monthlyRequired: number;
  monthlyAvailable: number;
  verdict: 'YES' | 'NO' | 'MAYBE';
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

interface Session {
  id: string;
  brief: TripBrief;
  missing_fields: string[];
  messages: Message[];
}

interface FinalizationResult {
  brief: TripBrief;
  estimate: TripCostEstimate;
  feasibility: FeasibilityResult;
  budgetPlan: BudgetPlan;
  provisional: boolean;
  message?: string;
}

export default function TripChat() {
  const { sessionId } = useParams<{ sessionId?: string }>();
  const navigate = useNavigate();
  const [session, setSession] = useState<Session | null>(null);
  const [finalization, setFinalization] = useState<FinalizationResult | null>(null);
  const [inputMessage, setInputMessage] = useState('');
  const [loading, setLoading] = useState(false);
  const [creating, setCreating] = useState(false);
  const messagesEndRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (sessionId) {
      loadSession();
    } else {
      createNewSession();
    }
  }, [sessionId]);

  useEffect(() => {
    scrollToBottom();
  }, [session?.messages]);

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  const createNewSession = async () => {
    try {
      const response = await fetch('/api/trip-chat/sessions', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
      });
      if (!response.ok) throw new Error('Failed to create session');
      const newSession = await response.json();
      setSession(newSession);
      navigate(`/plan/trip/${newSession.id}`, { replace: true });
    } catch (error) {
      console.error('Error creating session:', error);
    }
  };

  const loadSession = async () => {
    try {
      const response = await fetch(`/api/trip-chat/sessions/${sessionId}`);
      if (!response.ok) throw new Error('Failed to load session');
      const sessionData = await response.json();
      setSession(sessionData);
    } catch (error) {
      console.error('Error loading session:', error);
    }
  };

  const sendMessage = async () => {
    if (!inputMessage.trim() || !session || loading) return;

    const userMessage = inputMessage.trim();
    setInputMessage('');
    setLoading(true);

    try {
      const response = await fetch(`/api/trip-chat/sessions/${session.id}/messages`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ content: userMessage }),
      });

      if (!response.ok) throw new Error('Failed to send message');
      const updatedSession = await response.json();
      setSession(updatedSession);
    } catch (error) {
      console.error('Error sending message:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleFinalize = async () => {
    if (!session) return;

    setLoading(true);
    try {
      const response = await fetch(`/api/trip-chat/sessions/${session.id}/finalize`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
      });

      if (!response.ok) throw new Error('Failed to finalize');
      const result = await response.json();
      setFinalization(result);
    } catch (error) {
      console.error('Error finalizing:', error);
    } finally {
      setLoading(false);
    }
  };

  const createGoalFromPlan = async () => {
    if (!finalization) return;

    setCreating(true);
    try {
      const payload = {
        goalType: 'TRIP',
        name: finalization.brief.name || `Trip to ${finalization.estimate.destination}`,
        targetAmount: finalization.estimate.total_cost,
        targetDate: finalization.estimate.target_date,
        planMonthlyContribution: finalization.feasibility.monthlyRequired,
        tripMetadata: {
          destination: finalization.estimate.destination,
          nights: finalization.brief.nights,
          partySize: finalization.brief.party_size
        }
      };

      const response = await fetch('/api/goals', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload),
      });

      if (!response.ok) throw new Error('Failed to create goal');
      const newGoal = await response.json();
      navigate(`/goals/${newGoal.id}`);
    } catch (error) {
      console.error('Error creating goal:', error);
    } finally {
      setCreating(false);
    }
  };

  const getCompletionPercentage = () => {
    if (!session) return 0;
    const totalFields = ['destination', 'nights', 'party_size', 'target_date', 'rough_budget'];
    const completedFields = totalFields.filter(field => session.brief[field] !== undefined);
    return (completedFields.length / totalFields.length) * 100;
  };

  const getProgressMessage = () => {
    const percentage = getCompletionPercentage();
    if (percentage < 25) return "We're just getting started 🌅";
    if (percentage < 50) return "Halfway there 🗺️";
    if (percentage < 75) return "Almost done 💪";
    return "Ready to run the numbers 🔢";
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
      month: 'long',
      day: 'numeric',
      year: 'numeric'
    });
  };

  const canFinalize = session && session.missing_fields.length === 0;

  if (!session) {
    return (
      <div className="flex items-center justify-center min-h-64">
        <div className="text-lg" style={{ color: themePrimitives.colors.slate[600] }}>
          Loading trip planner...
        </div>
      </div>
    );
  }

  if (finalization) {
    return (
      <div className="max-w-4xl mx-auto p-6 space-y-6">
        <div className="text-center">
          <h1 className="text-3xl font-bold mb-2" style={{ color: themePrimitives.colors.slate[900] }}>
            Your Trip Plan
          </h1>
          <p className="text-lg" style={{ color: themePrimitives.colors.slate[600] }}>
            {finalization.estimate.destination}
          </p>
        </div>

        {finalization.provisional && (
          <Card
            className="p-4 border-l-4"
            style={{
              backgroundColor: themePrimitives.colors.orange[50],
              borderLeftColor: themePrimitives.colors.orange[500]
            }}
          >
            <div className="flex items-center justify-between">
              <div>
                <div className="font-medium" style={{ color: themePrimitives.colors.orange[800] }}>
                  📊 Provisional Plan
                </div>
                <div className="text-sm" style={{ color: themePrimitives.colors.orange[700] }}>
                  This plan uses rough numbers. Link accounts to verify and auto-tune.
                </div>
              </div>
              <Button
                variant="outline"
                style={{
                  borderColor: themePrimitives.colors.orange[500],
                  color: themePrimitives.colors.orange[700]
                }}
              >
                Link Accounts
              </Button>
            </div>
          </Card>
        )}

        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          {/* Trip Cost Estimate */}
          <Card className="p-6">
            <h2 className="text-xl font-semibold mb-4" style={{ color: themePrimitives.colors.slate[900] }}>
              Cost Breakdown
            </h2>
            <div className="space-y-3">
              {Object.entries(finalization.estimate.breakdown).map(([category, amount]) => (
                <div key={category} className="flex justify-between">
                  <span style={{ color: themePrimitives.colors.slate[600] }}>
                    {category.charAt(0).toUpperCase() + category.slice(1)}
                  </span>
                  <span className="font-medium" style={{ color: themePrimitives.colors.slate[900] }}>
                    {formatCurrency(amount)}
                  </span>
                </div>
              ))}
              <div
                className="flex justify-between pt-3 border-t font-semibold text-lg"
                style={{ borderColor: themePrimitives.colors.slate[200] }}
              >
                <span style={{ color: themePrimitives.colors.slate[900] }}>Total</span>
                <span style={{ color: themePrimitives.colors.slate[900] }}>
                  {formatCurrency(finalization.estimate.total_cost)}
                </span>
              </div>
            </div>
            <div className="mt-4 text-sm" style={{ color: themePrimitives.colors.slate[600] }}>
              Target Date: {formatDate(finalization.estimate.target_date)}
            </div>
          </Card>

          {/* Feasibility */}
          <Card className="p-6">
            <h2 className="text-xl font-semibold mb-4" style={{ color: themePrimitives.colors.slate[900] }}>
              Feasibility Analysis
            </h2>
            <div
              className="text-center p-4 rounded-lg mb-4 font-semibold text-lg"
              style={{
                backgroundColor: finalization.feasibility.verdict === 'YES'
                  ? themePrimitives.colors.green[50]
                  : finalization.feasibility.verdict === 'MAYBE'
                    ? themePrimitives.colors.orange[50]
                    : themePrimitives.colors.red[50],
                color: finalization.feasibility.verdict === 'YES'
                  ? themePrimitives.colors.green[700]
                  : finalization.feasibility.verdict === 'MAYBE'
                    ? themePrimitives.colors.orange[700]
                    : themePrimitives.colors.red[700]
              }}
            >
              {finalization.feasibility.verdict === 'YES' && '✅ Feasible'}
              {finalization.feasibility.verdict === 'MAYBE' && '⚠️ Challenging but Possible'}
              {finalization.feasibility.verdict === 'NO' && '❌ Not Feasible'}
            </div>
            <div className="space-y-3">
              <div className="flex justify-between">
                <span style={{ color: themePrimitives.colors.slate[600] }}>Monthly Required</span>
                <span className="font-medium" style={{ color: themePrimitives.colors.slate[900] }}>
                  {formatCurrency(finalization.feasibility.monthlyRequired)}
                </span>
              </div>
              <div className="flex justify-between">
                <span style={{ color: themePrimitives.colors.slate[600] }}>Monthly Available</span>
                <span
                  className="font-medium"
                  style={{
                    color: finalization.feasibility.monthlyAvailable >= finalization.feasibility.monthlyRequired
                      ? themePrimitives.colors.green[600]
                      : themePrimitives.colors.red[600]
                  }}
                >
                  {formatCurrency(finalization.feasibility.monthlyAvailable)}
                </span>
              </div>
            </div>
          </Card>
        </div>

        {/* Budget Plan */}
        <BudgetPlanView budgetPlan={finalization.budgetPlan} />

        {/* Create Goal CTA */}
        <div className="text-center">
          <Button
            onClick={createGoalFromPlan}
            disabled={creating}
            size="lg"
            style={{
              backgroundColor: themePrimitives.colors.green[600],
              borderColor: themePrimitives.colors.green[600],
              fontSize: '1.125rem',
              padding: '0.75rem 2rem'
            }}
          >
            {creating ? 'Creating Goal...' : 'Create Goal from this Plan'}
          </Button>
        </div>
      </div>
    );
  }

  return (
    <div className="max-w-6xl mx-auto p-6">
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 h-[calc(100vh-8rem)]">
        {/* Chat Area */}
        <div className="lg:col-span-2 flex flex-col">
          {/* Progress Bar */}
          <div className="mb-4">
            <div className="flex justify-between items-center mb-2">
              <span className="text-sm font-medium" style={{ color: themePrimitives.colors.slate[700] }}>
                {getProgressMessage()}
              </span>
              <span className="text-sm" style={{ color: themePrimitives.colors.slate[600] }}>
                {Math.round(getCompletionPercentage())}%
              </span>
            </div>
            <div
              className="w-full h-2 rounded-full"
              style={{ backgroundColor: themePrimitives.colors.slate[200] }}
            >
              <div
                className="h-2 rounded-full transition-all duration-300"
                style={{
                  width: `${getCompletionPercentage()}%`,
                  backgroundColor: themePrimitives.colors.orange[500]
                }}
              />
            </div>
          </div>

          {/* Messages */}
          <Card className="flex-1 p-4 overflow-hidden flex flex-col">
            <div className="flex-1 overflow-y-auto space-y-4 mb-4">
              {session.messages.map((message) => (
                <div
                  key={message.id}
                  className={`flex ${message.role === 'user' ? 'justify-end' : 'justify-start'}`}
                >
                  <div
                    className={`max-w-[80%] p-3 rounded-lg ${
                      message.role === 'user'
                        ? 'rounded-br-none'
                        : 'rounded-bl-none'
                    }`}
                    style={{
                      backgroundColor: message.role === 'user'
                        ? themePrimitives.colors.orange[500]
                        : themePrimitives.colors.slate[100],
                      color: message.role === 'user'
                        ? 'white'
                        : themePrimitives.colors.slate[900]
                    }}
                  >
                    {message.content}
                  </div>
                </div>
              ))}
              {loading && (
                <div className="flex justify-start">
                  <div
                    className="p-3 rounded-lg rounded-bl-none"
                    style={{ backgroundColor: themePrimitives.colors.slate[100] }}
                  >
                    <div className="flex space-x-1">
                      <div className="w-2 h-2 bg-slate-400 rounded-full animate-bounce"></div>
                      <div className="w-2 h-2 bg-slate-400 rounded-full animate-bounce" style={{ animationDelay: '0.1s' }}></div>
                      <div className="w-2 h-2 bg-slate-400 rounded-full animate-bounce" style={{ animationDelay: '0.2s' }}></div>
                    </div>
                  </div>
                </div>
              )}
              <div ref={messagesEndRef} />
            </div>

            {/* Input */}
            <div className="flex space-x-2">
              <Input
                value={inputMessage}
                onChange={(e) => setInputMessage(e.target.value)}
                onKeyPress={(e) => e.key === 'Enter' && !e.shiftKey && sendMessage()}
                placeholder="Type your message..."
                disabled={loading}
                className="flex-1"
              />
              <Button
                onClick={sendMessage}
                disabled={!inputMessage.trim() || loading}
                style={{
                  backgroundColor: themePrimitives.colors.orange[500],
                  borderColor: themePrimitives.colors.orange[500]
                }}
              >
                Send
              </Button>
            </div>
          </Card>

          {/* Finalize Button */}
          {canFinalize && (
            <div className="mt-4">
              <Button
                onClick={handleFinalize}
                disabled={loading}
                className="w-full"
                size="lg"
                style={{
                  backgroundColor: themePrimitives.colors.green[600],
                  borderColor: themePrimitives.colors.green[600]
                }}
              >
                {loading ? 'Analyzing...' : 'Finalize & Budget It'}
              </Button>
            </div>
          )}
        </div>

        {/* Sidebar - Trip Brief */}
        <div>
          <Card className="p-6 sticky top-6">
            <h3 className="text-lg font-semibold mb-4" style={{ color: themePrimitives.colors.slate[900] }}>
              Trip Brief
            </h3>
            <div className="space-y-4">
              <div>
                <label className="text-sm font-medium" style={{ color: themePrimitives.colors.slate[600] }}>
                  Destination
                </label>
                <div className="mt-1 font-medium" style={{ color: themePrimitives.colors.slate[900] }}>
                  {session.brief.destination || 'Not specified'}
                </div>
              </div>

              <div>
                <label className="text-sm font-medium" style={{ color: themePrimitives.colors.slate[600] }}>
                  Trip Name
                </label>
                <div className="mt-1 font-medium" style={{ color: themePrimitives.colors.slate[900] }}>
                  {session.brief.name || 'Not specified'}
                </div>
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="text-sm font-medium" style={{ color: themePrimitives.colors.slate[600] }}>
                    Nights
                  </label>
                  <div className="mt-1 font-medium" style={{ color: themePrimitives.colors.slate[900] }}>
                    {session.brief.nights || '—'}
                  </div>
                </div>
                <div>
                  <label className="text-sm font-medium" style={{ color: themePrimitives.colors.slate[600] }}>
                    Party Size
                  </label>
                  <div className="mt-1 font-medium" style={{ color: themePrimitives.colors.slate[900] }}>
                    {session.brief.party_size || '—'}
                  </div>
                </div>
              </div>

              <div>
                <label className="text-sm font-medium" style={{ color: themePrimitives.colors.slate[600] }}>
                  Target Date
                </label>
                <div className="mt-1 font-medium" style={{ color: themePrimitives.colors.slate[900] }}>
                  {session.brief.target_date ? formatDate(session.brief.target_date) : 'Not specified'}
                </div>
              </div>

              <div>
                <label className="text-sm font-medium" style={{ color: themePrimitives.colors.slate[600] }}>
                  Budget
                </label>
                <div className="mt-1 font-medium" style={{ color: themePrimitives.colors.slate[900] }}>
                  {session.brief.rough_budget ? formatCurrency(session.brief.rough_budget) : 'Not specified'}
                </div>
              </div>
            </div>

            {session.missing_fields.length > 0 && (
              <div className="mt-6">
                <div className="text-sm font-medium mb-2" style={{ color: themePrimitives.colors.slate[700] }}>
                  Still need:
                </div>
                <div className="space-y-1">
                  {session.missing_fields.map((field) => (
                    <div
                      key={field}
                      className="text-sm px-2 py-1 rounded"
                      style={{
                        backgroundColor: themePrimitives.colors.orange[50],
                        color: themePrimitives.colors.orange[700]
                      }}
                    >
                      {field.charAt(0).toUpperCase() + field.slice(1).replace('_', ' ')}
                    </div>
                  ))}
                </div>
              </div>
            )}
          </Card>
        </div>
      </div>
    </div>
  );
}