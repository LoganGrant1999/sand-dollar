import * as React from 'react'
import { cn } from '@/lib/utils'

const Textarea = React.forwardRef<HTMLTextAreaElement, React.TextareaHTMLAttributes<HTMLTextAreaElement>>(
  ({ className, ...props }, ref) => {
    return (
      <textarea
        ref={ref}
        className={cn(
          'min-h-[120px] w-full rounded-[1rem] border border-[rgba(255,255,255,0.08)] bg-[var(--color-input-bg)] px-4 py-3 text-sm text-[var(--color-text-primary)] shadow-sm transition-colors focus:outline-none focus:ring-2 focus:ring-[var(--color-accent-teal)] focus:border-transparent placeholder:text-[var(--color-text-secondary)]',
          className
        )}
        {...props}
      />
    )
  }
)
Textarea.displayName = 'Textarea'

export { Textarea }
