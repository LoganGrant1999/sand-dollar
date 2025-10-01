import { cn } from '@/lib/utils'

export const themePrimitives = {
  text: {
    primary: 'text-sd-text',
    muted: 'text-sd-text-muted',
    white: 'text-sd-white',
    brand: {
      seafoam: 'text-sd-seafoam',
      lavender: 'text-sd-lavender',
      coral: 'text-sd-coral',
      beige: 'text-sd-beige',
    },
    semantic: {
      primary: 'text-sd-primary',
      secondary: 'text-sd-secondary',
      accent: 'text-sd-accent',
      info: 'text-sd-info',
      success: 'text-sd-success',
      warning: 'text-sd-warning',
    },
  },
  bg: {
    primary: 'bg-sd-bg',
    surface: 'bg-sd-surface',
    white: 'bg-sd-white',
    brand: {
      seafoam: 'bg-sd-seafoam',
      lavender: 'bg-sd-lavender',
      coral: 'bg-sd-coral',
      beige: 'bg-sd-beige',
    },
    semantic: {
      primary: 'bg-sd-primary',
      secondary: 'bg-sd-secondary',
      accent: 'bg-sd-accent',
      info: 'bg-sd-info',
      success: 'bg-sd-success',
      warning: 'bg-sd-warning',
    },
  },
  border: {
    primary: 'border-sd-border',
    brand: {
      seafoam: 'border-sd-seafoam',
      lavender: 'border-sd-lavender',
      coral: 'border-sd-coral',
      beige: 'border-sd-beige',
    },
    semantic: {
      primary: 'border-sd-primary',
      secondary: 'border-sd-secondary',
      accent: 'border-sd-accent',
      info: 'border-sd-info',
      success: 'border-sd-success',
      warning: 'border-sd-warning',
    },
  },
}

export const getThemeClasses = {
  text: (variant: keyof typeof themePrimitives.text | keyof typeof themePrimitives.text.brand | keyof typeof themePrimitives.text.semantic, type: 'base' | 'brand' | 'semantic' = 'base') => {
    if (type === 'brand') return themePrimitives.text.brand[variant as keyof typeof themePrimitives.text.brand]
    if (type === 'semantic') return themePrimitives.text.semantic[variant as keyof typeof themePrimitives.text.semantic]
    return themePrimitives.text[variant as keyof typeof themePrimitives.text]
  },
  bg: (variant: keyof typeof themePrimitives.bg | keyof typeof themePrimitives.bg.brand | keyof typeof themePrimitives.bg.semantic, type: 'base' | 'brand' | 'semantic' = 'base') => {
    if (type === 'brand') return themePrimitives.bg.brand[variant as keyof typeof themePrimitives.bg.brand]
    if (type === 'semantic') return themePrimitives.bg.semantic[variant as keyof typeof themePrimitives.bg.semantic]
    return themePrimitives.bg[variant as keyof typeof themePrimitives.bg]
  },
  border: (variant: keyof typeof themePrimitives.border | keyof typeof themePrimitives.border.brand | keyof typeof themePrimitives.border.semantic, type: 'base' | 'brand' | 'semantic' = 'base') => {
    if (type === 'brand') return themePrimitives.border.brand[variant as keyof typeof themePrimitives.border.brand]
    if (type === 'semantic') return themePrimitives.border.semantic[variant as keyof typeof themePrimitives.border.semantic]
    return themePrimitives.border[variant as keyof typeof themePrimitives.border]
  },
}

type ThemeVariant = 'primary' | 'secondary' | 'accent' | 'info' | 'success' | 'warning'

export const Button = {
  variants: {
    primary: cn(
      getThemeClasses.bg('primary', 'semantic'),
      getThemeClasses.text('white'),
      'hover:opacity-90 transition-opacity'
    ),
    secondary: cn(
      getThemeClasses.bg('secondary', 'semantic'),
      getThemeClasses.text('white'),
      'hover:opacity-90 transition-opacity'
    ),
    outline: cn(
      getThemeClasses.border('primary', 'semantic'),
      getThemeClasses.text('primary', 'semantic'),
      'border hover:bg-opacity-10',
      getThemeClasses.bg('primary', 'semantic') + '/10'
    ),
    ghost: cn(
      getThemeClasses.text('primary'),
      'hover:bg-opacity-5',
      getThemeClasses.bg('primary', 'semantic') + '/5'
    ),
  },
}

export const Card = {
  base: cn(
    getThemeClasses.bg('surface'),
    getThemeClasses.border('primary'),
    'border rounded-lg shadow-subtle'
  ),
}

export const Input = {
  base: cn(
    getThemeClasses.bg('surface'),
    getThemeClasses.border('primary'),
    getThemeClasses.text('primary'),
    'border rounded-md px-3 py-2 placeholder:text-sd-text-muted focus:outline-none focus:ring-2 focus:ring-sd-primary focus:border-transparent'
  ),
}

export const Badge = {
  variants: {
    default: cn(
      getThemeClasses.bg('primary', 'semantic'),
      getThemeClasses.text('white'),
      'px-2 py-1 text-xs rounded-full'
    ),
    secondary: cn(
      getThemeClasses.bg('secondary', 'semantic'),
      getThemeClasses.text('white'),
      'px-2 py-1 text-xs rounded-full'
    ),
    success: cn(
      getThemeClasses.bg('success', 'semantic'),
      getThemeClasses.text('white'),
      'px-2 py-1 text-xs rounded-full'
    ),
    warning: cn(
      getThemeClasses.bg('warning', 'semantic'),
      getThemeClasses.text('white'),
      'px-2 py-1 text-xs rounded-full'
    ),
    outline: cn(
      getThemeClasses.border('primary'),
      getThemeClasses.text('primary'),
      'border px-2 py-1 text-xs rounded-full bg-transparent'
    ),
  },
}

export function getCategoryColor(category: string): string {
  const colors = [
    'sd-seafoam',
    'sd-lavender',
    'sd-coral',
    'sd-beige',
    'sd-primary',
    'sd-secondary',
    'sd-success',
    'sd-warning',
  ]

  let hash = 0
  for (let i = 0; i < category.length; i++) {
    const char = category.charCodeAt(i)
    hash = ((hash << 5) - hash) + char
    hash = hash & hash
  }

  const index = Math.abs(hash) % colors.length
  return colors[index]
}

export function getCategoryClasses(category: string) {
  const colorVar = getCategoryColor(category)
  return {
    bg: `bg-${colorVar}`,
    text: `text-${colorVar}`,
    border: `border-${colorVar}`,
    bgLight: `bg-${colorVar}/10`,
    borderLight: `border-${colorVar}/20`,
  }
}