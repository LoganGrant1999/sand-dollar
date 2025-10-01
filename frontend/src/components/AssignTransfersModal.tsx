import React, { useState, useEffect } from 'react';
import { themePrimitives, Button, Card } from './ui/ThemePrimitives';

interface CandidateTransaction {
  id: number;
  name: string;
  merchantName?: string;
  amount: number;
  date: string;
  plaidTransactionId: string;
  isTransfer: boolean;
}

interface AssignTransfersModalProps {
  isOpen: boolean;
  onClose: () => void;
  goalId: number;
  onAssigned: () => void;
}

export default function AssignTransfersModal({
  isOpen,
  onClose,
  goalId,
  onAssigned
}: AssignTransfersModalProps) {
  const [candidates, setCandidates] = useState<CandidateTransaction[]>([]);
  const [selectedTransactions, setSelectedTransactions] = useState<Set<number>>(new Set());
  const [loading, setLoading] = useState(false);
  const [assigning, setAssigning] = useState(false);

  useEffect(() => {
    if (isOpen) {
      fetchCandidates();
    }
  }, [isOpen, goalId]);

  const fetchCandidates = async () => {
    setLoading(true);
    try {
      const response = await fetch(`/api/goals/${goalId}/nudge/candidates`);
      if (!response.ok) throw new Error('Failed to fetch candidates');
      const data = await response.json();
      setCandidates(data);
    } catch (error) {
      console.error('Error fetching candidates:', error);
    } finally {
      setLoading(false);
    }
  };

  const toggleTransaction = (transactionId: number) => {
    const newSelected = new Set(selectedTransactions);
    if (newSelected.has(transactionId)) {
      newSelected.delete(transactionId);
    } else {
      newSelected.add(transactionId);
    }
    setSelectedTransactions(newSelected);
  };

  const assignSelectedTransactions = async () => {
    if (selectedTransactions.size === 0) return;

    setAssigning(true);
    try {
      // For each selected transaction, create a goal contribution
      const selectedCandidates = candidates.filter(t => selectedTransactions.has(t.id));

      for (const transaction of selectedCandidates) {
        const response = await fetch(`/api/goals/${goalId}/contributions`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            amount: transaction.amount,
            description: `Auto-assigned from ${transaction.name} (ID: ${transaction.plaidTransactionId})`,
          }),
        });

        if (!response.ok) {
          throw new Error(`Failed to assign transaction: ${transaction.name}`);
        }
      }

      // Clear the nudge after assignment
      await fetch(`/api/goals/${goalId}/nudge/clear`, {
        method: 'POST',
      });

      onAssigned();
      onClose();
    } catch (error) {
      console.error('Error assigning transactions:', error);
    } finally {
      setAssigning(false);
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

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString('en-US', {
      month: 'short',
      day: 'numeric',
      year: 'numeric'
    });
  };

  const getTotalSelected = () => {
    return candidates
      .filter(t => selectedTransactions.has(t.id))
      .reduce((sum, t) => sum + t.amount, 0);
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center p-4 z-50">
      <Card className="w-full max-w-2xl max-h-[80vh] overflow-hidden flex flex-col">
        <div className="p-6 border-b" style={{ borderColor: themePrimitives.colors.slate[200] }}>
          <h2 className="text-xl font-semibold" style={{ color: themePrimitives.colors.slate[900] }}>
            Assign Recent Transfers
          </h2>
          <p className="text-sm mt-1" style={{ color: themePrimitives.colors.slate[600] }}>
            Select recent income deposits to assign as goal contributions
          </p>
        </div>

        <div className="flex-1 overflow-y-auto p-6">
          {loading ? (
            <div className="text-center py-8" style={{ color: themePrimitives.colors.slate[600] }}>
              Loading recent transfers...
            </div>
          ) : candidates.length === 0 ? (
            <div className="text-center py-8">
              <div className="text-4xl mb-3">💸</div>
              <h3 className="font-medium mb-2" style={{ color: themePrimitives.colors.slate[900] }}>
                No Recent Transfers Found
              </h3>
              <p className="text-sm" style={{ color: themePrimitives.colors.slate[600] }}>
                We couldn't find any recent income deposits to assign to this goal.
              </p>
            </div>
          ) : (
            <div className="space-y-3">
              {candidates.map((transaction) => (
                <div
                  key={transaction.id}
                  className={`p-4 rounded-lg border-2 cursor-pointer transition-all ${
                    selectedTransactions.has(transaction.id)
                      ? 'border-orange-500 bg-orange-50'
                      : 'border-slate-200 hover:border-slate-300'
                  }`}
                  onClick={() => toggleTransaction(transaction.id)}
                >
                  <div className="flex justify-between items-start">
                    <div className="flex-1">
                      <div className="flex items-center space-x-2">
                        <div
                          className={`w-4 h-4 rounded border-2 flex items-center justify-center ${
                            selectedTransactions.has(transaction.id)
                              ? 'bg-orange-500 border-orange-500'
                              : 'border-slate-300'
                          }`}
                        >
                          {selectedTransactions.has(transaction.id) && (
                            <div className="w-2 h-2 bg-white rounded-sm" />
                          )}
                        </div>
                        <div className="font-medium" style={{ color: themePrimitives.colors.slate[900] }}>
                          {transaction.name}
                        </div>
                        {transaction.isTransfer && (
                          <span
                            className="text-xs px-2 py-1 rounded"
                            style={{
                              backgroundColor: themePrimitives.colors.blue[50],
                              color: themePrimitives.colors.blue[700]
                            }}
                          >
                            Transfer
                          </span>
                        )}
                      </div>
                      {transaction.merchantName && (
                        <div className="text-sm mt-1" style={{ color: themePrimitives.colors.slate[600] }}>
                          {transaction.merchantName}
                        </div>
                      )}
                      <div className="text-sm mt-1" style={{ color: themePrimitives.colors.slate[500] }}>
                        {formatDate(transaction.date)}
                      </div>
                    </div>
                    <div className="text-right">
                      <div className="font-semibold text-lg" style={{ color: themePrimitives.colors.green[600] }}>
                        {formatCurrency(transaction.amount)}
                      </div>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>

        <div className="p-6 border-t" style={{ borderColor: themePrimitives.colors.slate[200] }}>
          {selectedTransactions.size > 0 && (
            <div className="mb-4 p-3 rounded-lg" style={{ backgroundColor: themePrimitives.colors.green[50] }}>
              <div className="flex justify-between items-center">
                <span className="text-sm font-medium" style={{ color: themePrimitives.colors.green[800] }}>
                  Selected {selectedTransactions.size} transaction{selectedTransactions.size !== 1 ? 's' : ''}
                </span>
                <span className="font-semibold" style={{ color: themePrimitives.colors.green[800] }}>
                  {formatCurrency(getTotalSelected())}
                </span>
              </div>
            </div>
          )}

          <div className="flex space-x-3">
            <Button
              variant="outline"
              onClick={onClose}
              disabled={assigning}
              className="flex-1"
            >
              Cancel
            </Button>
            <Button
              onClick={assignSelectedTransactions}
              disabled={selectedTransactions.size === 0 || assigning}
              className="flex-1"
              style={{
                backgroundColor: themePrimitives.colors.green[600],
                borderColor: themePrimitives.colors.green[600]
              }}
            >
              {assigning
                ? `Assigning ${selectedTransactions.size} transaction${selectedTransactions.size !== 1 ? 's' : ''}...`
                : `Assign ${selectedTransactions.size} transaction${selectedTransactions.size !== 1 ? 's' : ''}`
              }
            </Button>
          </div>
        </div>
      </Card>
    </div>
  );
}