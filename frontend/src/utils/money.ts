/**
 * Money utility functions for Sand Dollar application
 */

/**
 * Convert cents to dollars
 * @param cents - Amount in cents (e.g., 210913)
 * @returns Amount in dollars (e.g., 2109.13)
 */
export const centsToDollars = (cents?: number | null): number => {
  return typeof cents === "number" ? cents / 100 : 0;
};

/**
 * Format a number as currency
 * @param amount - Amount in dollars
 * @param currency - Currency code (default: "USD")
 * @returns Formatted currency string (e.g., "$2,109.13")
 */
export const formatCurrency = (amount: number, currency = "USD"): string => {
  return new Intl.NumberFormat("en-US", {
    style: "currency",
    currency,
    minimumFractionDigits: 0,
    maximumFractionDigits: 2,
  }).format(amount);
};

/**
 * Calculate percentage with safe division
 * @param numerator - The numerator value
 * @param denominator - The denominator value
 * @returns Percentage between 0 and 100, or 0 if invalid inputs
 */
export const percent = (numerator: number, denominator: number): number => {
  if (!isFinite(numerator) || !isFinite(denominator) || denominator <= 0) {
    return 0;
  }
  return Math.min(100, Math.max(0, (numerator / denominator) * 100));
};

/**
 * Convert BigDecimal or string to number safely
 * @param value - Value that might be BigDecimal, string, or number
 * @returns Number value or 0 if invalid
 */
export const toNumber = (value: unknown): number => {
  if (typeof value === 'number') return value;
  if (typeof value === 'string') {
    const parsed = parseFloat(value);
    return Number.isNaN(parsed) ? 0 : parsed;
  }
  if (value instanceof Object && value !== null && 'toString' in value) {
    const parsed = parseFloat((value as { toString(): string }).toString());
    return Number.isNaN(parsed) ? 0 : parsed;
  }
  return 0;
};