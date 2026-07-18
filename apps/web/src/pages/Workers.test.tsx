import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { beforeEach, describe, expect, it } from 'vitest'
import { useAuthStore } from '@fleetmgm/store'
import { resetAssignmentsMock, resetWorkersMock, SEED_ASSIGNMENTS, SEED_WORKERS } from '@/mocks/handlers'
import { Workers } from './Workers'

function renderWorkers() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  })

  return render(
    <QueryClientProvider client={queryClient}>
      <Workers />
    </QueryClientProvider>,
  )
}

const [FIRST_WORKER, SECOND_WORKER] = SEED_WORKERS

function loginAs(role: 'ADMIN' | 'DRIVER') {
  useAuthStore.getState().login({
    email: 'user@fleetmgm.com',
    role,
    accessToken: 'token',
    refreshToken: 'refresh',
  })
}

describe('Workers', () => {
  beforeEach(() => {
    resetWorkersMock()
    resetAssignmentsMock()
    useAuthStore.getState().logout()
  })

  it('renders the paginated worker list', async () => {
    loginAs('ADMIN')
    renderWorkers()

    for (const worker of SEED_WORKERS) {
      expect(await screen.findByText(worker.fullName)).toBeInTheDocument()
    }
  })

  it('shows only the assigned profile for the DRIVER role', async () => {
    loginAs('DRIVER')
    renderWorkers()

    expect(await screen.findByText(FIRST_WORKER!.fullName)).toBeInTheDocument()
    expect(screen.queryByText(SECOND_WORKER!.fullName)).not.toBeInTheDocument()
  })

  it('hides management actions for the DRIVER role', async () => {
    loginAs('DRIVER')
    renderWorkers()

    await screen.findByText(FIRST_WORKER!.fullName)

    expect(screen.queryByRole('button', { name: /nuevo trabajador/i })).not.toBeInTheDocument()
    expect(screen.queryByLabelText('Editar trabajador')).not.toBeInTheDocument()
    expect(screen.queryByLabelText('Eliminar trabajador')).not.toBeInTheDocument()
  })

  it('shows the assigned vehicle license plate for a worker with an active assignment', async () => {
    loginAs('ADMIN')
    renderWorkers()

    const [activeAssignment] = SEED_ASSIGNMENTS
    expect(await screen.findByText(activeAssignment!.vehicleLicensePlate!)).toBeInTheDocument()
  })

  it('shows "<make> <model>" for a worker assigned to a vehicle without a license plate', async () => {
    loginAs('ADMIN')
    renderWorkers()

    const heavyMachineryAssignment = SEED_ASSIGNMENTS.find((assignment) => !assignment.vehicleLicensePlate)!
    expect(
      await screen.findByText(`${heavyMachineryAssignment.vehicleMake} ${heavyMachineryAssignment.vehicleModel}`),
    ).toBeInTheDocument()
  })

  it('shows a dash for a worker without an active assignment', async () => {
    loginAs('ADMIN')
    renderWorkers()

    const thirdWorker = SEED_WORKERS.find((worker) => worker.id === 'worker-3')!
    const row = (await screen.findByText(thirdWorker.fullName)).closest('tr')!
    expect(row).toHaveTextContent('—')
  })

  it('narrows the worker list with the name filter', async () => {
    loginAs('ADMIN')
    const user = userEvent.setup()
    renderWorkers()

    await screen.findByText(FIRST_WORKER!.fullName)

    await user.type(screen.getByLabelText(/buscar por nombre/i), 'Laura')

    await waitFor(() => {
      expect(screen.getByText(SECOND_WORKER!.fullName)).toBeInTheDocument()
    })
    expect(screen.queryByText(FIRST_WORKER!.fullName)).not.toBeInTheDocument()
  })

  it('narrows the worker list with the document filter', async () => {
    loginAs('ADMIN')
    const user = userEvent.setup()
    renderWorkers()

    await screen.findByText(FIRST_WORKER!.fullName)

    await user.type(screen.getByLabelText(/buscar por documento/i), FIRST_WORKER!.nationalId.slice(0, 4))

    await waitFor(() => {
      expect(screen.getByText(FIRST_WORKER!.fullName)).toBeInTheDocument()
    })
    expect(screen.queryByText(SECOND_WORKER!.fullName)).not.toBeInTheDocument()
  })

  it('narrows the worker list with the role filter', async () => {
    loginAs('ADMIN')
    const user = userEvent.setup()
    renderWorkers()

    await screen.findByText(SECOND_WORKER!.fullName)

    await user.selectOptions(screen.getByLabelText(/filtrar por rol/i), 'TECHNICIAN')

    await waitFor(() => {
      expect(screen.getByText(SECOND_WORKER!.fullName)).toBeInTheDocument()
    })
    expect(screen.queryByText(FIRST_WORKER!.fullName)).not.toBeInTheDocument()
  })
})
