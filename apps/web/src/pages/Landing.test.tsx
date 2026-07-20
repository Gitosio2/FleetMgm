import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it } from 'vitest'
import { Landing } from './Landing'

function renderLanding() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  })

  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={['/']}>
        <Landing />
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

describe('Landing', () => {
  it('keeps the login dialog closed by default', () => {
    renderLanding()

    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
  })

  it('opens the login dialog when the nav "Acceder al login" button is clicked', async () => {
    const user = userEvent.setup()
    renderLanding()

    await user.click(screen.getAllByRole('button', { name: /acceder al login/i })[0]!)

    const dialog = await screen.findByRole('dialog')
    expect(dialog).toBeInTheDocument()
    expect(screen.getByLabelText(/correo electrónico/i)).toBeInTheDocument()
  })

  it('opens the login dialog from the hero CTA', async () => {
    const user = userEvent.setup()
    renderLanding()

    const heroButton = screen.getAllByRole('button', { name: /acceder al login/i })[1]!
    await user.click(heroButton)

    expect(await screen.findByRole('dialog')).toBeInTheDocument()
  })

  it('opens the login dialog from the footer CTA', async () => {
    const user = userEvent.setup()
    renderLanding()

    const buttons = screen.getAllByRole('button', { name: /acceder al login/i })
    await user.click(buttons[buttons.length - 1]!)

    expect(await screen.findByRole('dialog')).toBeInTheDocument()
  })

  it('shows the origin story section', () => {
    renderLanding()

    expect(screen.getByRole('heading', { name: /de un problema real a una plataforma/i })).toBeInTheDocument()
  })
})
