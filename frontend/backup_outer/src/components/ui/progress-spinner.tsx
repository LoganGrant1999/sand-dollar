import { useEffect, useState, useRef } from 'react'
import { cn } from '@/lib/utils'

interface ProgressSpinnerProps {
  size?: 'sm' | 'md' | 'lg'
  className?: string
  text?: string
  progress?: number // 0 to 1
  showProgress?: boolean
  duration?: number // Animation duration in ms
}

export function ProgressSpinner({ 
  size = 'md', 
  className, 
  text,
  progress = 0,
  showProgress = false,
  duration = 1000
}: ProgressSpinnerProps) {
  const [animatedProgress, setAnimatedProgress] = useState(0)
  const animationRef = useRef<number | null>(null)
  const startTimeRef = useRef<number | null>(null)
  const startProgressRef = useRef<number>(0)

  const sizeClasses = {
    sm: 'h-4 w-4',
    md: 'h-6 w-6', 
    lg: 'h-8 w-8'
  }

  const strokeWidth = {
    sm: 2,
    md: 2,
    lg: 3
  }

  const radius = {
    sm: 6,
    md: 10,
    lg: 14
  }

  useEffect(() => {
    if (!showProgress) return

    const animateProgress = (timestamp: number) => {
      if (startTimeRef.current === null) {
        startTimeRef.current = timestamp
        startProgressRef.current = animatedProgress
      }

      const elapsed = timestamp - startTimeRef.current
      const progressDelta = progress - startProgressRef.current
      const easedProgress = easeInOutCubic(Math.min(elapsed / duration, 1))
      
      const newProgress = startProgressRef.current + (progressDelta * easedProgress)
      setAnimatedProgress(newProgress)

      if (elapsed < duration && Math.abs(newProgress - progress) > 0.01) {
        animationRef.current = requestAnimationFrame(animateProgress)
      } else {
        setAnimatedProgress(progress)
        startTimeRef.current = null
      }
    }

    // Start animation
    startTimeRef.current = null
    animationRef.current = requestAnimationFrame(animateProgress)

    return () => {
      if (animationRef.current !== null) {
        cancelAnimationFrame(animationRef.current)
        animationRef.current = null
      }
    }
  }, [progress, duration, showProgress])

  // Easing function for smooth animation
  const easeInOutCubic = (t: number): number => {
    return t < 0.5 ? 4 * t * t * t : 1 - Math.pow(-2 * t + 2, 3) / 2
  }

  if (!showProgress) {
    // Regular spinner
    return (
      <div className={cn('flex items-center justify-center space-x-2', className)}>
        <div
          className={cn(
            'animate-spin rounded-full border-2 border-[rgba(255,255,255,0.08)] border-t-[var(--color-accent-teal)]',
            sizeClasses[size]
          )}
        />
        {text && (
          <span className="text-sm text-[var(--color-text-secondary)] animate-pulse">{text}</span>
        )}
      </div>
    )
  }

  // Progress spinner
  const r = radius[size]
  const circumference = 2 * Math.PI * r
  const offset = circumference - (animatedProgress * circumference)

  return (
    <div className={cn('flex items-center justify-center space-x-3', className)}>
      <div className="relative">
        <svg className={sizeClasses[size]} viewBox="0 0 24 24">
          {/* Background circle */}
          <circle
            cx="12"
            cy="12"
            r={r}
            stroke="currentColor"
            strokeWidth={strokeWidth[size]}
            fill="none"
            className="text-[rgba(255,255,255,0.08)]"
          />
          {/* Progress circle */}
          <circle
            cx="12"
            cy="12"
            r={r}
            stroke="currentColor"
            strokeWidth={strokeWidth[size]}
            fill="none"
            strokeDasharray={circumference}
            strokeDashoffset={offset}
            strokeLinecap="round"
            className="text-[var(--color-accent-blue)] transition-all duration-75"
            style={{
              transform: 'rotate(-90deg)',
              transformOrigin: '50% 50%',
            }}
          />
        </svg>
        {/* Percentage text for larger sizes */}
        {size !== 'sm' && (
          <div className="absolute inset-0 flex items-center justify-center">
            <span className="text-xs font-medium text-[var(--color-accent-blue)]">
              {Math.round(animatedProgress * 100)}%
            </span>
          </div>
        )}
      </div>
      {text && (
        <span className="text-sm text-[var(--color-text-secondary)]">{text}</span>
      )}
    </div>
  )
}
