import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClient } from '@tanstack/react-query'
import { createMemoryHistory } from 'history'
import { Route, Router, Routes } from 'react-router-dom'
import Budgeting from '../Budgeting'
import { renderWithProviders } from '@/test/utils'
import type { FinancialSnapshotResponse, GenerateBudgetResponse } from '@/types'

vi.mock('@/components/GoalsForm', () => ({
  default: ({ onNext }: { onNext: (data: any) => void }) => (
    <button onClick={() => onNext({
      goals: ['Emergency fund'],
      style: 'balanced',
      mustKeepCategories: [],
      categoryCaps: {},
      notes: 'demo'
    })}>
      Submit Goals
    </button>
  ),
}))

vi.mock('@/components/AiBudgetPreview', () => ({
  default: ({ onAccept }: { onAccept: (targets: any) => void }) => (
    <div>
      <p>Review Step</p>
      <button onClick={() => onAccept([{ category: 'Dining', target: 250, reason: 'demo' }])}>
        Accept Plan
      </button>
    </div>
  ),
}))

const snapshotMock = vi.fn()
const generateMock = vi.fn()
const acceptMock = vi.fn()

vi.mock('@/hooks/useAiBudget', () => ({
  useBudgetSnapshot: () => snapshotMock(),
  useGenerateAiBudget: () => generateMock(),
  useAcceptAiBudget: () => acceptMock(),
  AI_BUDGET_QUERY_KEY: ['ai-budget', 'snapshot'],
}))

beforeEach(() => {
  vi.clearAllMocks()
})

describe('Budgeting AI wizard flow', () => {
  it('advances from snapshot to review and accepts plan', async () => {
    const user = userEvent.setup()
    const snapshot: FinancialSnapshotResponse = {
      month: '2025-01',
      income: 6200,
      actualsByCategory: [],
      totals: {
        expenses: 1500,
        savings: 200,
        netCashFlow: 200,
      },
      targetsByCategory: [],
      acceptedAt: null,
    }

    snapshotMock.mockReturnValue({
      data: snapshot,
      isLoading: false,
      error: null,
      refetch: vi.fn(),
    })

    const generated: GenerateBudgetResponse = {
      month: '2025-01',
      targetsByCategory: [{ category: 'Dining', target: 300, reason: 'cap' }],
      summary: { savingsRate: 0.2, notes: [] },
      promptTokens: 0,
      completionTokens: 0,
    }

    let generateCalls = 0
    let acceptCalls = 0

    generateMock.mockReturnValue({
      mutate: (_payload: any, opts?: any) => {
        generateCalls += 1
        opts?.onSuccess?.(generated)
      },
    })

    acceptMock.mockReturnValue({
      mutate: (_payload: any, opts?: any) => {
        acceptCalls += 1
        opts?.onSuccess?.()
      },
    })

    const history = createMemoryHistory({ initialEntries: ['/budgeting/ai/snapshot'] })
    const queryClient = new QueryClient()

    renderWithProviders(
      <Router location={history.location} navigator={history}>
        <Routes>
          <Route path="/budgeting/*" element={<Budgeting />} />
        </Routes>
      </Router>,
      { queryClient }
    )

    await user.click(screen.getByRole('button', { name: /continue to goals/i }))
    expect(history.location.pathname).toBe('/budgeting/ai/goals')

    await user.click(screen.getByRole('button', { name: /submit goals/i }))
    expect(generateCalls).toBe(1)
    expect(history.location.pathname).toBe('/budgeting/ai/review')

    await user.click(screen.getByRole('button', { name: /accept plan/i }))
    expect(acceptCalls).toBe(1)
    expect(history.location.pathname).toBe('/budgeting')
  })
})
