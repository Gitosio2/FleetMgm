import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { beforeEach, describe, expect, it } from 'vitest'
import { useAuthStore } from '@fleetmgm/store'
import { resetVehiclesMock, SEED_PROFITABILITY, SEED_VEHICLE_REVENUE, SEED_VEHICLES } from '@/mocks/handlers'
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

    await screen.findByText(`${FIRST_VEHICLE!.make} ${FIRST_VEHICLE!.model}`)

    expect(await screen.findByText('Activo')).toBeInTheDocument()
    expect(await screen.findByText('Mantenimiento')).toBeInTheDocument()
  })

  it('shows only the assigned vehicle for the DRIVER role', async () => {
    loginAs('DRIVER')
    renderVehicles()

    expect(await screen.findByText(`${FIRST_VEHICLE!.make} ${FIRST_VEHICLE!.model}`)).toBeInTheDocument()
    expect(screen.queryByText(`${SECOND_VEHICLE!.make} ${SECOND_VEHICLE!.model}`)).not.toBeInTheDocument()
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

  it('filters both history lists when the month selector changes', async () => {
    const user = userEvent.setup()
    loginAs('ADMIN')
    renderVehicles()

    await screen.findByText(`${FIRST_VEHICLE!.make} ${FIRST_VEHICLE!.model}`)

    const profitabilityButtons = await screen.findAllByLabelText('Ver rentabilidad')
    await user.click(profitabilityButtons[0]!)
    await screen.findByText(`Rentabilidad — ${FIRST_VEHICLE!.make} ${FIRST_VEHICLE!.model}`)

    // Default selector is the current month (July 2026, this project's fixed dev clock) — the
    // vehicle's July maintenance record and July invoice line item should be visible.
    expect(await screen.findByText('Cambio de pastillas de freno')).toBeInTheDocument()
    expect(await screen.findByText('INV-2026-00001')).toBeInTheDocument()

    const juneInvoice = SEED_VEHICLE_REVENUE.find((item) => item.invoiceNumber === 'INV-2026-00002')!
    expect(screen.queryByText(juneInvoice.invoiceNumber)).not.toBeInTheDocument()

    await user.selectOptions(screen.getByLabelText('Mes'), 'Junio')

    expect(await screen.findByText(juneInvoice.invoiceNumber)).toBeInTheDocument()
    expect(screen.queryByText('Cambio de pastillas de freno')).not.toBeInTheDocument()
    expect(screen.queryByText('INV-2026-00001')).not.toBeInTheDocument()
    expect(await screen.findByText('Sin mantenimientos en este periodo')).toBeInTheDocument()
  })
})
