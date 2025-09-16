import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import GoalsForm from '../GoalsForm'

describe('GoalsForm', () => {
  it('requires at least one goal before submission', async () => {
    const user = userEvent.setup()
    const onNext = vi.fn()

    render(<GoalsForm onNext={onNext} />)

    const submit = screen.getByRole('button', { name: /generate ai budget/i })
    await user.click(submit)

    expect(onNext).not.toHaveBeenCalled()
    expect(screen.getByText(/select at least one goal/i)).toBeInTheDocument()

    const input = screen.getByPlaceholderText(/add custom goal/i)
    await user.type(input, 'Build emergency fund{enter}')

    await user.click(submit)

    expect(onNext).toHaveBeenCalledTimes(1)
    const payload = onNext.mock.lastCall?.[0]
    expect(payload.goals).toContain('Build emergency fund')
    expect(payload.style).toBe('balanced')
  })
})
