import { render, screen } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { afterEach, describe, expect, it } from 'vitest'
import { useAuthStore, type AppRole } from '@fleetmgm/store'
import { SEED_FINANCIAL_SUMMARY, SEED_FLEET_SUMMARY } from '@/mocks/handlers'
import { Dashboard } from './Dashboard'
import { DashboardHome } from './DashboardHome'

function renderDashboard() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  })

  return render(
    <QueryClientProvider client={queryClient}>
      <Dashboard />
    </QueryClientProvider>,
  )
}

function renderDashboardHomeAt(initialPath: string) {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  })

  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={[initialPath]}>
        <Routes>
          <Route path="/" element={<DashboardHome />} />
          <Route path="/jobs" element={<div>Jobs Page</div>} />
          <Route path="/workshop" element={<div>Workshop Page</div>} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

function loginAs(role: AppRole) {
  useAuthStore.getState().login({
    email: 'user@fleetmgm.com',
    role,
    accessToken: 'token',
    refreshToken: 'refresh',
  })
}

// Numbers in the KPI cards are split across sibling text/element nodes for styling (e.g. "12"
// followed by a <span> with " / 15"), so a plain screen.getByText('12') can match more than one
// ancestor with identical textContent. Scoping to the card and asserting on its full textContent
// sidesteps that ambiguity entirely.
function cardByTitle(title: string) {
  return screen.getByText(title).closest('[data-slot="card"]') as HTMLElement
}

describe('Dashboard', () => {
  afterEach(() => {
    useAuthStore.getState().logout()
  })

  it('renders the 3 fleet KPI cards with the fetched summary', async () => {
    loginAs('ADMIN')
    renderDashboard()

    await screen.findByText('Vehículos activos')

    const activeCard = cardByTitle('Vehículos activos')
    expect(activeCard.textContent).toContain(String(SEED_FLEET_SUMMARY.activeVehicles))
    expect(activeCard.textContent).toContain(String(SEED_FLEET_SUMMARY.totalVehicles))

    const workshopCard = cardByTitle('En taller')
    expect(workshopCard.textContent).toContain(String(SEED_FLEET_SUMMARY.inWorkshop))

    const maintenanceCard = cardByTitle('Mantenimiento pendiente')
    expect(maintenanceCard.textContent).toContain(String(SEED_FLEET_SUMMARY.pendingMaintenance))
    expect(maintenanceCard.textContent).toContain('vencen en 48 horas')
  })

  it('renders the financial summary section with monthly costs and upcoming invoice lists', async () => {
    loginAs('ADMIN')
    renderDashboard()

    await screen.findByText('Costes del mes')

    const costsCard = cardByTitle('Costes del mes')
    expect(costsCard.textContent).toContain(SEED_FINANCIAL_SUMMARY.monthlyCosts.toFixed(2))

    expect(screen.getByText('Facturas por cobrar')).toBeInTheDocument()
    expect(screen.getByText('Facturas por pagar')).toBeInTheDocument()

    const overdueReceivable = SEED_FINANCIAL_SUMMARY.upcomingReceivables.find((invoice) => invoice.overdue)!
    const notOverdueReceivable = SEED_FINANCIAL_SUMMARY.upcomingReceivables.find((invoice) => !invoice.overdue)!

    const overdueRow = (await screen.findByText(overdueReceivable.number)).closest('li') as HTMLElement
    expect(overdueRow.textContent).toContain('Vencida')

    const notOverdueRow = screen.getByText(notOverdueReceivable.number).closest('li') as HTMLElement
    expect(notOverdueRow.textContent).not.toContain('Vencida')
  })

  it('redirects a DRIVER landing on "/" to /jobs instead of showing the dashboard', () => {
    loginAs('DRIVER')
    renderDashboardHomeAt('/')

    expect(screen.getByText('Jobs Page')).toBeInTheDocument()
    expect(screen.queryByText('Vehículos activos')).not.toBeInTheDocument()
  })

  it('redirects a WORKSHOP_STAFF landing on "/" to /workshop instead of showing the dashboard', () => {
    loginAs('WORKSHOP_STAFF')
    renderDashboardHomeAt('/')

    expect(screen.getByText('Workshop Page')).toBeInTheDocument()
    expect(screen.queryByText('Vehículos activos')).not.toBeInTheDocument()
  })

  it('renders the dashboard content for a MANAGER landing on "/"', async () => {
    loginAs('MANAGER')
    renderDashboardHomeAt('/')

    expect(await screen.findByText('Vehículos activos')).toBeInTheDocument()
  })
})
