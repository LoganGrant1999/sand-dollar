import { Card, CardContent, CardHeader } from '@/components/ui/card'
import { cn } from '@/lib/utils'

interface ListSkeletonProps {
  variant?: 'allocations' | 'default'
  className?: string
  count?: number
}

export function ListSkeleton({ variant = 'default', className, count = 3 }: ListSkeletonProps) {
  if (variant === 'allocations') {
    return (
      <div className={cn('space-y-4', className)}>
        {Array.from({ length: count }).map((_, i) => (
          <div key={i} className="rounded-lg border border-[rgba(255,255,255,0.08)] bg-[rgba(255,255,255,0.02)] p-4">
            <div className="flex items-center justify-between mb-2">
              <div className="h-4 w-1/3 rounded animate-pulse bg-[rgba(255,255,255,0.1)]"></div>
              <div className="text-right space-y-1">
                <div className="h-4 w-20 rounded animate-pulse bg-[rgba(255,255,255,0.1)]"></div>
                <div className="h-3 w-16 rounded animate-pulse bg-[rgba(255,255,255,0.1)]"></div>
              </div>
            </div>
            <div className="mb-2 h-2 w-full animate-pulse rounded-full bg-[rgba(255,255,255,0.1)]"></div>
            <div className="flex justify-between">
              <div className="h-3 w-16 rounded animate-pulse bg-[rgba(255,255,255,0.1)]"></div>
              <div className="h-3 w-20 rounded animate-pulse bg-[rgba(255,255,255,0.1)]"></div>
            </div>
          </div>
        ))}
      </div>
    )
  }

  return (
    <Card className={className}>
      <CardHeader>
        <div className="h-5 w-1/2 rounded animate-pulse bg-[rgba(255,255,255,0.1)]"></div>
        <div className="h-4 w-3/4 rounded animate-pulse bg-[rgba(255,255,255,0.1)]"></div>
      </CardHeader>
      <CardContent>
        <div className="space-y-3">
          {Array.from({ length: count }).map((_, i) => (
            <div key={i} className="flex items-center justify-between rounded border border-[rgba(255,255,255,0.08)] bg-[rgba(255,255,255,0.02)] p-3">
              <div className="space-y-2 flex-1">
                <div className="h-4 w-1/2 rounded animate-pulse bg-[rgba(255,255,255,0.1)]"></div>
                <div className="h-3 w-1/3 rounded animate-pulse bg-[rgba(255,255,255,0.1)]"></div>
              </div>
              <div className="space-y-2">
                <div className="h-4 w-16 rounded animate-pulse bg-[rgba(255,255,255,0.1)]"></div>
                <div className="h-3 w-12 rounded animate-pulse bg-[rgba(255,255,255,0.1)]"></div>
              </div>
            </div>
          ))}
        </div>
      </CardContent>
    </Card>
  )
}
