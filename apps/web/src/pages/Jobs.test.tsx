import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { http, HttpResponse } from 'msw'
import { beforeEach, describe, expect, it } from 'vitest'
import { useAuthStore } from '@fleetmgm/store'
import { resetJobsMock, resetVehiclesMock, resetWorkersMock, SEED_JOBS, SEED_VEHICLES } from '@/mocks/handlers'
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

    await waitFor(() => expect(within(row).getByText('Completado')).toBeInTheDocument())
  })

  it('cancelling a job transitions its status badge to Cancelado', async () => {
    loginAs('ADMIN')
    const user = userEvent.setup()
    renderJobs()

    const row = (await screen.findByText('Entrega urgente')).closest('tr')!

    await user.click(within(row).getByRole('button', { name: /cancelar/i }))

    await waitFor(() => expect(within(row).getByText('Cancelado')).toBeInTheDocument())
  })

  it('shows an error message when a double "Iniciar" click causes an invalid state transition', async () => {
    loginAs('ADMIN')
    const user = userEvent.setup()
    renderJobs()

    const row = (await screen.findByText('Entrega urgente')).closest('tr')!
    const button = within(row).getByRole('button', { name: /iniciar/i })

    await Promise.all([user.click(button), user.click(button)])

    await waitFor(() =>
      expect(within(row).getByRole('alert')).toHaveTextContent(/no se pudo completar la acción/i),
    )
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
    await user.type(within(dialog).getByLabelText('Inicio real'), '2026-07-20T12:00')
    await user.type(within(dialog).getByLabelText('Fin real'), '2026-07-22T09:00')
    await user.click(within(dialog).getByRole('button', { name: /guardar cambios/i }))

    const expectedStartDate = new Date('2026-07-20T12:00').toLocaleDateString('es-ES')
    const expectedEndDate = new Date('2026-07-22T09:00').toLocaleDateString('es-ES')
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
    const ORIGINAL_TZ = process.env.TZ

    beforeEach(() => {
      process.env.TZ = 'Europe/Madrid'
    })

    afterEach(() => {
      process.env.TZ = ORIGINAL_TZ
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
})
