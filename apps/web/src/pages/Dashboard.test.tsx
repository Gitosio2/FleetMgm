import { render, renderHook, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { http, HttpResponse } from 'msw'
import { afterEach, describe, expect, it } from 'vitest'
import { useAuthStore, type AppRole } from '@fleetmgm/store'
import { useIssueInvoice } from '@fleetmgm/hooks'
import { server } from '@/mocks/server'
import {
  SEED_CLIENTS,
  SEED_FINANCIAL_SUMMARY,
  SEED_FINANCIAL_TREND,
  SEED_FLEET_SUMMARY,
  SEED_INVOICES,
  SEED_SUPPLIER_INVOICES,
} from '@/mocks/handlers'
import { formatCurrency } from '@/lib/currency'
import { Dashboard } from './Dashboard'
import { DashboardHome } from './DashboardHome'

function renderDashboard() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  })

  const view = render(
    <QueryClientProvider client={queryClient}>
      <Dashboard />
    </QueryClientProvider>,
  )

  return { ...view, queryClient }
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
    expect(summaryCard.textContent).toContain('Cobros')
    expect(summaryCard.textContent).toContain(formatCurrency(SEED_FINANCIAL_SUMMARY.monthlyCollections))

    expect(screen.getByText('Facturas por cobrar')).toBeInTheDocument()
    expect(screen.getByText('Facturas por pagar')).toBeInTheDocument()

    const overdueReceivable = SEED_FINANCIAL_SUMMARY.upcomingReceivables.find((invoice) => invoice.overdue)!
    const notOverdueReceivable = SEED_FINANCIAL_SUMMARY.upcomingReceivables.find((invoice) => !invoice.overdue)!

    const overdueRow = (await screen.findByText(overdueReceivable.number)).closest('li') as HTMLElement
    expect(overdueRow.textContent).toContain('Vencida')

    const notOverdueRow = screen.getByText(notOverdueReceivable.number).closest('li') as HTMLElement
    expect(notOverdueRow.textContent).not.toContain('Vencida')
  })

  it('explains what each figure counts when the "Resumen del mes" info icon is focused', async () => {
    loginAs('ADMIN')
    renderDashboard()

    await screen.findByText('Resumen del mes')
    const infoTrigger = screen.getByRole('button', { name: /qué se cuenta en este resumen/i })
    // Focus (not hover) opens the tooltip immediately in Radix — no artificial delay to wait out.
    infoTrigger.focus()

    // Radix renders the tooltip content twice (a positioned copy plus a visually-hidden
    // accessibility copy) — at least one match confirms the explanation is shown.
    const matches = await screen.findAllByText(/facturas a clientes emitidas este mes/i)
    expect(matches.length).toBeGreaterThan(0)
  })

  it('refetches the financial summary after an invoice is issued elsewhere in the app', async () => {
    loginAs('ADMIN')
    let requestCount = 0
    server.use(
      http.get('/api/v1/reports/financial-summary', () => {
        requestCount += 1
        return HttpResponse.json(SEED_FINANCIAL_SUMMARY)
      }),
    )
    const { queryClient } = renderDashboard()

    await screen.findByText('Resumen del mes')
    expect(requestCount).toBe(1)

    // Issuing lives on the Billing page, not the Dashboard — this simulates that mutation firing
    // while the Dashboard is mounted elsewhere in the app, sharing the same QueryClient cache.
    const draft = SEED_INVOICES[0]!
    const { result } = renderHook(() => useIssueInvoice(), {
      wrapper: ({ children }) => <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>,
    })
    result.current.mutate(draft.id)

    await waitFor(() => expect(requestCount).toBe(2))
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

  it('shows a negative percentage instead of NaN% when the current month has no revenue data', async () => {
    loginAs('ADMIN')
    const summaryWithoutRevenue = {
      monthlyCosts: SEED_FINANCIAL_SUMMARY.monthlyCosts,
      previousMonthMargin: SEED_FINANCIAL_SUMMARY.previousMonthMargin,
      upcomingReceivables: SEED_FINANCIAL_SUMMARY.upcomingReceivables,
      upcomingPayables: SEED_FINANCIAL_SUMMARY.upcomingPayables,
    }
    server.use(http.get('/api/v1/reports/financial-summary', () => HttpResponse.json(summaryWithoutRevenue)))
    renderDashboard()

    await screen.findByText('Resumen del mes')
    const summaryCard = cardByTitle('Resumen del mes')

    // monthlyRevenue missing -> monthlyMargin normalizes to 0 -> change vs. previousMonthMargin
    // (900) is -100%, not NaN%.
    expect(summaryCard.textContent).not.toContain('NaN')
    const changeText = within(summaryCard).getByText('-100.0%')
    expect(changeText.className).toContain('text-error')
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

  it('opens the invoice modal in read-only mode from the invoice code in "Facturas por cobrar"', async () => {
    const user = userEvent.setup()
    loginAs('ADMIN')
    renderDashboard()

    const receivable = SEED_FINANCIAL_SUMMARY.upcomingReceivables[0]!

    await user.click(await screen.findByRole('button', { name: receivable.number }))

    const dialog = await screen.findByRole('dialog')
    expect(within(dialog).getByRole('heading', { name: 'Factura' })).toBeInTheDocument()
    expect(within(dialog).queryByRole('button', { name: /guardar cambios/i })).not.toBeInTheDocument()
    expect(within(dialog).queryByRole('button', { name: /crear factura/i })).not.toBeInTheDocument()
  })

  it('opens the supplier invoice modal in read-only mode from the invoice code in "Facturas por pagar", even though it is still PENDING (normally editable)', async () => {
    const user = userEvent.setup()
    loginAs('ADMIN')
    renderDashboard()

    const payable = SEED_FINANCIAL_SUMMARY.upcomingPayables[0]!
    const supplierInvoice = SEED_SUPPLIER_INVOICES.find((si) => si.id === payable.id)!
    expect(supplierInvoice.status).toBe('PENDING')

    await user.click(await screen.findByRole('button', { name: payable.number }))

    const dialog = await screen.findByRole('dialog')
    expect(within(dialog).getByRole('heading', { name: 'Factura de proveedor' })).toBeInTheDocument()
    expect(within(dialog).getByLabelText(/nº factura proveedor/i)).toBeDisabled()
    expect(within(dialog).queryByRole('button', { name: /guardar cambios/i })).not.toBeInTheDocument()
  })

  it('does not fetch clients/suppliers/vehicles until an invoice link is opened', async () => {
    const user = userEvent.setup()
    loginAs('ADMIN')

    const requestedPaths = new Set<string>()
    server.events.on('request:start', ({ request }) => {
      requestedPaths.add(new URL(request.url).pathname)
    })

    renderDashboard()

    // InvoiceInfoLink/SupplierInvoiceInfoLink are mounted for every upcoming invoice as soon as
    // this section renders — if InvoiceFormModal/SupplierInvoiceFormModal were unconditionally
    // mounted alongside them (instead of gated on `open`), their useAllClients()/useAllSuppliers()/
    // useAllVehicles() calls would already have fired by now.
    await screen.findByText('Facturas por cobrar')
    await screen.findByText('Facturas por pagar')

    expect(requestedPaths.has('/api/v1/clients')).toBe(false)
    expect(requestedPaths.has('/api/v1/suppliers')).toBe(false)
    expect(requestedPaths.has('/api/v1/vehicles')).toBe(false)

    const receivable = SEED_FINANCIAL_SUMMARY.upcomingReceivables[0]!
    await user.click(await screen.findByRole('button', { name: receivable.number }))

    await waitFor(() => expect(requestedPaths.has('/api/v1/clients')).toBe(true))
  })

  it('shows a "Marcar factura como pagada" button for each row in the upcoming invoice lists', async () => {
    loginAs('ADMIN')
    renderDashboard()

    const receivable = SEED_FINANCIAL_SUMMARY.upcomingReceivables[0]!
    const payable = SEED_FINANCIAL_SUMMARY.upcomingPayables[0]!

    const receivableRow = (await screen.findByText(receivable.number)).closest('li') as HTMLElement
    expect(within(receivableRow).getByRole('button', { name: /marcar factura como pagada/i })).toBeInTheDocument()

    const payableRow = screen.getByText(payable.number).closest('li') as HTMLElement
    expect(within(payableRow).getByRole('button', { name: /marcar factura como pagada/i })).toBeInTheDocument()
  })

  it('pays a receivable from "Facturas por cobrar" and the row disappears once the dashboard refetches', async () => {
    const user = userEvent.setup()
    loginAs('ADMIN')

    // invoice-2 is a real, ISSUED invoice in SEED_INVOICES, so the real PATCH /invoices/:id/pay
    // handler succeeds unmodified — only the summary endpoint is overridden here, call-count
    // branched, to prove the post-pay refetch actually happens (without the FINANCIAL_SUMMARY_KEY
    // invalidation fix, the count would stay at 1 and this row would never disappear).
    let financialSummaryCallCount = 0
    server.use(
      http.get('/api/v1/reports/financial-summary', () => {
        financialSummaryCallCount++
        if (financialSummaryCallCount === 1) {
          return HttpResponse.json(SEED_FINANCIAL_SUMMARY)
        }
        return HttpResponse.json({
          ...SEED_FINANCIAL_SUMMARY,
          upcomingReceivables: SEED_FINANCIAL_SUMMARY.upcomingReceivables.filter(
            (invoice) => invoice.id !== 'invoice-2',
          ),
        })
      }),
    )

    renderDashboard()

    const row = (await screen.findByText('INV-2026-00002')).closest('li') as HTMLElement
    await user.click(within(row).getByRole('button', { name: /marcar factura como pagada/i }))

    await waitFor(() => expect(screen.queryByText('INV-2026-00002')).not.toBeInTheDocument())
    expect(financialSummaryCallCount).toBeGreaterThanOrEqual(2)
  })

  it('pays a payable from "Facturas por pagar" and the row disappears once the dashboard refetches', async () => {
    const user = userEvent.setup()
    loginAs('ADMIN')

    // supplier-invoice-1 is a real, PENDING supplier invoice in SEED_SUPPLIER_INVOICES with a
    // header vehicleId (so it never hits the per-line-item allocation guard) — same rationale as
    // the receivable test above, mirrored for the payables card/hook.
    let financialSummaryCallCount = 0
    server.use(
      http.get('/api/v1/reports/financial-summary', () => {
        financialSummaryCallCount++
        if (financialSummaryCallCount === 1) {
          return HttpResponse.json(SEED_FINANCIAL_SUMMARY)
        }
        return HttpResponse.json({
          ...SEED_FINANCIAL_SUMMARY,
          upcomingPayables: SEED_FINANCIAL_SUMMARY.upcomingPayables.filter(
            (invoice) => invoice.id !== 'supplier-invoice-1',
          ),
        })
      }),
    )

    renderDashboard()

    const row = (await screen.findByText('F-2026-0456')).closest('li') as HTMLElement
    await user.click(within(row).getByRole('button', { name: /marcar factura como pagada/i }))

    await waitFor(() => expect(screen.queryByText('F-2026-0456')).not.toBeInTheDocument())
    expect(financialSummaryCallCount).toBeGreaterThanOrEqual(2)
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
