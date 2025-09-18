import { Loader2 } from 'lucide-react'
import { cn } from '@/lib/utils'

interface SpinnerProps {
  size?: 'sm' | 'md' | 'lg'
  className?: string
  text?: string
}

export function Spinner({ size = 'md', className, text }: SpinnerProps) {
  const sizeClasses = {
    sm: 'h-4 w-4',
    md: 'h-6 w-6', 
    lg: 'h-8 w-8'
  }

  return (
    <div className={cn('flex items-center justify-center space-x-2', className)}>
      <Loader2 className={cn('animate-spin text-[var(--color-accent-teal)]', sizeClasses[size])} />
      {text && (
        <span className="text-sm text-[var(--color-text-secondary)] animate-pulse">{text}</span>
      )}
    </div>
  )
}
