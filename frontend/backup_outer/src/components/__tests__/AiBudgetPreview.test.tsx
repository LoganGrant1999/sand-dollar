import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import AiBudgetPreview from '../AiBudgetPreview'
import type { GenerateBudgetResponse, CategoryActual } from '@/types'

describe('AiBudgetPreview', () => {
  it('allows inline target edits before acceptance', async () => {
    const user = userEvent.setup()
    const onAccept = vi.fn()

    const budget: GenerateBudgetResponse = {
      month: '2025-01',
      targetsByCategory: [
        { category: 'Dining', target: 300, reason: 'Cap dining' },
      ],
      summary: {
        savingsRate: 0.2,
        notes: ['Keep savings momentum'],
      },
      promptTokens: 0,
      completionTokens: 0,
    }

    const actuals: CategoryActual[] = [
      { category: 'Dining', actual: 280 },
    ]

    render(
      <AiBudgetPreview
        budgetData={budget}
        month="2025-01"
        onBack={() => {}}
        onAccept={onAccept}
        actualsByCategory={actuals}
      />
    )

    const adjustButton = screen.getByRole('button', { name: /adjust/i })
    await user.click(adjustButton)

    const input = screen.getByLabelText(/new target/i)
    await user.clear(input)
    await user.type(input, '250')

    // Toggle off edit mode
    await user.click(screen.getByRole('button', { name: /done/i }))

    await user.click(screen.getByRole('button', { name: /accept ai budget/i }))

    expect(onAccept).toHaveBeenCalledTimes(1)
    const accepted = onAccept.mock.lastCall?.[0]
    expect(accepted[0].category).toBe('Dining')
    expect(accepted[0].target).toBe(250)
  })
})
