import { render, screen } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it } from 'vitest'
import { useAuthStore } from '@fleetmgm/store'
import { Home } from './Home'

function renderHome() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  })

  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={['/']}>
        <Routes>
          <Route path="/" element={<Home />} />
          <Route path="/dashboard" element={<div>Dashboard Home</div>} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

describe('Home', () => {
  beforeEach(() => {
    useAuthStore.getState().logout()
  })

  it('renders the landing page when logged out', async () => {
    renderHome()

    expect(
      await screen.findByRole('heading', { name: /controla tu flota y factura sin fricción/i }),
    ).toBeInTheDocument()
  })

  it('redirects to the dashboard when already authenticated', async () => {
    useAuthStore.getState().login({
      email: 'admin@fleetmgm.com',
      role: 'ADMIN',
      accessToken: 'token',
      refreshToken: 'refresh-token',
    })

    renderHome()

    expect(await screen.findByText('Dashboard Home')).toBeInTheDocument()
    expect(
      screen.queryByRole('heading', { name: /controla tu flota y factura sin fricción/i }),
    ).not.toBeInTheDocument()
  })
})
