import { Card, CardContent, CardHeader } from '@/components/ui/card'
import { cn } from '@/lib/utils'

interface CardSkeletonProps {
  variant?: 'summary' | 'default'
  className?: string
}

export function CardSkeleton({ variant = 'default', className }: CardSkeletonProps) {
  if (variant === 'summary') {
    return (
      <div className={cn('grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4', className)}>
        {Array.from({ length: 4 }).map((_, i) => (
          <Card key={i}>
            <CardContent className="p-4">
              <div className="text-center space-y-2">
                <div className="h-3 rounded animate-pulse bg-[rgba(255,255,255,0.1)]"></div>
                <div className="h-6 rounded animate-pulse bg-[rgba(255,255,255,0.1)]"></div>
              </div>
            </CardContent>
          </Card>
        ))}
      </div>
    )
  }

  return (
    <Card className={className}>
      <CardHeader>
        <div className="space-y-2">
          <div className="h-5 w-3/4 rounded animate-pulse bg-[rgba(255,255,255,0.1)]"></div>
          <div className="h-4 w-1/2 rounded animate-pulse bg-[rgba(255,255,255,0.1)]"></div>
        </div>
      </CardHeader>
      <CardContent>
        <div className="space-y-4">
          <div className="h-4 rounded animate-pulse bg-[rgba(255,255,255,0.1)]"></div>
          <div className="h-4 w-5/6 rounded animate-pulse bg-[rgba(255,255,255,0.1)]"></div>
          <div className="h-4 w-4/6 rounded animate-pulse bg-[rgba(255,255,255,0.1)]"></div>
          <div className="space-y-2">
            <div className="h-3 w-3/4 rounded animate-pulse bg-[rgba(255,255,255,0.1)]"></div>
            <div className="h-3 w-2/3 rounded animate-pulse bg-[rgba(255,255,255,0.1)]"></div>
            <div className="h-3 w-4/5 rounded animate-pulse bg-[rgba(255,255,255,0.1)]"></div>
          </div>
        </div>
      </CardContent>
    </Card>
  )
}
