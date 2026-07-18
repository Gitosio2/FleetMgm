import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { http, HttpResponse } from 'msw'
import { beforeEach, describe, expect, it } from 'vitest'
import { useAuthStore } from '@fleetmgm/store'
import { resetMaintenanceMock, resetVehiclesMock, resetWorkersMock, resetWorkshopSchedulesMock, SEED_VEHICLES } from '@/mocks/handlers'
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

describe('Workshop (Agenda)', () => {
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

  it('creates a workshop schedule entry with a category, which creates its linked order', async () => {
    loginAs('ADMIN')
    const user = userEvent.setup()
    renderWorkshop()

    await screen.findByText('Cambio de aceite')

    await user.click(screen.getByRole('button', { name: /nueva entrada/i }))

    expect(screen.getByLabelText(/categoría/i)).toHaveValue('PREVENTIVE')

    await user.type(screen.getByLabelText(/tipo/i), 'Rotura de transmisión')
    await user.selectOptions(screen.getByLabelText(/vehículo/i), 'vehicle-1')
    await user.selectOptions(screen.getByLabelText(/categoría/i), 'CORRECTIVE')
    await user.type(screen.getByLabelText(/fecha/i), '2026-07-15')

    await user.click(screen.getByRole('button', { name: /crear entrada/i }))

    const row = (await screen.findByText('Rotura de transmisión')).closest('tr')!
    expect(within(row).getByText('Correctivo')).toBeInTheDocument()
    expect(within(row).getByText('Pendiente')).toBeInTheDocument()
  })

  it('does not show the category selector in edit mode', async () => {
    loginAs('ADMIN')
    const user = userEvent.setup()
    renderWorkshop()

    const row = (await screen.findByText('Cambio de aceite')).closest('tr')!
    await user.click(within(row).getByRole('button', { name: /editar entrada/i }))

    expect(await screen.findByRole('heading', { name: 'Editar entrada' })).toBeInTheDocument()
    expect(screen.queryByLabelText(/categoría/i)).not.toBeInTheDocument()
  })

  it('starting a linked schedule transitions its badge from Pendiente to En curso', async () => {
    loginAs('ADMIN')
    const user = userEvent.setup()
    renderWorkshop()

    const row = (await screen.findByText('Cambio de aceite')).closest('tr')!
    expect(within(row).getByText('Pendiente')).toBeInTheDocument()
    expect(within(row).getByText('Preventivo')).toBeInTheDocument()

    await user.click(within(row).getByRole('button', { name: /iniciar/i }))

    await waitFor(() => expect(within(row).getByText('En curso')).toBeInTheDocument())
  })

  it('starting an orphan schedule (no linked order) creates and starts one', async () => {
    loginAs('ADMIN')
    const user = userEvent.setup()
    renderWorkshop()

    await user.click(screen.getByRole('button', { name: /mes/i }))
    const row = (await screen.findByText('Revisión general')).closest('tr')!
    // No linked order yet: both "Categoría" and "Técnico" render as dashes.
    expect(within(row).getAllByText('—')).toHaveLength(2)

    await user.click(within(row).getByRole('button', { name: /iniciar/i }))

    await waitFor(() => expect(within(row).getByText('En curso')).toBeInTheDocument())
    expect(within(row).getByText('Preventivo')).toBeInTheDocument()
    // Only "Técnico" is still unset now that a linked order exists.
    expect(within(row).getAllByText('—')).toHaveLength(1)
  })

  it('shows an error message when a double "Iniciar" click causes an invalid state transition', async () => {
    loginAs('ADMIN')
    const user = userEvent.setup()
    renderWorkshop()

    const row = (await screen.findByText('Cambio de aceite')).closest('tr')!
    const button = within(row).getByRole('button', { name: /iniciar/i })

    await Promise.all([user.click(button), user.click(button)])

    await waitFor(() =>
      expect(within(row).getByRole('alert')).toHaveTextContent(/no se pudo completar la acción/i),
    )
  })

  it('completing an in-progress schedule transitions its badge to Completado', async () => {
    loginAs('ADMIN')
    const user = userEvent.setup()
    renderWorkshop()

    await user.click(screen.getByRole('button', { name: /semana/i }))
    const row = (await screen.findByText('Revisión de frenos')).closest('tr')!
    expect(within(row).getByText('En curso')).toBeInTheDocument()

    await user.click(within(row).getByRole('button', { name: /completar/i }))

    await waitFor(() => expect(within(row).getByText('Completado')).toBeInTheDocument())
  })

  it('shows an error message when completing fails', async () => {
    loginAs('ADMIN')
    const user = userEvent.setup()
    server.use(
      http.patch('/api/v1/workshop/schedules/:id/complete', () =>
        HttpResponse.json(
          {
            status: 409,
            code: 'MAINTENANCE_INVALID_STATE_TRANSITION',
            message: 'Cannot complete',
            correlationId: 'test-correlation-id',
          },
          { status: 409 },
        ),
      ),
    )
    renderWorkshop()

    await user.click(screen.getByRole('button', { name: /semana/i }))
    const row = (await screen.findByText('Revisión de frenos')).closest('tr')!

    await user.click(within(row).getByRole('button', { name: /completar/i }))

    await waitFor(() =>
      expect(within(row).getByRole('alert')).toHaveTextContent(/no se pudo completar la acción/i),
    )
  })

  it('shows "<make> <model>" in the schedule list when the vehicle has no license plate', async () => {
    loginAs('ADMIN')
    const user = userEvent.setup()
    renderWorkshop()

    await user.click(screen.getByRole('button', { name: /mes/i }))
    const row = (await screen.findByText('Revisión general')).closest('tr')!
    const [, heavyMachinery] = SEED_VEHICLES
    expect(within(row).getByText(`${heavyMachinery!.make} ${heavyMachinery!.model}`)).toBeInTheDocument()
  })

  it('cancelling a pending schedule transitions its badge to Cancelado', async () => {
    loginAs('ADMIN')
    const user = userEvent.setup()
    renderWorkshop()

    const row = (await screen.findByText('Cambio de aceite')).closest('tr')!

    await user.click(within(row).getByRole('button', { name: /cancelar/i }))

    await waitFor(() => expect(within(row).getByText('Cancelado')).toBeInTheDocument())
  })

  it('shows an error message when a double "Cancelar" click on a schedule causes an invalid state transition', async () => {
    loginAs('ADMIN')
    const user = userEvent.setup()
    renderWorkshop()

    const row = (await screen.findByText('Cambio de aceite')).closest('tr')!
    const button = within(row).getByRole('button', { name: /cancelar/i })

    await Promise.all([user.click(button), user.click(button)])

    await waitFor(() =>
      expect(within(row).getByRole('alert')).toHaveTextContent(/no se pudo completar la acción/i),
    )
  })

  it('edits a workshop schedule via the modal and reflects the change in the agenda', async () => {
    loginAs('ADMIN')
    const user = userEvent.setup()
    renderWorkshop()

    const row = (await screen.findByText('Cambio de aceite')).closest('tr')!
    await user.click(within(row).getByRole('button', { name: /editar entrada/i }))

    expect(await screen.findByRole('heading', { name: 'Editar entrada' })).toBeInTheDocument()
    expect(screen.getByLabelText(/tipo/i)).toHaveValue('Cambio de aceite')
    expect(screen.getByLabelText(/fecha/i)).toHaveValue('2026-07-09')

    await user.selectOptions(screen.getByLabelText(/prioridad/i), 'HIGH')
    await user.click(screen.getByRole('button', { name: /guardar cambios/i }))

    await waitFor(() => {
      const updatedRow = screen.getByText('Cambio de aceite').closest('tr')!
      expect(within(updatedRow).getByText('Alta')).toBeInTheDocument()
    })
  })

  it('prefills the schedule time inputs from an existing schedule in edit mode', async () => {
    loginAs('ADMIN')
    const user = userEvent.setup()
    renderWorkshop()

    const row = (await screen.findByText('Cambio de aceite')).closest('tr')!
    await user.click(within(row).getByRole('button', { name: /editar entrada/i }))

    expect(await screen.findByRole('heading', { name: 'Editar entrada' })).toBeInTheDocument()
    expect(screen.getByLabelText(/hora de inicio/i)).toHaveValue('08:00')
    expect(screen.getByLabelText(/hora de fin/i)).toHaveValue('09:00')
  })

  it('creates a workshop schedule entry with a time range and includes it in the request', async () => {
    loginAs('ADMIN')
    const user = userEvent.setup()
    renderWorkshop()

    await screen.findByText('Cambio de aceite')

    await user.click(screen.getByRole('button', { name: /nueva entrada/i }))

    expect(screen.getByLabelText(/hora de inicio/i)).toHaveValue('')
    expect(screen.getByLabelText(/hora de fin/i)).toHaveValue('')

    await user.type(screen.getByLabelText(/tipo/i), 'Revisión de neumáticos')
    await user.selectOptions(screen.getByLabelText(/vehículo/i), 'vehicle-1')
    await user.type(screen.getByLabelText(/fecha/i), '2026-07-20')
    await user.type(screen.getByLabelText(/hora de inicio/i), '08:00')
    await user.type(screen.getByLabelText(/hora de fin/i), '09:30')

    await user.click(screen.getByRole('button', { name: /crear entrada/i }))

    const row = (await screen.findByText('Revisión de neumáticos')).closest('tr')!
    await user.click(within(row).getByRole('button', { name: /editar entrada/i }))

    expect(await screen.findByRole('heading', { name: 'Editar entrada' })).toBeInTheDocument()
    expect(screen.getByLabelText(/hora de inicio/i)).toHaveValue('08:00')
    expect(screen.getByLabelText(/hora de fin/i)).toHaveValue('09:30')
  })

  it('blocks schedule submission when the end time is before the start time', async () => {
    loginAs('ADMIN')
    const user = userEvent.setup()
    renderWorkshop()

    await screen.findByText('Cambio de aceite')
    await user.click(screen.getByRole('button', { name: /nueva entrada/i }))

    await user.type(screen.getByLabelText(/tipo/i), 'Prueba horario inválido')
    await user.selectOptions(screen.getByLabelText(/vehículo/i), 'vehicle-1')
    await user.type(screen.getByLabelText(/fecha/i), '2026-07-20')
    await user.type(screen.getByLabelText(/hora de inicio/i), '10:00')
    await user.type(screen.getByLabelText(/hora de fin/i), '09:00')

    await user.click(screen.getByRole('button', { name: /crear entrada/i }))

    expect(await screen.findByRole('alert')).toHaveTextContent(
      /la hora de fin debe ser posterior a la hora de inicio/i,
    )
    expect(screen.queryByText('Prueba horario inválido')).not.toBeInTheDocument()
  })

  it('blocks schedule submission when the end time equals the start time', async () => {
    loginAs('ADMIN')
    const user = userEvent.setup()
    renderWorkshop()

    await screen.findByText('Cambio de aceite')
    await user.click(screen.getByRole('button', { name: /nueva entrada/i }))

    await user.type(screen.getByLabelText(/tipo/i), 'Prueba horario igual')
    await user.selectOptions(screen.getByLabelText(/vehículo/i), 'vehicle-1')
    await user.type(screen.getByLabelText(/fecha/i), '2026-07-20')
    await user.type(screen.getByLabelText(/hora de inicio/i), '10:00')
    await user.type(screen.getByLabelText(/hora de fin/i), '10:00')

    await user.click(screen.getByRole('button', { name: /crear entrada/i }))

    expect(await screen.findByRole('alert')).toHaveTextContent(
      /la hora de fin debe ser posterior a la hora de inicio/i,
    )
    expect(screen.queryByText('Prueba horario igual')).not.toBeInTheDocument()
  })

  it('paginates the agenda table when there is more than one page', async () => {
    loginAs('ADMIN')
    const user = userEvent.setup()

    server.use(
      http.get('/api/v1/workshop/schedules', ({ request }) => {
        const page = Number(new URL(request.url).searchParams.get('page') ?? 0)
        const schedule = {
          id: `schedule-page-${page}`,
          vehicleId: SEED_VEHICLES[0]!.id,
          vehicleLicensePlate: SEED_VEHICLES[0]!.licensePlate,
          vehicleMake: SEED_VEHICLES[0]!.make,
          vehicleModel: SEED_VEHICLES[0]!.model,
          technicianId: null,
          technicianName: null,
          maintenanceRecordId: null,
          maintenanceCategory: null,
          scheduledDate: '2026-07-18',
          scheduledStartTime: null,
          scheduledEndTime: null,
          type: page === 0 ? 'Entrada más reciente' : 'Entrada más antigua',
          priority: 'MEDIUM',
          status: 'PENDING',
          notes: null,
          createdAt: '2026-07-01T09:00:00Z',
        }
        return HttpResponse.json({ content: [schedule], page, size: 20, totalElements: 21, totalPages: 2 })
      }),
    )

    renderWorkshop()

    expect(await screen.findByText('Entrada más reciente')).toBeInTheDocument()
    expect(screen.getByText('Página 1 de 2')).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: /siguiente/i }))

    await waitFor(() => expect(screen.getByText('Entrada más antigua')).toBeInTheDocument())
    expect(screen.getByText('Página 2 de 2')).toBeInTheDocument()
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
