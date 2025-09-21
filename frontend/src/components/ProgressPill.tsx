interface ProgressPillProps {
  current: number // MTD value in cents
  typical: number // 3-mo avg in cents
  labelCurrent: string // e.g. "September to Date"
  labelTypical: string // e.g. "Monthly Avg (past 3 months)"
  format: (cents: number) => string // utility for $ formatting
  color?: "red" | "green" | "blue"
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
      default:
        return "bg-blue-500"
    }
  }

  const getTextColor = () => {
    return pct > 40 ? "white" : "black"
  }

  return (
    <div className="space-y-2">
      {/* Labels */}
      <div className="flex justify-between text-sm text-gray-600 dark:text-gray-400">
        <span>{labelCurrent}: {format(current)}</span>
        <span>{labelTypical}: {format(typical)}</span>
      </div>

      {/* Progress bar */}
      <div className="flex items-center space-x-2 w-full">
        <span className="text-sm font-medium text-gray-900 dark:text-gray-100">
          {format(current)}
        </span>
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
        <span className="text-sm font-medium text-gray-900 dark:text-gray-100">
          {format(typical)}
        </span>
      </div>
    </div>
  )
}