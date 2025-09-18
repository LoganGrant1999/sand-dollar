import * as React from "react"
import { cn } from "@/lib/utils"

export interface BadgeProps extends React.HTMLAttributes<HTMLDivElement> {
  variant?: "default" | "secondary" | "destructive" | "outline"
}

function Badge({ className, variant = "default", ...props }: BadgeProps) {
  return (
    <div
      className={cn(
        "inline-flex items-center rounded-full border px-2.5 py-0.5 text-xs font-semibold transition-colors focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2",
        {
          "border-transparent bg-[var(--color-accent-teal)] text-[var(--color-bg-dark)] hover:bg-[var(--color-accent-blue)]":
            variant === "default",
          "border-transparent bg-[var(--color-panel-dark)] text-[var(--color-text-primary)] hover:bg-[var(--color-panel-dark)]/80":
            variant === "secondary",
          "border-transparent bg-[var(--color-error)] text-[var(--color-bg-dark)] hover:bg-[var(--color-error)]/90":
            variant === "destructive",
          "border border-[var(--color-accent-teal)] text-[var(--color-accent-teal)]":
            variant === "outline",
        },
        className
      )}
      {...props}
    />
  )
}

export { Badge }
