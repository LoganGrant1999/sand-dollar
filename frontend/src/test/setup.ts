import '@testing-library/jest-dom/vitest'

// Silence React Query network error noise in tests.
vi.stubGlobal('IS_REACT_ACT_ENVIRONMENT', true)
