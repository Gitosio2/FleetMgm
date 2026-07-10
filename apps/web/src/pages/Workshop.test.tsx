import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { beforeEach, describe, expect, it } from 'vitest'
import { useAuthStore } from '@fleetmgm/store'
import {
  resetMaintenanceMock,
  resetVehiclesMock,
  resetWorkersMock,
  resetWorkshopSchedulesMock,
  SEED_VEHICLES,
} from '@/mocks/handlers'
import { Workshop } from './Workshop'

function renderWorkshop() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  })

  return render(
    <QueryClientProvider client={queryClient}>
      <Workshop />
    </QueryClientProvider>,
  )
}

function loginAs(role: 'ADMIN' | 'WORKSHOP_STAFF') {
  useAuthStore.getState().login({
    email: 'user@fleetmgm.com',
    role,
    accessToken: 'token',
    refreshToken: 'refresh',
  })
}

describe('Workshop', () => {
  beforeEach(() => {
    resetMaintenanceMock()
    resetWorkshopSchedulesMock()
    resetVehiclesMock()
    resetWorkersMock()
    useAuthStore.getState().logout()
  })

  it('range selector filters the schedule list', async () => {
    loginAs('ADMIN')
    const user = userEvent.setup()
    renderWorkshop()

    expect(await screen.findByText('Cambio de aceite')).toBeInTheDocument()
    expect(screen.queryByText('Revisión de frenos')).not.toBeInTheDocument()
    expect(screen.queryByText('Revisión general')).not.toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: /semana/i }))
    await waitFor(() => expect(screen.getByText('Revisión de frenos')).toBeInTheDocument())
    expect(screen.queryByText('Revisión general')).not.toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: /mes/i }))
    await waitFor(() => expect(screen.getByText('Revisión general')).toBeInTheDocument())
    expect(screen.getByText('Cambio de aceite')).toBeInTheDocument()
    expect(screen.getByText('Revisión de frenos')).toBeInTheDocument()
  })

  it('WORKSHOP_STAFF sees and creates a maintenance order', async () => {
    loginAs('WORKSHOP_STAFF')
    const user = userEvent.setup()
    renderWorkshop()

    await screen.findByText('Cambio de aceite')
    expect(await screen.findByText('Cambio de filtro')).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: /nueva orden/i }))

    await user.type(screen.getByLabelText(/tipo/i), 'Cambio de neumáticos')
    await user.selectOptions(screen.getByLabelText(/vehículo/i), 'vehicle-1')

    await user.click(screen.getByRole('button', { name: /crear orden/i }))

    await waitFor(() => expect(screen.getByText('Cambio de neumáticos')).toBeInTheDocument())
  })

  it('starting a maintenance order transitions its badge from Programado to En curso', async () => {
    loginAs('ADMIN')
    const user = userEvent.setup()
    renderWorkshop()

    const row = (await screen.findByText('Cambio de aceite y filtro')).closest('tr')!
    expect(within(row).getByText('Programado')).toBeInTheDocument()

    await user.click(within(row).getByRole('button', { name: /iniciar/i }))

    await waitFor(() => expect(within(row).getByText('En curso')).toBeInTheDocument())
  })

  it('completing a maintenance order also completes its linked schedule (no manual schedule complete exists)', async () => {
    loginAs('ADMIN')
    const user = userEvent.setup()
    renderWorkshop()

    await user.click(screen.getByRole('button', { name: /semana/i }))
    const scheduleRow = (await screen.findByText('Revisión de frenos')).closest('tr')!
    expect(within(scheduleRow).getByText('En curso')).toBeInTheDocument()

    const maintenanceRow = (await screen.findByText('Cambio de pastillas de freno')).closest('tr')!
    await user.click(within(maintenanceRow).getByRole('button', { name: /completar/i }))

    await waitFor(() => expect(within(maintenanceRow).getByText('Completado')).toBeInTheDocument())
    await waitFor(() => expect(within(scheduleRow).getByText('Completado')).toBeInTheDocument())
  })

  it('shows "<make> <model>" in the maintenance list when the vehicle has no license plate', async () => {
    loginAs('ADMIN')
    renderWorkshop()

    const row = (await screen.findByText('Cambio de filtro')).closest('tr')!
    const [, heavyMachinery] = SEED_VEHICLES
    expect(
      within(row).getByText(`${heavyMachinery!.make} ${heavyMachinery!.model}`),
    ).toBeInTheDocument()
  })

  it('shows "<make> <model>" in the schedule list when the vehicle has no license plate', async () => {
    loginAs('ADMIN')
    const user = userEvent.setup()
    renderWorkshop()

    await user.click(screen.getByRole('button', { name: /mes/i }))
    const row = (await screen.findByText('Revisión general')).closest('tr')!
    const [, heavyMachinery] = SEED_VEHICLES
    expect(
      within(row).getByText(`${heavyMachinery!.make} ${heavyMachinery!.model}`),
    ).toBeInTheDocument()
  })

  it('cancelling a pending schedule transitions its badge to Cancelado', async () => {
    loginAs('ADMIN')
    const user = userEvent.setup()
    renderWorkshop()

    const row = (await screen.findByText('Cambio de aceite')).closest('tr')!

    await user.click(within(row).getByRole('button', { name: /cancelar/i }))

    await waitFor(() => expect(within(row).getByText('Cancelado')).toBeInTheDocument())
  })
})
