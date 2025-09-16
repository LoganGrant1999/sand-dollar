import { cn } from '@/lib/utils'

interface SkeletonProps {
  className?: string
}

export function Skeleton({ className }: SkeletonProps) {
  return (
    <div
      className={cn(
        'animate-pulse rounded-[1rem] bg-[rgba(255,255,255,0.08)]',
        className
      )}
    />
  )
}
