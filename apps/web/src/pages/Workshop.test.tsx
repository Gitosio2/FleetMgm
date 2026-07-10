import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { http, HttpResponse } from 'msw'
import { beforeEach, describe, expect, it } from 'vitest'
import { useAuthStore } from '@fleetmgm/store'
import {
  resetMaintenanceMock,
  resetVehiclesMock,
  resetWorkersMock,
  resetWorkshopSchedulesMock,
  SEED_VEHICLES,
} from '@/mocks/handlers'
import { server } from '@/mocks/server'
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

  it('shows an error message when a double "Iniciar" click causes an invalid state transition', async () => {
    loginAs('ADMIN')
    const user = userEvent.setup()
    renderWorkshop()

    const row = (await screen.findByText('Cambio de aceite y filtro')).closest('tr')!
    const button = within(row).getByRole('button', { name: /iniciar/i })

    await Promise.all([user.click(button), user.click(button)])

    await waitFor(() =>
      expect(within(row).getByRole('alert')).toHaveTextContent(/no se pudo completar la acción/i),
    )
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

  it('cancelling a scheduled maintenance order transitions its badge to Cancelado', async () => {
    loginAs('ADMIN')
    const user = userEvent.setup()
    renderWorkshop()

    const row = (await screen.findByText('Cambio de aceite y filtro')).closest('tr')!
    // maintenance-1 is linked to schedule-1 ('Cambio de aceite', visible in the default
    // 'today' range) — cancelling the maintenance order must cascade-cancel its schedule too.
    const scheduleRow = (await screen.findByText('Cambio de aceite')).closest('tr')!

    await user.click(within(row).getByRole('button', { name: /cancelar/i }))

    await waitFor(() => expect(within(row).getByText('Cancelado')).toBeInTheDocument())
    await waitFor(() => expect(within(scheduleRow).getByText('Cancelado')).toBeInTheDocument())
  })

  it('shows an error message when a double "Cancelar" click on a maintenance order causes an invalid state transition', async () => {
    loginAs('ADMIN')
    const user = userEvent.setup()
    renderWorkshop()

    const row = (await screen.findByText('Cambio de aceite y filtro')).closest('tr')!
    const button = within(row).getByRole('button', { name: /cancelar/i })

    await Promise.all([user.click(button), user.click(button)])

    await waitFor(() =>
      expect(within(row).getByRole('alert')).toHaveTextContent(/no se pudo completar la acción/i),
    )
  })

  it('cancelling a pending schedule transitions its badge to Cancelado', async () => {
    loginAs('ADMIN')
    const user = userEvent.setup()
    renderWorkshop()

    const row = (await screen.findByText('Cambio de aceite')).closest('tr')!
    // schedule-1 is linked to maintenance-1 ('Cambio de aceite y filtro') — cancelling the
    // schedule must cascade-cancel its linked maintenance order too (mirror direction).
    const maintenanceRow = (await screen.findByText('Cambio de aceite y filtro')).closest('tr')!

    await user.click(within(row).getByRole('button', { name: /cancelar/i }))

    await waitFor(() => expect(within(row).getByText('Cancelado')).toBeInTheDocument())
    await waitFor(() => expect(within(maintenanceRow).getByText('Cancelado')).toBeInTheDocument())
  })

  it('shows an error message when the schedule agenda query fails', async () => {
    loginAs('ADMIN')
    server.use(
      http.get('/api/v1/workshop/schedules', () =>
        HttpResponse.json(
          {
            status: 500,
            code: 'INTERNAL_SERVER_ERROR',
            message: 'Unexpected error',
            correlationId: 'test-correlation-id',
          },
          { status: 500 },
        ),
      ),
    )
    renderWorkshop()

    expect(await screen.findByText('No se pudieron cargar los datos.')).toBeInTheDocument()
    expect(screen.queryByText('Cambio de aceite')).not.toBeInTheDocument()
  })
})
