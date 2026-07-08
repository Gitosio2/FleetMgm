import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { beforeEach, describe, expect, it } from 'vitest'
import { useAuthStore } from '@fleetmgm/store'
import { resetJobsMock, resetVehiclesMock, resetWorkersMock } from '@/mocks/handlers'
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

  it('hides the create button for the DRIVER role', async () => {
    loginAs('DRIVER')
    renderJobs()

    await screen.findByText('Entrega urgente')
    expect(screen.queryByRole('button', { name: /nuevo trabajo/i })).not.toBeInTheDocument()
  })
})
