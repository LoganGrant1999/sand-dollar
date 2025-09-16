import { cn } from '../../lib/utils'

interface ProgressBarProps {
  value: number
  max: number
  label?: string
  formatValue?: (value: number) => string
  className?: string
  showPercentage?: boolean
  variant?: 'default' | 'success' | 'warning' | 'danger'
}

export default function ProgressBar({
  value,
  max,
  label,
  formatValue = (val) => `$${val.toLocaleString()}`,
  className,
  showPercentage = true,
  variant = 'default'
}: ProgressBarProps) {
  const percentage = max > 0 ? Math.min((value / max) * 100, 100) : 0
  const isOverBudget = value > max

  const getVariantClasses = () => {
    if (isOverBudget) return 'bg-[var(--color-error)]'

    switch (variant) {
      case 'success':
        return 'bg-[var(--color-success)]'
      case 'warning':
        return 'bg-[var(--color-warning)]'
      case 'danger':
        return 'bg-[var(--color-error)]'
      default:
        return 'bg-[var(--color-accent-teal)]'
    }
  }

  return (
    <div className={cn('space-y-2', className)}>
      {(label || showPercentage) && (
        <div className="flex items-center justify-between text-sm">
          {label && (
            <span className="font-medium text-[var(--color-text-primary)]">
              {label}
            </span>
          )}
          {showPercentage && (
            <span className={cn(
              'text-xs font-medium',
              isOverBudget
                ? 'text-[var(--color-error)]'
                : 'text-[var(--color-text-secondary)]'
            )}>
              {percentage.toFixed(0)}%
              {isOverBudget && ' (Over Budget)'}
            </span>
          )}
        </div>
      )}
      
      <div className="h-2 w-full rounded-full bg-[rgba(255,255,255,0.08)]">
        <div 
          className={cn(
            'h-2 rounded-full transition-all duration-300',
            getVariantClasses()
          )}
          style={{ width: `${Math.min(percentage, 100)}%` }}
          role="progressbar"
          aria-valuenow={value}
          aria-valuemin={0}
          aria-valuemax={max}
          aria-label={label || 'Progress'}
        />
      </div>
      
      <div className="flex justify-between text-xs text-[var(--color-text-secondary)]">
        <span>{formatValue(value)}</span>
        <span>{formatValue(max)}</span>
      </div>
    </div>
  )
}
