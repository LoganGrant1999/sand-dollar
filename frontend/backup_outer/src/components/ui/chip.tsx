import * as React from "react"
import { cva, type VariantProps } from "class-variance-authority"
import { X } from "lucide-react"
import { cn } from "@/lib/utils"

const chipVariants = cva(
  "inline-flex items-center rounded-full border px-3 py-1 text-sm font-medium transition-all duration-200 focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2",
  {
    variants: {
      variant: {
        default:
          "border-transparent bg-[var(--color-accent-teal)] text-[var(--color-bg-dark)] hover:bg-[var(--color-accent-blue)]",
        secondary:
          "border-transparent bg-[var(--color-panel-dark)] text-[var(--color-text-primary)] hover:bg-[var(--color-panel-dark)]/80",
        destructive:
          "border-transparent bg-[var(--color-error)] text-[var(--color-bg-dark)] hover:bg-[var(--color-error)]/90",
        outline:
          "border border-[var(--color-accent-blue)] text-[var(--color-accent-blue)] hover:border-[var(--color-accent-teal)] hover:text-[var(--color-accent-teal)]",
        success:
          "border-transparent bg-[var(--color-success)] text-[var(--color-bg-dark)] hover:bg-[var(--color-success)]/90",
        warning:
          "border-transparent bg-[var(--color-warning)] text-[var(--color-bg-dark)] hover:bg-[var(--color-warning)]/90",
      },
      size: {
        default: "px-3 py-1",
        sm: "px-2 py-0.5 text-xs",
        lg: "px-4 py-2",
      },
    },
    defaultVariants: {
      variant: "default",
      size: "default",
    },
  }
)

export interface ChipProps
  extends React.HTMLAttributes<HTMLDivElement>,
    VariantProps<typeof chipVariants> {
  onRemove?: () => void
  removable?: boolean
}

const Chip = React.forwardRef<HTMLDivElement, ChipProps>(
  ({ className, variant, size, children, onRemove, removable, ...props }, ref) => {
    return (
      <div
        className={cn(chipVariants({ variant, size }), className)}
        ref={ref}
        {...props}
      >
        {children}
        {removable && onRemove && (
          <button
            type="button"
            className="ml-2 -mr-1 flex h-4 w-4 items-center justify-center rounded-full hover:bg-[var(--color-bg-dark)]/40 focus:outline-none focus:ring-1 focus:ring-ring"
            onClick={onRemove}
          >
            <X className="h-3 w-3" />
            <span className="sr-only">Remove</span>
          </button>
        )}
      </div>
    )
  }
)
Chip.displayName = "Chip"

export { Chip, chipVariants }
