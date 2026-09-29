import type { ReactNode } from 'react'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { http, HttpResponse } from 'msw'
import { beforeEach, describe, expect, it } from 'vitest'
import { useAuthStore, type AppRole } from '@fleetmgm/store'
import { GPS_MOCK_WINDOW_MINUTES, resetGpsMock } from '@/mocks/handlers'
import { server } from '@/mocks/server'
import { GpsMockToggle } from './GpsMockToggle'

function renderWithClient(ui: ReactNode) {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  })

  return render(<QueryClientProvider client={queryClient}>{ui}</QueryClientProvider>)
}

function loginAs(role: AppRole) {
  useAuthStore.getState().login({
    email: `${role.toLowerCase()}@fleetmgm.com`,
    role,
    accessToken: 'token',
    refreshToken: 'refresh',
  })
}

describe('GpsMockToggle', () => {
  beforeEach(() => {
    resetGpsMock()
    useAuthStore.getState().logout()
  })

  it('shows the generator off by default, so a fresh deployment consumes nothing', async () => {
    loginAs('ADMIN')
    renderWithClient(<GpsMockToggle />)

    const toggle = await screen.findByRole('switch', { name: /simulación gps/i })
    await waitFor(() => expect(toggle).not.toBeChecked())
    expect(screen.getByText(/apagada para no consumir recursos/i)).toBeInTheDocument()
  })

  it('turns the generator on and reports when it will switch itself off', async () => {
    loginAs('ADMIN')
    const user = userEvent.setup()
    renderWithClient(<GpsMockToggle />)

    const toggle = await screen.findByRole('switch', { name: /simulación gps/i })
    await waitFor(() => expect(toggle).toBeEnabled())
    await user.click(toggle)

    await waitFor(() => expect(toggle).toBeChecked())
    expect(
      await screen.findByText(
        new RegExp(`se apaga sola en ${GPS_MOCK_WINDOW_MINUTES} min`, 'i'),
      ),
    ).toBeInTheDocument()
  })

  it('turns the generator back off', async () => {
    loginAs('ADMIN')
    const user = userEvent.setup()
    renderWithClient(<GpsMockToggle />)

    const toggle = await screen.findByRole('switch', { name: /simulación gps/i })
    await waitFor(() => expect(toggle).toBeEnabled())
    await user.click(toggle)
    await waitFor(() => expect(toggle).toBeChecked())

    await user.click(toggle)

    await waitFor(() => expect(toggle).not.toBeChecked())
    expect(screen.getByText(/apagada para no consumir recursos/i)).toBeInTheDocument()
  })

  it('lets ADMINISTRATIVE see the state but not change it', async () => {
    loginAs('ADMINISTRATIVE')
    renderWithClient(<GpsMockToggle />)

    const toggle = await screen.findByRole('switch', { name: /simulación gps/i })
    await waitFor(() => expect(toggle).toBeDisabled())
    expect(screen.getByText(/solo un admin o manager puede activarla/i)).toBeInTheDocument()
  })

  it('reports a failed toggle instead of showing a state the server never accepted', async () => {
    loginAs('ADMIN')
    const user = userEvent.setup()
    server.use(
      http.patch('/api/v1/gps/mock', () => new HttpResponse(null, { status: 500 })),
    )
    renderWithClient(<GpsMockToggle />)

    const toggle = await screen.findByRole('switch', { name: /simulación gps/i })
    await waitFor(() => expect(toggle).toBeEnabled())
    await user.click(toggle)

    expect(await screen.findByRole('alert')).toHaveTextContent(/no se pudo cambiar/i)
    await waitFor(() => expect(toggle).not.toBeChecked())
  })

  // The switch exists to stop background cost; a status query that polls would reintroduce it by
  // keeping the backend awake for as long as the page stays open.
  it('does not poll the status endpoint', async () => {
    loginAs('ADMIN')
    let statusRequests = 0
    server.events.on('request:start', ({ request }) => {
      const url = new URL(request.url)
      if (url.pathname === '/api/v1/gps/mock' && request.method === 'GET') {
        statusRequests++
      }
    })

    renderWithClient(<GpsMockToggle />)

    await screen.findByText(/apagada para no consumir recursos/i)
    await new Promise((resolve) => setTimeout(resolve, 50))

    expect(statusRequests).toBe(1)
  })
})
