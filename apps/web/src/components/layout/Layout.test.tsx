import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it } from 'vitest'
import { useAuthStore } from '@fleetmgm/store'
import { Layout } from './Layout'

function renderLayout() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  })

  useAuthStore.getState().login({
    email: 'admin@fleetmgm.com',
    role: 'ADMIN',
    accessToken: 'token',
    refreshToken: 'refresh-token',
  })

  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={['/']}>
        <Routes>
          <Route element={<Layout />}>
            <Route path="/" element={<div>Panel content</div>} />
            <Route path="/vehicles" element={<div>Vehicles content</div>} />
          </Route>
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

describe('Layout — mobile navigation', () => {
  beforeEach(() => {
    useAuthStore.getState().logout()
  })

  it('keeps the mobile nav drawer closed by default', () => {
    renderLayout()

    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
  })

  it('opens the mobile nav drawer when the hamburger button is clicked', async () => {
    const user = userEvent.setup()
    renderLayout()

    await user.click(screen.getByRole('button', { name: /abrir menú/i }))

    expect(screen.getByRole('dialog')).toBeInTheDocument()
  })

  it('closes the drawer after clicking a nav link inside it', async () => {
    const user = userEvent.setup()
    renderLayout()

    await user.click(screen.getByRole('button', { name: /abrir menú/i }))
    const dialog = screen.getByRole('dialog')
    await user.click(within(dialog).getByRole('link', { name: /vehículos/i }))

    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument())
    expect(screen.getByText('Vehicles content')).toBeInTheDocument()
  })

  it('closes the drawer when the close button is clicked', async () => {
    const user = userEvent.setup()
    renderLayout()

    await user.click(screen.getByRole('button', { name: /abrir menú/i }))
    await user.click(screen.getByRole('button', { name: /cerrar menú/i }))

    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument())
  })
})
