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

// The new "Horario del día" widget (DaySchedule) always queries the 'today' range, same as the
// Agenda section's default range — so a schedule entry tagged 'today' (like schedule-1 and any
// newly created entry, which are always tagged for every range) now legitimately renders twice
// on the page. Scope queries to the specific section under test instead of relying on unscoped
// screen.findByText/getByText, which would otherwise throw on the now-ambiguous match.
function agendaSection() {
  return screen.getByRole('heading', { name: 'Agenda' }).closest('section')!
}

function maintenanceSection() {
  return screen.getByRole('heading', { name: 'Órdenes de mantenimiento' }).closest('section')!
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

    expect(await within(agendaSection()).findByText('Cambio de aceite')).toBeInTheDocument()
    expect(within(agendaSection()).queryByText('Revisión de frenos')).not.toBeInTheDocument()
    expect(within(agendaSection()).queryByText('Revisión general')).not.toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: /semana/i }))
    await waitFor(() =>
      expect(within(agendaSection()).getByText('Revisión de frenos')).toBeInTheDocument(),
    )
    expect(within(agendaSection()).queryByText('Revisión general')).not.toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: /mes/i }))
    await waitFor(() =>
      expect(within(agendaSection()).getByText('Revisión general')).toBeInTheDocument(),
    )
    expect(within(agendaSection()).getByText('Cambio de aceite')).toBeInTheDocument()
    expect(within(agendaSection()).getByText('Revisión de frenos')).toBeInTheDocument()
  })

  it('WORKSHOP_STAFF sees and creates a maintenance order', async () => {
    loginAs('WORKSHOP_STAFF')
    const user = userEvent.setup()
    renderWorkshop()

    await screen.findByText('Cambio de aceite y filtro')
    expect(await screen.findByText('Cambio de filtro')).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: /nueva orden/i }))

    await user.type(screen.getByLabelText(/tipo/i), 'Cambio de neumáticos')
    await user.selectOptions(screen.getByLabelText(/vehículo/i), 'vehicle-1')

    await user.click(screen.getByRole('button', { name: /crear orden/i }))

    // Also creates its linked agenda entry (see the dedicated test below) — appears once in the
    // maintenance table and once in the agenda table (also once more in the "Horario del día"
    // widget, not asserted here — see DaySchedule.test.tsx for that widget's own coverage).
    await waitFor(() => {
      expect(within(maintenanceSection()).getByText('Cambio de neumáticos')).toBeInTheDocument()
      expect(within(agendaSection()).getByText('Cambio de neumáticos')).toBeInTheDocument()
    })
  })

  it('creating a maintenance order also creates its linked agenda entry', async () => {
    loginAs('WORKSHOP_STAFF')
    const user = userEvent.setup()
    renderWorkshop()

    await screen.findByText('Cambio de aceite y filtro')

    await user.click(screen.getByRole('button', { name: /nueva orden/i }))

    await user.type(screen.getByLabelText(/tipo/i), 'Cambio de correa de distribución')
    await user.selectOptions(screen.getByLabelText(/vehículo/i), 'vehicle-1')

    await user.click(screen.getByRole('button', { name: /crear orden/i }))

    await waitFor(() => {
      expect(
        within(maintenanceSection()).getByText('Cambio de correa de distribución'),
      ).toBeInTheDocument()
      expect(within(agendaSection()).getByText('Cambio de correa de distribución')).toBeInTheDocument()
    })
    const scheduleRow = within(agendaSection())
      .getByText('Cambio de correa de distribución')
      .closest('tr')!
    expect(within(scheduleRow).getByText('Pendiente')).toBeInTheDocument()
  })

  it('creates a workshop schedule entry directly from the agenda', async () => {
    loginAs('ADMIN')
    const user = userEvent.setup()
    renderWorkshop()

    await screen.findByText('Cambio de aceite y filtro')

    await user.click(screen.getByRole('button', { name: /nueva entrada/i }))

    await user.type(screen.getByLabelText(/tipo/i), 'Revisión de suspensión')
    await user.selectOptions(screen.getByLabelText(/vehículo/i), 'vehicle-1')
    await user.type(screen.getByLabelText(/fecha/i), '2026-07-15')

    await user.click(screen.getByRole('button', { name: /crear entrada/i }))

    await waitFor(() =>
      expect(within(agendaSection()).getByText('Revisión de suspensión')).toBeInTheDocument(),
    )
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
    const scheduleRow = (await within(agendaSection()).findByText('Cambio de aceite')).closest('tr')!

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

    const row = (await within(agendaSection()).findByText('Cambio de aceite')).closest('tr')!
    // schedule-1 is linked to maintenance-1 ('Cambio de aceite y filtro') — cancelling the
    // schedule must cascade-cancel its linked maintenance order too (mirror direction).
    const maintenanceRow = (await screen.findByText('Cambio de aceite y filtro')).closest('tr')!

    await user.click(within(row).getByRole('button', { name: /cancelar/i }))

    await waitFor(() => expect(within(row).getByText('Cancelado')).toBeInTheDocument())
    await waitFor(() => expect(within(maintenanceRow).getByText('Cancelado')).toBeInTheDocument())
  })

  it('edits a maintenance order via the modal and reflects the change in the table', async () => {
    loginAs('WORKSHOP_STAFF')
    const user = userEvent.setup()
    renderWorkshop()

    const row = (await screen.findByText('Cambio de aceite y filtro')).closest('tr')!
    await user.click(within(row).getByRole('button', { name: /editar orden/i }))

    expect(await screen.findByRole('heading', { name: 'Editar orden' })).toBeInTheDocument()
    expect(screen.getByLabelText(/tipo/i)).toHaveValue('Cambio de aceite y filtro')
    // UpdateMaintenanceRequest has no scheduledDate field — the date input must not render in edit mode.
    expect(screen.queryByLabelText(/fecha/i)).not.toBeInTheDocument()

    await user.clear(screen.getByLabelText(/tipo/i))
    await user.type(screen.getByLabelText(/tipo/i), 'Cambio de aceite sintético')

    await user.click(screen.getByRole('button', { name: /guardar cambios/i }))

    await waitFor(() => expect(screen.getByText('Cambio de aceite sintético')).toBeInTheDocument())
    expect(screen.queryByText('Cambio de aceite y filtro')).not.toBeInTheDocument()
  })

  it('edits a workshop schedule via the modal and reflects the change in the agenda', async () => {
    loginAs('ADMIN')
    const user = userEvent.setup()
    renderWorkshop()

    const row = (await within(agendaSection()).findByText('Cambio de aceite')).closest('tr')!
    await user.click(within(row).getByRole('button', { name: /editar entrada/i }))

    expect(await screen.findByRole('heading', { name: 'Editar entrada' })).toBeInTheDocument()
    expect(screen.getByLabelText(/tipo/i)).toHaveValue('Cambio de aceite')
    expect(screen.getByLabelText(/fecha/i)).toHaveValue('2026-07-09')

    await user.selectOptions(screen.getByLabelText(/prioridad/i), 'HIGH')
    await user.click(screen.getByRole('button', { name: /guardar cambios/i }))

    await waitFor(() => {
      const updatedRow = within(agendaSection()).getByText('Cambio de aceite').closest('tr')!
      expect(within(updatedRow).getByText('Alta')).toBeInTheDocument()
    })
  })

  it('starting a workshop schedule transitions its badge from Pendiente to En curso', async () => {
    loginAs('ADMIN')
    const user = userEvent.setup()
    renderWorkshop()

    const row = (await within(agendaSection()).findByText('Cambio de aceite')).closest('tr')!
    expect(within(row).getByText('Pendiente')).toBeInTheDocument()

    await user.click(within(row).getByRole('button', { name: /iniciar/i }))

    await waitFor(() => expect(within(row).getByText('En curso')).toBeInTheDocument())
  })

  it('shows an error message when a double "Iniciar" click on a schedule causes an invalid state transition', async () => {
    loginAs('ADMIN')
    const user = userEvent.setup()
    renderWorkshop()

    const row = (await within(agendaSection()).findByText('Cambio de aceite')).closest('tr')!
    const button = within(row).getByRole('button', { name: /iniciar/i })

    await Promise.all([user.click(button), user.click(button)])

    await waitFor(() =>
      expect(within(row).getByRole('alert')).toHaveTextContent(/no se pudo completar la acción/i),
    )
  })

  it('prefills the schedule time inputs from an existing schedule in edit mode', async () => {
    loginAs('ADMIN')
    const user = userEvent.setup()
    renderWorkshop()

    const row = (await within(agendaSection()).findByText('Cambio de aceite')).closest('tr')!
    await user.click(within(row).getByRole('button', { name: /editar entrada/i }))

    expect(await screen.findByRole('heading', { name: 'Editar entrada' })).toBeInTheDocument()
    expect(screen.getByLabelText(/hora de inicio/i)).toHaveValue('08:00')
    expect(screen.getByLabelText(/hora de fin/i)).toHaveValue('09:00')
  })

  it('creates a workshop schedule entry with a time range and includes it in the request', async () => {
    loginAs('ADMIN')
    const user = userEvent.setup()
    renderWorkshop()

    await screen.findByText('Cambio de aceite y filtro')

    await user.click(screen.getByRole('button', { name: /nueva entrada/i }))

    expect(screen.getByLabelText(/hora de inicio/i)).toHaveValue('')
    expect(screen.getByLabelText(/hora de fin/i)).toHaveValue('')

    await user.type(screen.getByLabelText(/tipo/i), 'Revisión de neumáticos')
    await user.selectOptions(screen.getByLabelText(/vehículo/i), 'vehicle-1')
    await user.type(screen.getByLabelText(/fecha/i), '2026-07-20')
    await user.type(screen.getByLabelText(/hora de inicio/i), '08:00')
    await user.type(screen.getByLabelText(/hora de fin/i), '09:30')

    await user.click(screen.getByRole('button', { name: /crear entrada/i }))

    const row = (await within(agendaSection()).findByText('Revisión de neumáticos')).closest('tr')!
    await user.click(within(row).getByRole('button', { name: /editar entrada/i }))

    expect(await screen.findByRole('heading', { name: 'Editar entrada' })).toBeInTheDocument()
    expect(screen.getByLabelText(/hora de inicio/i)).toHaveValue('08:00')
    expect(screen.getByLabelText(/hora de fin/i)).toHaveValue('09:30')
  })

  it('blocks schedule submission when the end time is before the start time', async () => {
    loginAs('ADMIN')
    const user = userEvent.setup()
    renderWorkshop()

    await screen.findByText('Cambio de aceite y filtro')
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

    await screen.findByText('Cambio de aceite y filtro')
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

  it('paginates the maintenance orders table when there is more than one page', async () => {
    loginAs('ADMIN')
    const user = userEvent.setup()

    server.use(
      http.get('/api/v1/maintenance', ({ request }) => {
        const page = Number(new URL(request.url).searchParams.get('page') ?? 0)
        const record = {
          id: `maintenance-page-${page}`,
          vehicleId: SEED_VEHICLES[0]!.id,
          vehicleLicensePlate: SEED_VEHICLES[0]!.licensePlate,
          vehicleMake: SEED_VEHICLES[0]!.make,
          vehicleModel: SEED_VEHICLES[0]!.model,
          type: page === 0 ? 'Orden más reciente' : 'Orden más antigua',
          description: null,
          usageAtService: null,
          cost: null,
          workshopEntryDate: null,
          workshopExitDate: null,
          workshopEntryTime: null,
          workshopExitTime: null,
          technicianId: null,
          technicianName: null,
          status: 'SCHEDULED',
          category: 'PREVENTIVE',
          createdAt: '2026-07-01T09:00:00Z',
        }
        return HttpResponse.json({ content: [record], page, size: 20, totalElements: 21, totalPages: 2 })
      }),
    )

    renderWorkshop()

    expect(await within(maintenanceSection()).findByText('Orden más reciente')).toBeInTheDocument()
    expect(within(maintenanceSection()).getByText('Página 1 de 2')).toBeInTheDocument()
    expect(within(maintenanceSection()).getByRole('button', { name: /anterior/i })).toBeDisabled()

    await user.click(within(maintenanceSection()).getByRole('button', { name: /siguiente/i }))

    await waitFor(() =>
      expect(within(maintenanceSection()).getByText('Orden más antigua')).toBeInTheDocument(),
    )
    expect(within(maintenanceSection()).getByText('Página 2 de 2')).toBeInTheDocument()
    expect(within(maintenanceSection()).getByRole('button', { name: /siguiente/i })).toBeDisabled()
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

    expect(await within(agendaSection()).findByText('Entrada más reciente')).toBeInTheDocument()
    expect(within(agendaSection()).getByText('Página 1 de 2')).toBeInTheDocument()

    await user.click(within(agendaSection()).getByRole('button', { name: /siguiente/i }))

    await waitFor(() =>
      expect(within(agendaSection()).getByText('Entrada más antigua')).toBeInTheDocument(),
    )
    expect(within(agendaSection()).getByText('Página 2 de 2')).toBeInTheDocument()
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

    // The "Horario del día" widget also queries the schedules endpoint and fails the same way,
    // so the error text now legitimately renders twice on the page — scope to the agenda section.
    expect(
      await within(agendaSection()).findByText('No se pudieron cargar los datos.'),
    ).toBeInTheDocument()
    expect(within(agendaSection()).queryByText('Cambio de aceite')).not.toBeInTheDocument()
  })
})
