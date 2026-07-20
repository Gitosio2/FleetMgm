import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { http, HttpResponse } from 'msw'
import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import { useAuthStore } from '@fleetmgm/store'
import {
  resetJobsMock,
  resetVehiclesMock,
  resetWorkersMock,
  SEED_JOBS,
  SEED_VEHICLES,
  SEED_WORKERS,
} from '@/mocks/handlers'
import { server } from '@/mocks/server'
import { Jobs } from './Jobs'

function renderJobs() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  })

  return render(
    <QueryClientProvider client={queryClient}>
      <Jobs />
    </QueryClientProvider>,
  )
}

function loginAs(role: 'ADMIN' | 'DRIVER') {
  useAuthStore.getState().login({
    email: 'user@fleetmgm.com',
    role,
    accessToken: 'token',
    refreshToken: 'refresh',
  })
}

describe('Jobs', () => {
  beforeEach(() => {
    resetJobsMock()
    resetVehiclesMock()
    resetWorkersMock()
    useAuthStore.getState().logout()
  })

  it('renders the paginated job list', async () => {
    loginAs('ADMIN')
    renderJobs()

    expect(await screen.findByText('Entrega urgente')).toBeInTheDocument()
    expect(screen.getByText('Reparto semanal')).toBeInTheDocument()
    expect(screen.getByText('Entrega finalizada')).toBeInTheDocument()
  })

  it('shows only active jobs assigned to the current driver', async () => {
    loginAs('DRIVER')
    renderJobs()

    expect(await screen.findByText('Entrega urgente')).toBeInTheDocument()
    expect(screen.getByText('Reparto semanal')).toBeInTheDocument()
    expect(screen.queryByText('Entrega finalizada')).not.toBeInTheDocument()
    expect(screen.queryByText('Traslado de excavadora')).not.toBeInTheDocument()
  })

  it('shows "Iniciar" only for a PENDING job, "Completar"/"Cancelar" for an IN_PROGRESS one', async () => {
    loginAs('ADMIN')
    renderJobs()

    const pendingRow = (await screen.findByText('Entrega urgente')).closest('tr')!
    expect(within(pendingRow).getByRole('button', { name: /iniciar/i })).toBeInTheDocument()
    expect(within(pendingRow).queryByRole('button', { name: /completar/i })).not.toBeInTheDocument()
    expect(within(pendingRow).getByRole('button', { name: /cancelar/i })).toBeInTheDocument()

    const inProgressRow = screen.getByText('Reparto semanal').closest('tr')!
    expect(within(inProgressRow).queryByRole('button', { name: /iniciar/i })).not.toBeInTheDocument()
    expect(within(inProgressRow).getByRole('button', { name: /completar/i })).toBeInTheDocument()
    expect(within(inProgressRow).getByRole('button', { name: /cancelar/i })).toBeInTheDocument()

    const completedRow = screen.getByText('Entrega finalizada').closest('tr')!
    expect(within(completedRow).queryByRole('button', { name: /iniciar/i })).not.toBeInTheDocument()
    expect(within(completedRow).queryByRole('button', { name: /completar/i })).not.toBeInTheDocument()
    expect(within(completedRow).queryByRole('button', { name: /cancelar/i })).not.toBeInTheDocument()
  })

  it('starting a job transitions its status badge from Pendiente to En curso', async () => {
    loginAs('ADMIN')
    const user = userEvent.setup()
    renderJobs()

    const row = (await screen.findByText('Entrega urgente')).closest('tr')!
    expect(within(row).getByText('Pendiente')).toBeInTheDocument()

    await user.click(within(row).getByRole('button', { name: /iniciar/i }))
    const dialog = await screen.findByRole('dialog')
    await user.click(within(dialog).getByRole('button', { name: /^iniciar$/i }))

    await waitFor(() => expect(within(row).getByText('En curso')).toBeInTheDocument())
    expect(within(row).getByRole('button', { name: /completar/i })).toBeInTheDocument()
  })

  it('completing a job transitions its status badge to Completado', async () => {
    loginAs('ADMIN')
    const user = userEvent.setup()
    renderJobs()

    const row = (await screen.findByText('Reparto semanal')).closest('tr')!
    expect(within(row).getByText('En curso')).toBeInTheDocument()

    await user.click(within(row).getByRole('button', { name: /completar/i }))
    const dialog = await screen.findByRole('dialog')
    await user.click(within(dialog).getByRole('button', { name: /^completar$/i }))

    await waitFor(() => expect(within(row).getByText('Completado')).toBeInTheDocument())
  })

  it('blocks completing a job with a usage value below the vehicle\'s current km and keeps it En curso', async () => {
    loginAs('ADMIN')
    const user = userEvent.setup()
    renderJobs()

    // Reparto semanal -> vehicle-1, currentKm 15000, startUsageValue 14500.
    const row = (await screen.findByText('Reparto semanal')).closest('tr')!
    expect(within(row).getByText('En curso')).toBeInTheDocument()

    await user.click(within(row).getByRole('button', { name: /completar/i }))
    const dialog = await screen.findByRole('dialog')
    await user.type(within(dialog).getByLabelText('Kilómetros actuales'), '10000')
    await user.click(within(dialog).getByRole('button', { name: /^completar$/i }))

    expect(await within(dialog).findByRole('alert')).toHaveTextContent(
      /el valor ingresado es menor al que ya tiene registrado el vehículo/i,
    )
    expect(within(row).getByText('En curso')).toBeInTheDocument()
    expect(screen.getByRole('dialog')).toBeInTheDocument()
  })

  it('cancelling a job transitions its status badge to Cancelado', async () => {
    loginAs('ADMIN')
    const user = userEvent.setup()
    renderJobs()

    const row = (await screen.findByText('Entrega urgente')).closest('tr')!

    await user.click(within(row).getByRole('button', { name: /cancelar/i }))

    await waitFor(() => expect(within(row).getByText('Cancelado')).toBeInTheDocument())
  })

  it('still starts the job when a double "Iniciar" click races, without leaving a stuck dialog', async () => {
    loginAs('ADMIN')
    const user = userEvent.setup()
    renderJobs()

    const row = (await screen.findByText('Entrega urgente')).closest('tr')!
    await user.click(within(row).getByRole('button', { name: /iniciar/i }))

    const dialog = await screen.findByRole('dialog')
    const confirmButton = within(dialog).getByRole('button', { name: /^iniciar$/i })
    // Concurrent (Promise.all), not sequential — awaiting each click in turn would let React
    // flush the confirm button's disabled={isPending} state between them, and user-event refuses
    // to click a disabled element, so the second click would silently never fire. One of the two
    // requests wins and starts the job; the other lands on an already-IN_PROGRESS job and gets a
    // 409 (JOB_INVALID_STATE_TRANSITION) — but since the modal only closes onSuccess, that
    // redundant failure has nothing left to attach an error to once the successful one already
    // closed the dialog. The job did start, which is the outcome that actually matters here.
    await Promise.all([user.click(confirmButton), user.click(confirmButton)])

    await waitFor(() => expect(within(row).getByText('En curso')).toBeInTheDocument())
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
  })

  it('shows "<make> <model>" in the job list when the vehicle has no license plate', async () => {
    loginAs('ADMIN')
    renderJobs()

    const row = (await screen.findByText('Traslado de excavadora')).closest('tr')!
    const [, heavyMachinery] = SEED_VEHICLES
    expect(
      within(row).getByText(`${heavyMachinery!.make} ${heavyMachinery!.model}`),
    ).toBeInTheDocument()
  })

  it('hides the create button for the DRIVER role', async () => {
    loginAs('DRIVER')
    renderJobs()

    await screen.findByText('Entrega urgente')
    expect(screen.queryByRole('button', { name: /nuevo trabajo/i })).not.toBeInTheDocument()
  })

  it('editing a job to set actual start/end dates shows them in the table', async () => {
    loginAs('ADMIN')
    const user = userEvent.setup()
    renderJobs()

    const row = (await screen.findByText('Traslado de excavadora')).closest('tr')!
    await user.click(within(row).getByRole('button', { name: /editar trabajo/i }))

    const dialog = await screen.findByRole('dialog')
    await user.type(within(dialog).getByLabelText('Inicio real'), '2026-07-10T12:00')
    await user.type(within(dialog).getByLabelText('Fin real'), '2026-07-12T09:00')
    await user.click(within(dialog).getByRole('button', { name: /guardar cambios/i }))

    const expectedStartDate = new Date('2026-07-10T12:00').toLocaleDateString('es-ES')
    const expectedEndDate = new Date('2026-07-12T09:00').toLocaleDateString('es-ES')
    await waitFor(() => expect(within(row).getByText(expectedStartDate)).toBeInTheDocument())
    expect(within(row).getByText(expectedEndDate)).toBeInTheDocument()
  })

  it('starting a job with a manually-set actualStart preserves the original date', async () => {
    loginAs('ADMIN')
    const user = userEvent.setup()
    renderJobs()

    const row = (await screen.findByText('Traslado programado')).closest('tr')!
    const seedJob = SEED_JOBS.find((job) => job.id === 'job-5')!
    const expectedDate = new Date(seedJob.actualStart!).toLocaleDateString('es-ES')
    expect(within(row).getByText(expectedDate)).toBeInTheDocument()

    await user.click(within(row).getByRole('button', { name: /iniciar/i }))
    const dialog = await screen.findByRole('dialog')
    await user.click(within(dialog).getByRole('button', { name: /^iniciar$/i }))

    await waitFor(() => expect(within(row).getByText('En curso')).toBeInTheDocument())
    expect(within(row).getByText(expectedDate)).toBeInTheDocument()
  })

  it('lists jobs with no actual start first, then most recently started first', async () => {
    loginAs('ADMIN')
    renderJobs()

    await screen.findByText('Entrega urgente')
    const titles = screen.getAllByRole('row').slice(1).map((row) => within(row).getAllByRole('cell')[0]!.textContent)

    expect(titles).toEqual([
      'Entrega urgente',
      'Traslado de excavadora',
      'Traslado programado',
      'Reparto semanal',
      'Entrega finalizada',
    ])
  })

  describe('actual date timezone round-trip', () => {
    // This frontend project has no @types/node in its ambient scope (see src/test/setup.ts) —
    // `process` still exists at runtime under vitest/Node, just untyped here, so it's cast locally
    // rather than widening the project's ambient types just for this one test.
    const nodeProcess = (globalThis as unknown as { process: { env: Record<string, string | undefined> } }).process
    const ORIGINAL_TZ = nodeProcess.env.TZ

    beforeEach(() => {
      nodeProcess.env.TZ = 'Europe/Madrid'
    })

    afterEach(() => {
      nodeProcess.env.TZ = ORIGINAL_TZ
    })

    it('keeps actualStart stable when the form is saved without touching it', async () => {
      loginAs('ADMIN')
      const user = userEvent.setup()

      let capturedActualStart: string | null | undefined
      server.use(
        http.put('/api/v1/jobs/:id', async ({ request, params }) => {
          const body = (await request.json()) as { actualStart?: string | null }
          capturedActualStart = body.actualStart
          const seedJob = SEED_JOBS.find((job) => job.id === params.id)!
          return HttpResponse.json({ ...seedJob, actualStart: body.actualStart ?? null })
        }),
      )

      renderJobs()

      const row = (await screen.findByText('Traslado programado')).closest('tr')!
      await user.click(within(row).getByRole('button', { name: /editar trabajo/i }))

      const dialog = await screen.findByRole('dialog')
      await user.click(within(dialog).getByRole('button', { name: /guardar cambios/i }))

      await waitFor(() => expect(capturedActualStart).not.toBeUndefined())

      const seedJob = SEED_JOBS.find((job) => job.id === 'job-5')!
      expect(new Date(capturedActualStart!).getTime()).toBe(new Date(seedJob.actualStart!).getTime())
    })
  })

  it('shows an explicit error message when actualStart is set in the future', async () => {
    loginAs('ADMIN')
    const user = userEvent.setup()
    renderJobs()

    const row = (await screen.findByText('Entrega urgente')).closest('tr')!
    await user.click(within(row).getByRole('button', { name: /editar trabajo/i }))

    const dialog = await screen.findByRole('dialog')
    await user.type(within(dialog).getByLabelText(/inicio real/i), '2099-01-01T10:00')
    await user.click(within(dialog).getByRole('button', { name: /guardar cambios/i }))

    expect(
      await screen.findByText('El inicio o el fin real no pueden ser una fecha futura.'),
    ).toBeInTheDocument()
  })

  it('narrows the job list with the title filter', async () => {
    loginAs('ADMIN')
    const user = userEvent.setup()
    renderJobs()

    await screen.findByText('Entrega urgente')

    await user.type(screen.getByLabelText(/buscar por título/i), 'urgente')

    await waitFor(() => {
      expect(screen.getByText('Entrega urgente')).toBeInTheDocument()
    })
    expect(screen.queryByText('Reparto semanal')).not.toBeInTheDocument()
  })

  it('narrows the job list with the origin filter', async () => {
    loginAs('ADMIN')
    const user = userEvent.setup()
    renderJobs()

    await screen.findByText('Entrega urgente')

    await user.type(screen.getByLabelText(/buscar por origen/i), 'Taller')

    await waitFor(() => {
      expect(screen.getByText('Traslado de excavadora')).toBeInTheDocument()
    })
    expect(screen.queryByText('Entrega urgente')).not.toBeInTheDocument()
  })

  it('narrows the job list with the destination filter', async () => {
    loginAs('ADMIN')
    const user = userEvent.setup()
    renderJobs()

    await screen.findByText('Entrega urgente')

    await user.type(screen.getByLabelText(/buscar por destino/i), 'Norte')

    await waitFor(() => {
      expect(screen.getByText('Traslado de excavadora')).toBeInTheDocument()
    })
    expect(screen.queryByText('Entrega urgente')).not.toBeInTheDocument()
  })

  it('narrows the job list with the vehicle filter', async () => {
    loginAs('ADMIN')
    const user = userEvent.setup()
    renderJobs()

    await screen.findByText('Traslado de excavadora')

    const [, heavyMachinery] = SEED_VEHICLES
    await user.selectOptions(screen.getByLabelText(/filtrar por vehículo/i), heavyMachinery!.id)

    await waitFor(() => {
      expect(screen.getByText('Traslado de excavadora')).toBeInTheDocument()
    })
    expect(screen.queryByText('Entrega urgente')).not.toBeInTheDocument()
  })

  it('narrows the job list with the driver filter', async () => {
    loginAs('ADMIN')
    const user = userEvent.setup()
    renderJobs()

    await screen.findByText('Entrega urgente')

    await user.selectOptions(screen.getByLabelText(/filtrar por conductor/i), SEED_WORKERS[0]!.id)

    await waitFor(() => {
      expect(screen.getByText('Entrega urgente')).toBeInTheDocument()
    })
    expect(screen.queryByText('Traslado de excavadora')).not.toBeInTheDocument()
  })

  it('narrows the job list with the status filter', async () => {
    loginAs('ADMIN')
    const user = userEvent.setup()
    renderJobs()

    await screen.findByText('Reparto semanal')

    await user.selectOptions(screen.getByLabelText(/filtrar por estado/i), 'IN_PROGRESS')

    await waitFor(() => {
      expect(screen.getByText('Reparto semanal')).toBeInTheDocument()
    })
    expect(screen.queryByText('Entrega urgente')).not.toBeInTheDocument()
  })

  it('narrows the job list with the actual start date range filter', async () => {
    loginAs('ADMIN')
    const user = userEvent.setup()
    renderJobs()

    await screen.findByText('Traslado programado')

    await user.type(screen.getByLabelText('Inicio desde'), '2026-07-07')
    await user.type(screen.getByLabelText('Inicio hasta'), '2026-07-09')

    await waitFor(() => {
      expect(screen.getByText('Traslado programado')).toBeInTheDocument()
    })
    expect(screen.queryByText('Reparto semanal')).not.toBeInTheDocument()
  })

  it('narrows the job list with the actual end date range filter', async () => {
    loginAs('ADMIN')
    const user = userEvent.setup()
    renderJobs()

    await screen.findByText('Entrega finalizada')

    await user.type(screen.getByLabelText('Fin desde'), '2026-06-19')
    await user.type(screen.getByLabelText('Fin hasta'), '2026-06-21')

    await waitFor(() => {
      expect(screen.getByText('Entrega finalizada')).toBeInTheDocument()
    })
    expect(screen.queryByText('Reparto semanal')).not.toBeInTheDocument()
  })

  it('shows an error message when the job list query fails', async () => {
    loginAs('ADMIN')
    server.use(
      http.get('/api/v1/jobs', () =>
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
    renderJobs()

    expect(await screen.findByText('No se pudieron cargar los datos.')).toBeInTheDocument()
    expect(screen.queryByText('Entrega urgente')).not.toBeInTheDocument()
  })

  it('offers a vehicle from beyond the first page in the vehicle filter select', async () => {
    loginAs('ADMIN')
    server.use(
      http.get('/api/v1/vehicles', ({ request }) => {
        const page = Number(new URL(request.url).searchParams.get('page') ?? 0)
        // Two pages regardless of the requested size — forces useAllVehicles' page loop to run
        // more than once, proving it doesn't stop after the first page (the bug this test guards
        // against: a plain `useVehicles({}, 0, 100)` silently dropped everything past page 0).
        const record = {
          id: page === 0 ? 'vehicle-page-0' : 'vehicle-page-1',
          vehicleCategory: 'LIGHT_VEHICLE',
          usageMeasure: 'KILOMETERS',
          make: 'Nissan',
          model: page === 0 ? 'Navara' : 'Beyond First Page',
          year: 2021,
          licensePlate: page === 0 ? '0000AAA' : '9999ZZZ',
          heavySubtype: null,
          vin: `VIN-page-${page}`,
          color: 'Blue',
          status: 'ACTIVE',
          currentKm: 1000,
          currentHours: null,
          acquisitionType: 'PURCHASED',
          acquisitionDate: '2021-01-01',
          purchasePrice: 20000,
          amortizationYears: 5,
          monthlyFee: null,
          contractEndDate: null,
          createdAt: '2026-01-01T09:00:00Z',
        }
        return HttpResponse.json({ content: [record], page, size: 200, totalElements: 2, totalPages: 2 })
      }),
    )
    renderJobs()

    await screen.findByText('Entrega urgente')

    expect(
      await within(screen.getByLabelText(/filtrar por vehículo/i)).findByRole('option', {
        name: /9999ZZZ/,
      }),
    ).toBeInTheDocument()
  })
})
