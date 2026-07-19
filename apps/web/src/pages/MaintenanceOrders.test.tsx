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
  SEED_WORKERS,
} from '@/mocks/handlers'
import { server } from '@/mocks/server'
import { MaintenanceOrders } from './MaintenanceOrders'

function renderMaintenanceOrders() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  })

  return render(
    <QueryClientProvider client={queryClient}>
      <MaintenanceOrders />
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

describe('MaintenanceOrders', () => {
  beforeEach(() => {
    resetMaintenanceMock()
    resetWorkshopSchedulesMock()
    resetVehiclesMock()
    resetWorkersMock()
    useAuthStore.getState().logout()
  })

  it('lists maintenance orders read-only, with no creation button', async () => {
    loginAs('WORKSHOP_STAFF')
    renderMaintenanceOrders()

    await screen.findByText('Cambio de aceite y filtro')
    expect(screen.getByText('Cambio de pastillas de freno')).toBeInTheDocument()

    expect(screen.queryByRole('button', { name: /nueva orden/i })).not.toBeInTheDocument()
  })

  it('shows no lifecycle action buttons — only Editar', async () => {
    loginAs('ADMIN')
    renderMaintenanceOrders()

    const row = (await screen.findByText('Cambio de aceite y filtro')).closest('tr')!
    expect(within(row).queryByRole('button', { name: /iniciar/i })).not.toBeInTheDocument()
    expect(within(row).queryByRole('button', { name: /completar/i })).not.toBeInTheDocument()
    expect(within(row).queryByRole('button', { name: /cancelar/i })).not.toBeInTheDocument()
    expect(within(row).getByRole('button', { name: /editar orden/i })).toBeInTheDocument()
  })

  it('shows the Costo column, formatted or as a dash when not yet costed', async () => {
    loginAs('ADMIN')
    renderMaintenanceOrders()

    const costedRow = (await screen.findByText('Cambio de filtro')).closest('tr')!
    expect(within(costedRow).getByText('85,50€')).toBeInTheDocument()

    const uncostedRow = screen.getByText('Cambio de aceite y filtro').closest('tr')!
    expect(within(uncostedRow).getByText('—')).toBeInTheDocument()
  })

  it('shows "<make> <model>" when the vehicle has no license plate', async () => {
    loginAs('ADMIN')
    renderMaintenanceOrders()

    const row = (await screen.findByText('Cambio de filtro')).closest('tr')!
    const [, heavyMachinery] = SEED_VEHICLES
    expect(within(row).getByText(`${heavyMachinery!.make} ${heavyMachinery!.model}`)).toBeInTheDocument()
  })

  it('edits a maintenance order via the modal and reflects the change in the table', async () => {
    loginAs('WORKSHOP_STAFF')
    const user = userEvent.setup()
    renderMaintenanceOrders()

    const row = (await screen.findByText('Cambio de aceite y filtro')).closest('tr')!
    await user.click(within(row).getByRole('button', { name: /editar orden/i }))

    const dialog = await screen.findByRole('dialog')
    expect(await within(dialog).findByRole('heading', { name: 'Editar orden' })).toBeInTheDocument()
    expect(within(dialog).getByLabelText(/tipo/i)).toHaveValue('Cambio de aceite y filtro')

    await user.clear(within(dialog).getByLabelText(/tipo/i))
    await user.type(within(dialog).getByLabelText(/tipo/i), 'Cambio de aceite sintético')

    await user.click(screen.getByRole('button', { name: /guardar cambios/i }))

    await waitFor(() => expect(screen.getByText('Cambio de aceite sintético')).toBeInTheDocument())
    expect(screen.queryByText('Cambio de aceite y filtro')).not.toBeInTheDocument()
  })

  it('edits the cost of a maintenance order and reflects it in the table', async () => {
    loginAs('WORKSHOP_STAFF')
    const user = userEvent.setup()
    renderMaintenanceOrders()

    const row = (await screen.findByText('Cambio de aceite y filtro')).closest('tr')!
    await user.click(within(row).getByRole('button', { name: /editar orden/i }))

    expect(await screen.findByRole('heading', { name: 'Editar orden' })).toBeInTheDocument()
    const costInput = screen.getByLabelText(/coste/i)
    expect(costInput).toHaveValue(null)

    await user.type(costInput, '120.50')
    await user.click(screen.getByRole('button', { name: /guardar cambios/i }))

    await waitFor(() => expect(screen.queryByRole('heading', { name: 'Editar orden' })).not.toBeInTheDocument())
    const updatedRow = screen.getByText('Cambio de aceite y filtro').closest('tr')!
    expect(within(updatedRow).getByText('120,50€')).toBeInTheDocument()
  })

  it('edits the cost of an in-progress maintenance order', async () => {
    loginAs('WORKSHOP_STAFF')
    const user = userEvent.setup()
    renderMaintenanceOrders()

    const row = (await screen.findByText('Cambio de pastillas de freno')).closest('tr')!
    await user.click(within(row).getByRole('button', { name: /editar orden/i }))

    expect(await screen.findByRole('heading', { name: 'Editar orden' })).toBeInTheDocument()
    const costInput = screen.getByLabelText(/coste/i)
    await user.type(costInput, '75.25')
    await user.click(screen.getByRole('button', { name: /guardar cambios/i }))

    await waitFor(() => expect(screen.queryByRole('heading', { name: 'Editar orden' })).not.toBeInTheDocument())
    const updatedRow = screen.getByText('Cambio de pastillas de freno').closest('tr')!
    expect(within(updatedRow).getByText('75,25€')).toBeInTheDocument()
  })

  it('edits the cost of a completed maintenance order', async () => {
    loginAs('WORKSHOP_STAFF')
    const user = userEvent.setup()
    renderMaintenanceOrders()

    const row = (await screen.findByText('Cambio de filtro')).closest('tr')!
    await user.click(within(row).getByRole('button', { name: /editar orden/i }))

    expect(await screen.findByRole('heading', { name: 'Editar orden' })).toBeInTheDocument()
    const costInput = screen.getByLabelText(/coste/i)
    expect(costInput).toHaveValue(85.5)

    await user.clear(costInput)
    await user.type(costInput, '99.99')
    await user.click(screen.getByRole('button', { name: /guardar cambios/i }))

    await waitFor(() => expect(screen.queryByRole('heading', { name: 'Editar orden' })).not.toBeInTheDocument())
    const updatedRow = screen.getByText('Cambio de filtro').closest('tr')!
    expect(within(updatedRow).getByText('99,99€')).toBeInTheDocument()
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

    renderMaintenanceOrders()

    expect(await screen.findByText('Orden más reciente')).toBeInTheDocument()
    expect(screen.getByText('Página 1 de 2')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /anterior/i })).toBeDisabled()

    await user.click(screen.getByRole('button', { name: /siguiente/i }))

    await waitFor(() => expect(screen.getByText('Orden más antigua')).toBeInTheDocument())
    expect(screen.getByText('Página 2 de 2')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /siguiente/i })).toBeDisabled()
  })

  it('narrows the maintenance list with the vehicle filter', async () => {
    loginAs('ADMIN')
    const user = userEvent.setup()
    renderMaintenanceOrders()

    await screen.findByText('Cambio de aceite y filtro')

    const [, heavyMachinery] = SEED_VEHICLES
    await user.selectOptions(screen.getByLabelText(/filtrar por vehículo/i), heavyMachinery!.id)

    await waitFor(() => {
      expect(screen.getByText('Cambio de filtro')).toBeInTheDocument()
    })
    expect(screen.queryByText('Cambio de aceite y filtro')).not.toBeInTheDocument()
  })

  it('narrows the maintenance list with the type filter', async () => {
    loginAs('ADMIN')
    const user = userEvent.setup()
    renderMaintenanceOrders()

    await screen.findByText('Cambio de aceite y filtro')

    await user.type(screen.getByLabelText(/buscar por tipo/i), 'pastillas')

    await waitFor(() => {
      expect(screen.getByText('Cambio de pastillas de freno')).toBeInTheDocument()
    })
    expect(screen.queryByText('Cambio de aceite y filtro')).not.toBeInTheDocument()
  })

  it('narrows the maintenance list with the category filter', async () => {
    loginAs('ADMIN')
    const user = userEvent.setup()
    renderMaintenanceOrders()

    await screen.findByText('Cambio de pastillas de freno')

    await user.selectOptions(screen.getByLabelText(/filtrar por categoría/i), 'CORRECTIVE')

    await waitFor(() => {
      expect(screen.getByText('Cambio de pastillas de freno')).toBeInTheDocument()
    })
    expect(screen.queryByText('Cambio de aceite y filtro')).not.toBeInTheDocument()
  })

  it('narrows the maintenance list with the status filter', async () => {
    loginAs('ADMIN')
    const user = userEvent.setup()
    renderMaintenanceOrders()

    await screen.findByText('Cambio de filtro')

    await user.selectOptions(screen.getByLabelText(/filtrar por estado/i), 'COMPLETED')

    await waitFor(() => {
      expect(screen.getByText('Cambio de filtro')).toBeInTheDocument()
    })
    expect(screen.queryByText('Cambio de aceite y filtro')).not.toBeInTheDocument()
  })

  it('narrows the maintenance list with the technician filter', async () => {
    loginAs('ADMIN')
    const user = userEvent.setup()
    renderMaintenanceOrders()

    await screen.findByText('Cambio de filtro')

    await user.selectOptions(screen.getByLabelText(/filtrar por técnico/i), SEED_WORKERS[1]!.id)

    await waitFor(() => {
      expect(screen.getByText('Cambio de aceite y filtro')).toBeInTheDocument()
    })
    expect(screen.queryByText('Cambio de filtro')).not.toBeInTheDocument()
  })

  it('narrows the maintenance list with the cost range filter', async () => {
    loginAs('ADMIN')
    const user = userEvent.setup()
    renderMaintenanceOrders()

    await screen.findByText('Cambio de aceite y filtro')

    await user.type(screen.getByLabelText(/importe desde/i), '50')
    await user.type(screen.getByLabelText(/importe hasta/i), '100')

    await waitFor(() => {
      expect(screen.getByText('Cambio de filtro')).toBeInTheDocument()
    })
    expect(screen.queryByText('Cambio de aceite y filtro')).not.toBeInTheDocument()
  })

  it('shows an error message when the maintenance query fails', async () => {
    loginAs('ADMIN')
    server.use(
      http.get('/api/v1/maintenance', () =>
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
    renderMaintenanceOrders()

    expect(await screen.findByText('No se pudieron cargar los datos.')).toBeInTheDocument()
    expect(screen.queryByText('Cambio de aceite y filtro')).not.toBeInTheDocument()
  })
})
