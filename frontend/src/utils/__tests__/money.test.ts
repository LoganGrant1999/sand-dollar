import { centsToDollars, formatCurrency, percent, toNumber } from '../money'

describe('money utilities', () => {
  describe('centsToDollars', () => {
    it('converts cents to dollars correctly', () => {
      expect(centsToDollars(210913)).toBe(2109.13)
      expect(centsToDollars(0)).toBe(0)
      expect(centsToDollars(100)).toBe(1)
      expect(centsToDollars(50)).toBe(0.5)
    })

    it('handles null and undefined values', () => {
      expect(centsToDollars(null)).toBe(0)
      expect(centsToDollars(undefined)).toBe(0)
    })

    it('handles non-number inputs', () => {
      expect(centsToDollars('123' as any)).toBe(0)
      expect(centsToDollars({} as any)).toBe(0)
    })
  })

  describe('formatCurrency', () => {
    it('formats currency with default USD', () => {
      expect(formatCurrency(2109.13)).toBe('$2,109.13')
      expect(formatCurrency(0)).toBe('$0')
      expect(formatCurrency(1)).toBe('$1')
      expect(formatCurrency(1.5)).toBe('$1.5')
      expect(formatCurrency(1.99)).toBe('$1.99')
    })

    it('formats currency with cents when needed', () => {
      expect(formatCurrency(1.01)).toBe('$1.01')
      expect(formatCurrency(1.50)).toBe('$1.5')
      expect(formatCurrency(999.99)).toBe('$999.99')
    })

    it('handles different currency codes', () => {
      expect(formatCurrency(100, 'EUR')).toBe('€100')
      expect(formatCurrency(100, 'GBP')).toBe('£100')
    })

    it('handles negative amounts', () => {
      expect(formatCurrency(-100)).toBe('-$100')
      expect(formatCurrency(-1.50)).toBe('-$1.5')
    })
  })

  describe('percent', () => {
    it('calculates percentage correctly', () => {
      expect(percent(50, 100)).toBe(50)
      expect(percent(1, 4)).toBe(25)
      expect(percent(3, 4)).toBe(75)
      expect(percent(100, 100)).toBe(100)
    })

    it('caps percentage at 100', () => {
      expect(percent(150, 100)).toBe(100)
      expect(percent(200, 100)).toBe(100)
    })

    it('floors percentage at 0', () => {
      expect(percent(-50, 100)).toBe(0)
      expect(percent(50, -100)).toBe(0)
    })

    it('handles edge cases', () => {
      expect(percent(0, 100)).toBe(0)
      expect(percent(100, 0)).toBe(0)
      expect(percent(Infinity, 100)).toBe(0)
      expect(percent(100, Infinity)).toBe(0)
      expect(percent(NaN, 100)).toBe(0)
      expect(percent(100, NaN)).toBe(0)
    })
  })

  describe('toNumber', () => {
    it('passes through numbers unchanged', () => {
      expect(toNumber(123)).toBe(123)
      expect(toNumber(0)).toBe(0)
      expect(toNumber(-456.78)).toBe(-456.78)
    })

    it('converts string numbers', () => {
      expect(toNumber('123')).toBe(123)
      expect(toNumber('456.78')).toBe(456.78)
      expect(toNumber('-123.45')).toBe(-123.45)
      expect(toNumber('0')).toBe(0)
    })

    it('handles invalid strings', () => {
      expect(toNumber('abc')).toBe(0)
      expect(toNumber('')).toBe(0)
      expect(toNumber('123abc')).toBe(123)
    })

    it('converts objects with toString method', () => {
      const obj = { toString: () => '123.45' }
      expect(toNumber(obj)).toBe(123.45)

      const badObj = { toString: () => 'abc' }
      expect(toNumber(badObj)).toBe(0)
    })

    it('handles null and undefined', () => {
      expect(toNumber(null)).toBe(0)
      expect(toNumber(undefined)).toBe(0)
    })

    it('handles other types', () => {
      expect(toNumber(true)).toBe(0)
      expect(toNumber(false)).toBe(0)
      expect(toNumber([])).toBe(0)
      expect(toNumber({})).toBe(0)
    })
  })
})