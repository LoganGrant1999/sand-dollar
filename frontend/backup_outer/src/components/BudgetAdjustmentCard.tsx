import { useState } from 'react'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { formatCurrency } from '@/lib/utils'
import { 
  CheckCircle2,
  AlertTriangle,
  TrendingUp,
  TrendingDown,
  ArrowRight
} from 'lucide-react'
import type { BudgetAdjustmentResponse, SourceCategoryOption } from '@/lib/api'

interface BudgetAdjustmentCardProps {
  adjustmentData: BudgetAdjustmentResponse
  instruction: string
  onConfirm: (sourceCategory?: string) => void
  onCancel: () => void
  loading?: boolean
}

export function BudgetAdjustmentCard({ 
  adjustmentData, 
  instruction, 
  onConfirm, 
  onCancel, 
  loading = false 
}: BudgetAdjustmentCardProps) {
  const [selectedSourceCategory, setSelectedSourceCategory] = useState<string | undefined>()

  const isConfirmationNeeded = adjustmentData.status === 'needs_confirmation'
  const diffs = adjustmentData.proposal?.diffs || []

  const handleConfirm = () => {
    if (isConfirmationNeeded && !selectedSourceCategory) {
      return // Don't allow confirmation without selecting a source
    }
    onConfirm(selectedSourceCategory)
  }

  return (
    <Card className="border-l-4 border-l-[var(--color-accent-teal)]">
      <CardHeader>
        <div className="flex items-start justify-between">
          <div>
            <CardTitle className="flex items-center space-x-2">
              {adjustmentData.status === 'success' ? (
                <CheckCircle2 className="h-5 w-5 text-[var(--color-success)]" />
              ) : (
                <AlertTriangle className="h-5 w-5 text-[var(--color-warning)]" />
              )}
              <span>Budget Adjustment</span>
            </CardTitle>
            <CardDescription className="mt-2">
              <span className="font-medium">Instruction:</span> "{instruction}"
            </CardDescription>
          </div>
          <Badge variant={adjustmentData.status === 'success' ? 'default' : 'secondary'}>
            {adjustmentData.status === 'success' ? 'Ready to Apply' : 'Needs Confirmation'}
          </Badge>
        </div>
      </CardHeader>

      <CardContent className="space-y-4">
        {/* Show proposed changes */}
        {diffs.length > 0 && (
          <div>
            <h4 className="font-medium text-[var(--color-text-primary)] mb-3">Proposed Changes</h4>
            <div className="space-y-2">
              {diffs.map((diff, index) => (
                <div key={index} className="flex items-center justify-between rounded-lg border border-[rgba(255,255,255,0.08)] bg-[rgba(255,255,255,0.02)] p-3">
                  <div className="flex items-center space-x-3">
                    <div className="flex items-center space-x-2">
                      {diff.deltaAmount > 0 ? (
                        <TrendingUp className="h-4 w-4 text-[var(--color-success)]" />
                      ) : (
                        <TrendingDown className="h-4 w-4 text-[var(--color-error)]" />
                      )}
                      <span className="font-medium">{diff.category}</span>
                    </div>
                  </div>
                  <div className="text-right">
                    <div className="flex items-center space-x-2 text-sm">
                      <span>{formatCurrency(diff.currentAmount)}</span>
                      <ArrowRight className="h-3 w-3 text-[var(--color-text-secondary)]/80" />
                      <span className="font-medium">{formatCurrency(diff.newAmount)}</span>
                    </div>
                    <div className={`text-xs ${diff.deltaAmount > 0 ? 'text-[var(--color-success)]' : 'text-[var(--color-error)]'}`}>
                      {diff.deltaAmount > 0 ? '+' : ''}{formatCurrency(diff.deltaAmount)}
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}

        {/* Show source category options if confirmation needed */}
        {isConfirmationNeeded && adjustmentData.options && adjustmentData.options.length > 0 && (
          <div>
            <h4 className="font-medium text-[var(--color-text-primary)] mb-3">Choose a category to reduce:</h4>
            <div className="space-y-2">
              {adjustmentData.options.map((option: SourceCategoryOption, index: number) => (
                <button
                  key={index}
                  onClick={() => setSelectedSourceCategory(option.category)}
                  className={`w-full rounded-lg border p-3 text-left transition-colors ${
                    selectedSourceCategory === option.category
                      ? 'border-[var(--color-accent-teal)] bg-[var(--color-accent-teal)]/15 shadow-[0px_2px_10px_rgba(0,0,0,0.4)]'
                      : 'border-[rgba(255,255,255,0.08)] hover:border-[var(--color-accent-teal)]'
                  }`}
                >
                  <div className="flex items-center justify-between">
                    <div>
                      <div className="font-medium">{option.category}</div>
                      <div className="text-sm text-[var(--color-text-secondary)]">
                        Current: {formatCurrency(option.currentAmount)}
                      </div>
                    </div>
                    <div className="text-right">
                      <div className="text-sm font-medium text-[var(--color-error)]">
                        -{formatCurrency(option.suggestedReduction)}
                      </div>
                      <div className="text-xs text-[var(--color-text-secondary)]">
                        Remaining: {formatCurrency(option.currentAmount - option.suggestedReduction)}
                      </div>
                    </div>
                  </div>
                </button>
              ))}
            </div>
          </div>
        )}

        {/* Action buttons */}
        <div className="flex justify-end space-x-3 border-t border-[rgba(255,255,255,0.08)] pt-4">
          <Button variant="outline" onClick={onCancel} disabled={loading}>
            Cancel
          </Button>
          <Button 
            onClick={handleConfirm} 
            disabled={loading || (isConfirmationNeeded && !selectedSourceCategory)}
            className="min-w-[100px]"
          >
            {loading ? (
              <div className="flex items-center space-x-2">
                <div className="h-4 w-4 animate-spin rounded-full border-2 border-[var(--color-text-primary)] border-t-transparent" />
                <span>Applying...</span>
              </div>
            ) : (
              adjustmentData.status === 'success' ? 'Apply Changes' : 'Confirm Adjustment'
            )}
          </Button>
        </div>
      </CardContent>
    </Card>
  )
}
