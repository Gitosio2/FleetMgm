import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { describe, expect, it } from 'vitest'
import { Landing } from './Landing'

function renderLanding() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  })

  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={['/']}>
        <Routes>
          <Route path="/" element={<Landing />} />
          <Route path="/login" element={<div>Login page</div>} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

describe('Landing', () => {
  it('navigates to /login when the nav "Acceder al login" link is clicked', async () => {
    const user = userEvent.setup()
    renderLanding()

    await user.click(screen.getAllByRole('link', { name: /acceder al login/i })[0]!)

    expect(await screen.findByText('Login page')).toBeInTheDocument()
  })

  it('navigates to /login from the hero CTA', async () => {
    const user = userEvent.setup()
    renderLanding()

    const heroLink = screen.getAllByRole('link', { name: /acceder al login/i })[1]!
    await user.click(heroLink)

    expect(await screen.findByText('Login page')).toBeInTheDocument()
  })

  it('navigates to /login from the footer CTA', async () => {
    const user = userEvent.setup()
    renderLanding()

    const links = screen.getAllByRole('link', { name: /acceder al login/i })
    await user.click(links[links.length - 1]!)

    expect(await screen.findByText('Login page')).toBeInTheDocument()
  })

  it('shows the origin story section', () => {
    renderLanding()

    expect(screen.getByRole('heading', { name: /de un problema real a una plataforma/i })).toBeInTheDocument()
  })
})
