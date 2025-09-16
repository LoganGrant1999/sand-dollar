import * as React from "react"
import { cn } from "@/lib/utils"

export interface SliderProps
  extends Omit<React.InputHTMLAttributes<HTMLInputElement>, 'type' | 'value' | 'defaultValue' | 'onChange'> {
  min?: number
  max?: number
  step?: number
  value?: number[]
  defaultValue?: number[]
  onValueChange?: (value: number[]) => void
}

const Slider = React.forwardRef<HTMLInputElement, SliderProps>(
  ({ className, min = 0, max = 100, step = 1, value, defaultValue, onValueChange, ...props }, ref) => {
    const [internalValue, setInternalValue] = React.useState(
      value?.[0] ?? defaultValue?.[0] ?? min
    )

    const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
      const newValue = Number(e.target.value)
      setInternalValue(newValue)
      onValueChange?.([newValue])
    }

    React.useEffect(() => {
      if (value !== undefined) {
        setInternalValue(value[0] ?? min)
      }
    }, [value, min])

    return (
      <div className={cn("relative flex w-full items-center", className)}>
        <input
          ref={ref}
          type="range"
          min={min}
          max={max}
          step={step}
          value={value?.[0] ?? internalValue}
          onChange={handleChange}
          className="h-3 w-full cursor-pointer appearance-none rounded-full bg-[rgba(255,255,255,0.08)] outline-none slider-thumb:h-6 slider-thumb:w-6 slider-thumb:rounded-full slider-thumb:bg-primary slider-thumb:cursor-pointer slider-thumb:border-2 slider-thumb:border-[var(--color-bg-dark)] slider-thumb:transition-all slider-thumb:duration-200 focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50"
          style={{
            background: `linear-gradient(to right, hsl(var(--primary)) 0%, hsl(var(--primary)) ${((value?.[0] ?? internalValue - min) / (max - min)) * 100}%, hsl(var(--secondary)) ${((value?.[0] ?? internalValue - min) / (max - min)) * 100}%, hsl(var(--secondary)) 100%)`
          }}
          {...props}
        />
      </div>
    )
  }
)
Slider.displayName = "Slider"

export { Slider }
