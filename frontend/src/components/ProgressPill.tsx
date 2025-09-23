interface ProgressPillProps {
  current: number // MTD value in cents
  typical: number // 3-mo avg in cents
  labelCurrent: string // e.g. "September to Date"
  labelTypical: string // e.g. "Monthly Avg (past 3 months)"
  format: (cents: number) => string // utility for $ formatting
  color?: "red" | "green" | "blue" | "gray" | string // Allow custom color classes
}

export default function ProgressPill({
  current,
  typical,
  labelCurrent,
  labelTypical,
  format,
  color = "blue"
}: ProgressPillProps) {
  const pct = typical > 0 ? Math.min((current / typical) * 100, 100) : 0
  const actualPct = typical > 0 ? (current / typical) * 100 : 0

  const getColorClass = () => {
    switch (color) {
      case "red":
        return "bg-red-500"
      case "green":
        return "bg-green-500"
      case "blue":
        return "bg-blue-500"
      case "gray":
        return "bg-gray-500"
      default:
        // If it's a custom color string, use it directly (should be a valid Tailwind class)
        return color || "bg-blue-500"
    }
  }

  const getTextColor = () => {
    return "black"
  }

  return (
    <div className="space-y-2">
      {/* Progress bar with integrated labels */}
      <div className="flex items-center space-x-3 w-full">
        <div className="text-sm text-gray-600 dark:text-gray-400 flex-shrink-0 text-center">
          <div className="font-medium">{format(current)}</div>
          <div className="text-xs">{labelCurrent}</div>
        </div>
        <div className="relative flex-1 h-6 rounded-full bg-gray-200 dark:bg-gray-700">
          <div
            className={`h-6 rounded-full ${getColorClass()}`}
            style={{ width: `${pct}%` }}
          />
          <span
            className="absolute inset-0 flex items-center justify-end pr-2 text-sm font-medium"
            style={{ color: getTextColor() }}
          >
            {typical > 0 ? `${Math.round(actualPct)}%` : "—"}
          </span>
        </div>
        <div className="text-sm text-gray-600 dark:text-gray-400 flex-shrink-0 text-center">
          <div className="font-medium">{format(typical)}</div>
          <div className="text-xs">{labelTypical}</div>
        </div>
      </div>
    </div>
  )
}