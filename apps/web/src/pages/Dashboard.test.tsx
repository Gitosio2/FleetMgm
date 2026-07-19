import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { http, HttpResponse } from 'msw'
import { afterEach, describe, expect, it } from 'vitest'
import { useAuthStore, type AppRole } from '@fleetmgm/store'
import { server } from '@/mocks/server'
import {
  SEED_CLIENTS,
  SEED_FINANCIAL_SUMMARY,
  SEED_FINANCIAL_TREND,
  SEED_FLEET_SUMMARY,
} from '@/mocks/handlers'
import { formatCurrency } from '@/lib/currency'
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

// "Ingresos" is also used as a label on the "Resumen del mes" card, so the trend chart's
// "Ingresos"/"Gastos" legend text must be scoped to its own <section> to stay unambiguous.
function trendSection() {
  return screen.getByText('Ingresos y gastos').closest('section') as HTMLElement
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

  it('renders the financial summary section with revenue, costs, and upcoming invoice lists', async () => {
    loginAs('ADMIN')
    renderDashboard()

    await screen.findByText('Resumen del mes')

    const summaryCard = cardByTitle('Resumen del mes')
    expect(summaryCard.textContent).toContain('Ingresos')
    expect(summaryCard.textContent).toContain(formatCurrency(SEED_FINANCIAL_SUMMARY.monthlyRevenue))
    expect(summaryCard.textContent).toContain('Gastos')
    expect(summaryCard.textContent).toContain(formatCurrency(SEED_FINANCIAL_SUMMARY.monthlyCosts))

    expect(screen.getByText('Facturas por cobrar')).toBeInTheDocument()
    expect(screen.getByText('Facturas por pagar')).toBeInTheDocument()

    const overdueReceivable = SEED_FINANCIAL_SUMMARY.upcomingReceivables.find((invoice) => invoice.overdue)!
    const notOverdueReceivable = SEED_FINANCIAL_SUMMARY.upcomingReceivables.find((invoice) => !invoice.overdue)!

    const overdueRow = (await screen.findByText(overdueReceivable.number)).closest('li') as HTMLElement
    expect(overdueRow.textContent).toContain('Vencida')

    const notOverdueRow = screen.getByText(notOverdueReceivable.number).closest('li') as HTMLElement
    expect(notOverdueRow.textContent).not.toContain('Vencida')
  })

  it('shows the margin change in green when the current month margin exceeds the previous month', async () => {
    loginAs('ADMIN')
    renderDashboard()

    await screen.findByText('Resumen del mes')
    const summaryCard = cardByTitle('Resumen del mes')

    // fixture: monthlyMargin (9500 - 8420.5 = 1079.50) > previousMonthMargin (900)
    const changeText = within(summaryCard).getByText(/%$/)
    expect(changeText.className).toContain('text-success')
  })

  it('shows the margin change in red when the current month margin is below the previous month', async () => {
    loginAs('ADMIN')
    server.use(
      http.get('/api/v1/reports/financial-summary', () =>
        HttpResponse.json({ ...SEED_FINANCIAL_SUMMARY, previousMonthMargin: 5000 }),
      ),
    )
    renderDashboard()

    await screen.findByText('Resumen del mes')
    const summaryCard = cardByTitle('Resumen del mes')

    const changeText = within(summaryCard).getByText(/%$/)
    expect(changeText.className).toContain('text-error')
  })

  it('shows a neutral color when the current month margin equals the previous month', async () => {
    loginAs('ADMIN')
    const equalMargin = SEED_FINANCIAL_SUMMARY.monthlyRevenue - SEED_FINANCIAL_SUMMARY.monthlyCosts
    server.use(
      http.get('/api/v1/reports/financial-summary', () =>
        HttpResponse.json({ ...SEED_FINANCIAL_SUMMARY, previousMonthMargin: equalMargin }),
      ),
    )
    renderDashboard()

    await screen.findByText('Resumen del mes')
    const summaryCard = cardByTitle('Resumen del mes')

    const changeText = within(summaryCard).getByText('0.0%')
    expect(changeText.className).toContain('text-on-surface-variant')
  })

  it('shows the euro difference instead of a percentage when the previous month margin was zero', async () => {
    loginAs('ADMIN')
    server.use(
      http.get('/api/v1/reports/financial-summary', () =>
        HttpResponse.json({ ...SEED_FINANCIAL_SUMMARY, previousMonthMargin: 0 }),
      ),
    )
    renderDashboard()

    await screen.findByText('Resumen del mes')
    const summaryCard = cardByTitle('Resumen del mes')

    const monthlyMargin = SEED_FINANCIAL_SUMMARY.monthlyRevenue - SEED_FINANCIAL_SUMMARY.monthlyCosts
    expect(summaryCard.textContent).toContain(`+${formatCurrency(monthlyMargin)}`)
  })

  it('opens a read-only client modal from the "Facturas por cobrar" list', async () => {
    const user = userEvent.setup()
    loginAs('ADMIN')
    renderDashboard()

    const receivable = SEED_FINANCIAL_SUMMARY.upcomingReceivables[0]!
    const client = SEED_CLIENTS.find((c) => c.id === receivable.counterpartyId)!

    await user.click(await screen.findByRole('button', { name: receivable.counterparty }))

    const dialog = await screen.findByRole('dialog')
    expect(within(dialog).getByRole('heading', { name: 'Datos del cliente' })).toBeInTheDocument()
    expect(within(dialog).getByLabelText(/teléfono/i)).toHaveValue(client.phone)
  })

  it('renders the fleet-wide monthly Ingresos/Gastos trend, defaulting to the last 6 months', async () => {
    loginAs('ADMIN')
    renderDashboard()

    await screen.findByText('Ingresos y gastos')

    // Chart legend uses "Gastos" (not "Costes") for outflow in this context. Scoped to the trend
    // section since "Ingresos" is also a label on the "Resumen del mes" card.
    const chart = trendSection()
    expect(await within(chart).findByText('Ingresos')).toBeInTheDocument()
    expect(within(chart).getByText('Gastos')).toBeInTheDocument()

    const last6Months = SEED_FINANCIAL_TREND.slice(-6)
    const totalRevenue = last6Months.reduce((sum, m) => sum + m.revenue, 0)
    const totalCosts = last6Months.reduce((sum, m) => sum + m.costs, 0)
    const totalMargin = totalRevenue - totalCosts

    const revenueCard = cardByTitle('Ingresos totales')
    expect(revenueCard.textContent).toContain(formatCurrency(totalRevenue))

    const costsCard = cardByTitle('Gastos totales')
    expect(costsCard.textContent).toContain(formatCurrency(totalCosts))

    const marginCard = cardByTitle('Margen total')
    expect(marginCard.textContent).toContain(formatCurrency(totalMargin))
  })

  it('recomputes the summary totals when the months selector changes to 3 months', async () => {
    loginAs('ADMIN')
    const user = userEvent.setup()
    renderDashboard()

    await screen.findByText('Ingresos y gastos')
    await within(trendSection()).findByText('Ingresos')

    await user.click(screen.getByRole('button', { name: '3 meses' }))

    const last3Months = SEED_FINANCIAL_TREND.slice(-3)
    const totalRevenue = last3Months.reduce((sum, m) => sum + m.revenue, 0)
    const totalCosts = last3Months.reduce((sum, m) => sum + m.costs, 0)
    const totalMargin = totalRevenue - totalCosts

    const revenueCard = cardByTitle('Ingresos totales')
    await waitFor(() => expect(revenueCard.textContent).toContain(formatCurrency(totalRevenue)))

    const costsCard = cardByTitle('Gastos totales')
    expect(costsCard.textContent).toContain(formatCurrency(totalCosts))

    const marginCard = cardByTitle('Margen total')
    expect(marginCard.textContent).toContain(formatCurrency(totalMargin))
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
