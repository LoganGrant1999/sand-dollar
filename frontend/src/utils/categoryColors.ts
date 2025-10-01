import { clsx } from 'clsx'
import { rechartsColors } from '@/styles/palette.sunset'

const CATEGORY_COLORS = rechartsColors

const TAILWIND_COLOR_CLASSES = [
  'primary',
  'secondary',
  'accent',
  'secondary',
  'primary',
  'muted',
  'accent',
  'foreground',
] as const

function hashString(str: string): number {
  let hash = 0
  for (let i = 0; i < str.length; i++) {
    const char = str.charCodeAt(i)
    hash = ((hash << 5) - hash) + char
    hash = hash & hash
  }
  return Math.abs(hash)
}

export function getDeterministicCategoryColor(category: string): {
  hex: string
  tailwindClass: string
  index: number
} {
  const hash = hashString(category.toLowerCase().trim())
  const index = hash % CATEGORY_COLORS.length

  return {
    hex: CATEGORY_COLORS[index],
    tailwindClass: TAILWIND_COLOR_CLASSES[index],
    index
  }
}

export function getCategoryColorClasses(category: string) {
  const { tailwindClass } = getDeterministicCategoryColor(category)

  return {
    bg: `bg-${tailwindClass}`,
    text: `text-${tailwindClass}`,
    border: `border-${tailwindClass}`,
    bgLight: `bg-${tailwindClass}/10`,
    bgLighter: `bg-${tailwindClass}/5`,
    borderLight: `border-${tailwindClass}/20`,
    ring: `ring-${tailwindClass}/30`,
  }
}

export function checkColorContrast(foregroundHex: string, backgroundHex: string): {
  ratio: number
  wcagAA: boolean
  wcagAAA: boolean
} {
  function hexToRgb(hex: string) {
    const result = /^#?([a-f\d]{2})([a-f\d]{2})([a-f\d]{2})$/i.exec(hex)
    return result ? {
      r: parseInt(result[1], 16),
      g: parseInt(result[2], 16),
      b: parseInt(result[3], 16)
    } : null
  }

  function getLuminance(rgb: { r: number; g: number; b: number }) {
    const { r, g, b } = rgb
    const [rs, gs, bs] = [r, g, b].map(c => {
      const srgb = c / 255
      return srgb <= 0.03928 ? srgb / 12.92 : Math.pow((srgb + 0.055) / 1.055, 2.4)
    })
    return 0.2126 * rs + 0.7152 * gs + 0.0722 * bs
  }

  const fgRgb = hexToRgb(foregroundHex)
  const bgRgb = hexToRgb(backgroundHex)

  if (!fgRgb || !bgRgb) {
    return { ratio: 0, wcagAA: false, wcagAAA: false }
  }

  const fgLuminance = getLuminance(fgRgb)
  const bgLuminance = getLuminance(bgRgb)

  const brightest = Math.max(fgLuminance, bgLuminance)
  const darkest = Math.min(fgLuminance, bgLuminance)

  const ratio = (brightest + 0.05) / (darkest + 0.05)

  return {
    ratio,
    wcagAA: ratio >= 4.5,
    wcagAAA: ratio >= 7
  }
}

export function getCategoryBadgeClasses(category: string, variant: 'solid' | 'outline' | 'soft' = 'soft'): string {
  const colorClasses = getCategoryColorClasses(category)
  const { hex } = getDeterministicCategoryColor(category)

  const whiteContrast = checkColorContrast('#FFFFFF', hex)
  const darkContrast = checkColorContrast('#2D3748', hex)

  const textColor = whiteContrast.wcagAA ? 'text-white' : 'text-gray-900'

  switch (variant) {
    case 'solid':
      return clsx(
        colorClasses.bg,
        textColor,
        'px-2 py-1 text-xs font-medium rounded-full'
      )
    case 'outline':
      return clsx(
        colorClasses.border,
        colorClasses.text,
        'border px-2 py-1 text-xs font-medium rounded-full bg-transparent'
      )
    case 'soft':
    default:
      return clsx(
        colorClasses.bgLight,
        colorClasses.text,
        'px-2 py-1 text-xs font-medium rounded-full'
      )
  }
}

export function getCategoryCardClasses(category: string): string {
  const colorClasses = getCategoryColorClasses(category)

  return clsx(
    colorClasses.bgLighter,
    colorClasses.borderLight,
    'border rounded-lg p-4 transition-colors hover:bg-opacity-80'
  )
}

type CategoryColorMap = Record<string, string>

const CATEGORY_OVERRIDES: CategoryColorMap = {
  'Entertainment': 'secondary',
  'Healthcare': 'accent',
  'Groceries': 'primary',
  'Education': 'secondary',
  'Software': 'accent',
  'Dining': 'primary',
  'Subscriptions': 'secondary',
  'Shopping': 'accent',
  'Transport': 'primary',
}

export function getCategoryColorWithOverrides(category: string): {
  hex: string
  tailwindClass: string
  index: number
} {
  const override = CATEGORY_OVERRIDES[category]

  if (override) {
    const index = TAILWIND_COLOR_CLASSES.indexOf(override as any)
    return {
      hex: CATEGORY_COLORS[index] || CATEGORY_COLORS[0],
      tailwindClass: override,
      index: index >= 0 ? index : 0
    }
  }

  return getDeterministicCategoryColor(category)
}