import { fireEvent, render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { http, HttpResponse } from 'msw'
import { beforeEach, describe, expect, it } from 'vitest'
import { useAuthStore } from '@fleetmgm/store'
import { resetVehiclesMock, SEED_PROFITABILITY, SEED_VEHICLE_REVENUE, SEED_VEHICLES } from '@/mocks/handlers'
import { server } from '@/mocks/server'
import { formatCurrency } from '@/lib/currency'
import { Vehicles } from './Vehicles'

function renderVehicles() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  })

  return render(
    <QueryClientProvider client={queryClient}>
      <Vehicles />
    </QueryClientProvider>,
  )
}

const [FIRST_VEHICLE, SECOND_VEHICLE] = SEED_VEHICLES

// Totales' Ingresos/Gastos/Margen cards can legitimately show the same formatted amount (e.g.
// margin === revenue whenever costs is 0,00 €), so a plain within(totalesGrid).getByText(...)
// can hit "multiple elements found". Scoping to each card by its fixed render order (Ingresos,
// Gastos, Margen — see VehicleProfitabilityPanel.tsx) avoids that ambiguity.
function getTotalesCardValue(index: 0 | 1 | 2 | 3 | 4) {
  const totalesHeading = screen.getByRole('heading', { name: 'Totales' })
  const totalesGrid = totalesHeading.nextElementSibling as HTMLElement
  const card = totalesGrid.children[index] as HTMLElement
  return within(card)
}

function loginAs(role: 'ADMIN' | 'DRIVER') {
  useAuthStore.getState().login({
    email: 'user@fleetmgm.com',
    role,
    accessToken: 'token',
    refreshToken: 'refresh',
  })
}

describe('Vehicles', () => {
  beforeEach(() => {
    resetVehiclesMock()
    useAuthStore.getState().logout()
  })

  it('renders the paginated vehicle list', async () => {
    loginAs('ADMIN')
    renderVehicles()

    for (const vehicle of SEED_VEHICLES) {
      expect(await screen.findByText(`${vehicle.make} ${vehicle.model}`)).toBeInTheDocument()
    }
  })

  it('shows the correct status badge per vehicle status', async () => {
    loginAs('ADMIN')
    renderVehicles()

    // Scoped to each row — "Activo"/"Mantenimiento" also appear as <option> labels in the new
    // status filter select, which would otherwise make screen.findByText ambiguous.
    const firstRow = (await screen.findByText(`${FIRST_VEHICLE!.make} ${FIRST_VEHICLE!.model}`)).closest('tr')!
    const secondRow = screen.getByText(`${SECOND_VEHICLE!.make} ${SECOND_VEHICLE!.model}`).closest('tr')!

    expect(within(firstRow).getByText('Activo')).toBeInTheDocument()
    expect(within(secondRow).getByText('Mantenimiento')).toBeInTheDocument()
  })

  it('shows only the assigned vehicle for the DRIVER role', async () => {
    loginAs('DRIVER')
    renderVehicles()

    expect(await screen.findByText(`${FIRST_VEHICLE!.make} ${FIRST_VEHICLE!.model}`)).toBeInTheDocument()
    expect(screen.queryByText(`${SECOND_VEHICLE!.make} ${SECOND_VEHICLE!.model}`)).not.toBeInTheDocument()
  })

  it('narrows the vehicle list with the category filter', async () => {
    const user = userEvent.setup()
    loginAs('ADMIN')
    renderVehicles()

    await screen.findByText(`${FIRST_VEHICLE!.make} ${FIRST_VEHICLE!.model}`)

    await user.selectOptions(screen.getByLabelText(/filtrar por tipo/i), 'HEAVY_MACHINERY')

    await screen.findByText(`${SECOND_VEHICLE!.make} ${SECOND_VEHICLE!.model}`)
    expect(screen.queryByText(`${FIRST_VEHICLE!.make} ${FIRST_VEHICLE!.model}`)).not.toBeInTheDocument()
  })

  it('narrows the vehicle list with the status filter', async () => {
    const user = userEvent.setup()
    loginAs('ADMIN')
    renderVehicles()

    await screen.findByText(`${FIRST_VEHICLE!.make} ${FIRST_VEHICLE!.model}`)

    await user.selectOptions(screen.getByLabelText(/filtrar por estado/i), 'MAINTENANCE')

    await screen.findByText(`${SECOND_VEHICLE!.make} ${SECOND_VEHICLE!.model}`)
    expect(screen.queryByText(`${FIRST_VEHICLE!.make} ${FIRST_VEHICLE!.model}`)).not.toBeInTheDocument()
  })

  it('narrows the vehicle list with the license plate search', async () => {
    const user = userEvent.setup()
    loginAs('ADMIN')
    renderVehicles()

    await screen.findByText(`${FIRST_VEHICLE!.make} ${FIRST_VEHICLE!.model}`)

    await user.type(screen.getByLabelText(/buscar por matrícula/i), '1234')

    await screen.findByText(`${FIRST_VEHICLE!.make} ${FIRST_VEHICLE!.model}`)
    expect(screen.queryByText(`${SECOND_VEHICLE!.make} ${SECOND_VEHICLE!.model}`)).not.toBeInTheDocument()
  })

  it('narrows the vehicle list with the vehicle (make/model) search', async () => {
    const user = userEvent.setup()
    loginAs('ADMIN')
    renderVehicles()

    await screen.findByText(`${FIRST_VEHICLE!.make} ${FIRST_VEHICLE!.model}`)

    await user.type(screen.getByLabelText(/buscar por vehículo/i), 'caterpillar')

    await screen.findByText(`${SECOND_VEHICLE!.make} ${SECOND_VEHICLE!.model}`)
    expect(screen.queryByText(`${FIRST_VEHICLE!.make} ${FIRST_VEHICLE!.model}`)).not.toBeInTheDocument()
  })

  it('hides management actions for the DRIVER role', async () => {
    loginAs('DRIVER')
    renderVehicles()

    await screen.findByText(`${FIRST_VEHICLE!.make} ${FIRST_VEHICLE!.model}`)

    expect(screen.queryByRole('button', { name: /nuevo vehículo/i })).not.toBeInTheDocument()
    expect(screen.queryByLabelText('Editar vehículo')).not.toBeInTheDocument()
    expect(screen.queryByLabelText('Eliminar vehículo')).not.toBeInTheDocument()
  })

  it('opens the profitability dialog for a vehicle', async () => {
    const user = userEvent.setup()
    loginAs('ADMIN')
    renderVehicles()

    await screen.findByText(`${FIRST_VEHICLE!.make} ${FIRST_VEHICLE!.model}`)

    const profitabilityButtons = await screen.findAllByLabelText('Ver rentabilidad')
    await user.click(profitabilityButtons[0]!)

    const profitability = SEED_PROFITABILITY.find((entry) => entry.vehicleId === FIRST_VEHICLE!.id)!

    expect(
      await screen.findByText(`Rentabilidad — ${FIRST_VEHICLE!.make} ${FIRST_VEHICLE!.model}`),
    ).toBeInTheDocument()
    expect(await screen.findByText(formatCurrency(profitability.revenue))).toBeInTheDocument()
    expect(await screen.findByText(formatCurrency(profitability.costs))).toBeInTheDocument()
    expect(await screen.findByText(formatCurrency(profitability.margin))).toBeInTheDocument()
  })

  it('shows full unfiltered history in all three sections when no Desde/Hasta is set', async () => {
    const user = userEvent.setup()
    loginAs('ADMIN')
    renderVehicles()

    await screen.findByText(`${FIRST_VEHICLE!.make} ${FIRST_VEHICLE!.model}`)

    const profitabilityButtons = await screen.findAllByLabelText('Ver rentabilidad')
    await user.click(profitabilityButtons[0]!)
    await screen.findByText(`Rentabilidad — ${FIRST_VEHICLE!.make} ${FIRST_VEHICLE!.model}`)

    // No range set — both maintenance records, both supplier-invoice cost sources (a whole invoice
    // tied directly to the vehicle, and a split line item on a bulk invoice with no header vehicle),
    // and both invoice line items for this vehicle are visible (SEED_MAINTENANCE has one with no
    // workshopEntryDate, one dated July; SEED_VEHICLE_REVENUE has one July + one June item), and
    // "Totales" shows the static all-time SEED_PROFITABILITY figures.
    expect(await screen.findByText('Cambio de aceite y filtro')).toBeInTheDocument()
    expect(await screen.findByText('Cambio de pastillas de freno')).toBeInTheDocument()
    expect(await screen.findByText('Taller Mecánico Norte – F-2026-0456')).toBeInTheDocument()
    expect(await screen.findByText('Estación de Servicio Central: Gasoil - Toyota Hilux')).toBeInTheDocument()
    expect(await screen.findByText('INV-2026-00001')).toBeInTheDocument()
    expect(await screen.findByText('INV-2026-00002')).toBeInTheDocument()

    // Merged list is sorted by date descending: the split line item (2026-07-08) is newer than the
    // "Cambio de pastillas de freno" maintenance record (2026-07-03), which in turn is newer than
    // the whole supplier invoice (2026-07-01) — proving maintenance and supplier-invoice costs are
    // interleaved into ONE date-sorted list, not two separate sub-lists.
    const gastosCard = (await screen.findByRole('heading', { name: /Historial de gastos/ })).closest(
      '[data-slot="card"]',
    ) as HTMLElement
    const gastosItems = within(gastosCard)
      .getAllByRole('listitem')
      .map((item) => item.textContent ?? '')
    const lineItemIndex = gastosItems.findIndex((text) => text.includes('Estación de Servicio Central'))
    const maintenanceIndex = gastosItems.findIndex((text) => text.includes('Cambio de pastillas de freno'))
    const wholeInvoiceIndex = gastosItems.findIndex((text) => text.includes('Taller Mecánico Norte'))
    expect(lineItemIndex).toBeGreaterThanOrEqual(0)
    expect(lineItemIndex).toBeLessThan(maintenanceIndex)
    expect(maintenanceIndex).toBeLessThan(wholeInvoiceIndex)

    const profitability = SEED_PROFITABILITY.find((entry) => entry.vehicleId === FIRST_VEHICLE!.id)!
    expect(getTotalesCardValue(0).getByText(formatCurrency(profitability.revenue))).toBeInTheDocument()
    expect(getTotalesCardValue(1).getByText(formatCurrency(profitability.costs))).toBeInTheDocument()
    expect(getTotalesCardValue(2).getByText(formatCurrency(profitability.margin))).toBeInTheDocument()

    // FIRST_VEHICLE is KILOMETERS-measured — the two usage-scoped cards must render with the "km"
    // unit label and the static seed's non-null cost/profit-per-usage-unit values.
    expect(screen.getByText('Coste/km')).toBeInTheDocument()
    expect(screen.getByText('Beneficio/km')).toBeInTheDocument()
    expect(getTotalesCardValue(3).getByText(formatCurrency(profitability.costPerUsageUnit!))).toBeInTheDocument()
    expect(getTotalesCardValue(4).getByText(formatCurrency(profitability.profitPerUsageUnit!))).toBeInTheDocument()
  })

  it('fetches every page of maintenance history, not just the first, for a vehicle with more than one page of records', async () => {
    const user = userEvent.setup()
    loginAs('ADMIN')

    // Regression: useVehicleMaintenanceHistory used to request only page 0 (size:100) — safe back
    // when the panel was scoped to one month via a year/month selector, but silently dropping
    // every record beyond the first page once the panel's Desde/Hasta range defaults to unbounded
    // full history. Both pages' records — and their combined cost in the card header — must appear.
    server.use(
      http.get('/api/v1/maintenance', ({ request }) => {
        const page = Number(new URL(request.url).searchParams.get('page') ?? 0)
        const record = {
          id: `maintenance-page-${page}`,
          vehicleId: FIRST_VEHICLE!.id,
          vehicleLicensePlate: FIRST_VEHICLE!.licensePlate,
          vehicleMake: FIRST_VEHICLE!.make,
          vehicleModel: FIRST_VEHICLE!.model,
          type: page === 0 ? 'Mantenimiento página 1' : 'Mantenimiento página 2',
          description: null,
          usageAtService: null,
          cost: 50,
          workshopEntryDate: page === 0 ? '2026-07-10' : '2026-07-01',
          workshopExitDate: null,
          workshopEntryTime: null,
          workshopExitTime: null,
          technicianId: null,
          technicianName: null,
          status: 'COMPLETED',
          category: 'PREVENTIVE',
          createdAt: '2026-07-01T09:00:00Z',
        }
        return HttpResponse.json({ content: [record], page, size: 200, totalElements: 2, totalPages: 2 })
      }),
    )

    renderVehicles()
    await screen.findByText(`${FIRST_VEHICLE!.make} ${FIRST_VEHICLE!.model}`)

    const profitabilityButtons = await screen.findAllByLabelText('Ver rentabilidad')
    await user.click(profitabilityButtons[0]!)
    await screen.findByText(`Rentabilidad — ${FIRST_VEHICLE!.make} ${FIRST_VEHICLE!.model}`)

    expect(await screen.findByText('Mantenimiento página 1')).toBeInTheDocument()
    expect(await screen.findByText('Mantenimiento página 2')).toBeInTheDocument()
  })

  it('labels the usage-scoped cards in hours for an HOURS-measured vehicle', async () => {
    const user = userEvent.setup()
    loginAs('ADMIN')
    renderVehicles()

    await screen.findByText(`${SECOND_VEHICLE!.make} ${SECOND_VEHICLE!.model}`)

    const profitabilityButtons = await screen.findAllByLabelText('Ver rentabilidad')
    await user.click(profitabilityButtons[1]!)
    await screen.findByText(`Rentabilidad — ${SECOND_VEHICLE!.make} ${SECOND_VEHICLE!.model}`)

    const profitability = SEED_PROFITABILITY.find((entry) => entry.vehicleId === SECOND_VEHICLE!.id)!

    expect(screen.getByText('Coste/hora')).toBeInTheDocument()
    expect(screen.getByText('Beneficio/hora')).toBeInTheDocument()
    expect(getTotalesCardValue(3).getByText(formatCurrency(profitability.costPerUsageUnit!))).toBeInTheDocument()
    expect(getTotalesCardValue(4).getByText(formatCurrency(profitability.profitPerUsageUnit!))).toBeInTheDocument()
  })

  it('filters all three sections — including Totales — when a Desde/Hasta range is applied', async () => {
    const user = userEvent.setup()
    loginAs('ADMIN')
    renderVehicles()

    await screen.findByText(`${FIRST_VEHICLE!.make} ${FIRST_VEHICLE!.model}`)

    const profitabilityButtons = await screen.findAllByLabelText('Ver rentabilidad')
    await user.click(profitabilityButtons[0]!)
    await screen.findByText(`Rentabilidad — ${FIRST_VEHICLE!.make} ${FIRST_VEHICLE!.model}`)

    await screen.findByText('Cambio de pastillas de freno')

    // Narrow to June only — the vehicle's July maintenance record and July invoice line item must
    // drop out of all three sections, not just the two history cards (the bug being fixed: "Total
    // histórico" used to always show all-time figures regardless of any range).
    fireEvent.change(screen.getByLabelText('Desde'), { target: { value: '2026-06-01' } })
    fireEvent.change(screen.getByLabelText('Hasta'), { target: { value: '2026-06-30' } })

    const juneInvoice = SEED_VEHICLE_REVENUE.find((item) => item.invoiceNumber === 'INV-2026-00002')!
    expect(await screen.findByText(juneInvoice.invoiceNumber)).toBeInTheDocument()
    expect(screen.queryByText('Cambio de aceite y filtro')).not.toBeInTheDocument()
    expect(screen.queryByText('Cambio de pastillas de freno')).not.toBeInTheDocument()
    expect(screen.queryByText('INV-2026-00001')).not.toBeInTheDocument()
    // Both of this vehicle's supplier-invoice cost sources are dated in July (2026-07-01 and
    // 2026-07-08) — narrowing to June-only also drops them, leaving the merged list empty.
    expect(screen.queryByText('Taller Mecánico Norte – F-2026-0456')).not.toBeInTheDocument()
    expect(screen.queryByText('Estación de Servicio Central: Gasoil - Toyota Hilux')).not.toBeInTheDocument()
    expect(await screen.findByText('Sin gastos en este periodo')).toBeInTheDocument()

    // "Totales" now reflects the June-only range: revenue is just the June invoice line item
    // (300,00 €), and this vehicle has no maintenance cost or supplier invoice dated in June, so
    // costs are 0,00 € and margin equals revenue (also 300,00 €).
    expect(await getTotalesCardValue(0).findByText(formatCurrency(juneInvoice.subtotal))).toBeInTheDocument()
    expect(getTotalesCardValue(1).getByText(formatCurrency(0))).toBeInTheDocument()
    expect(getTotalesCardValue(2).getByText(formatCurrency(juneInvoice.subtotal))).toBeInTheDocument()

    // The mock (like a real vehicle with no usage-log baseline before `from`) returns null for both
    // usage-scoped fields once a Desde/Hasta range is applied — must render as "Sin datos
    // suficientes", never as a misleading 0,00 €.
    expect(getTotalesCardValue(3).getByText('Sin datos suficientes')).toBeInTheDocument()
    expect(getTotalesCardValue(4).getByText('Sin datos suficientes')).toBeInTheDocument()
  })
})
