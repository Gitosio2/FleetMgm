import type { ReactNode } from 'react'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import {
  resetAssignmentsMock,
  resetWorkersMock,
  SEED_ASSIGNMENTS,
  SEED_VEHICLES,
  SEED_WORKERS,
} from '@/mocks/handlers'
import { AssignmentModal } from './AssignmentModal'
import { AssignmentHistory } from './AssignmentHistory'
import { VehicleAssignmentPanel } from './VehicleAssignmentPanel'

function renderWithClient(ui: ReactNode) {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  })

  return render(<QueryClientProvider client={queryClient}>{ui}</QueryClientProvider>)
}

const [FIRST_ASSIGNMENT] = SEED_ASSIGNMENTS
const [, SECOND_VEHICLE] = SEED_VEHICLES
const UNASSIGNED_DRIVER = SEED_WORKERS.find((worker) => worker.id === 'worker-3')!

describe('Assignments', () => {
  beforeEach(() => {
    resetAssignmentsMock()
    resetWorkersMock()
  })

  it('creates a new assignment from the modal', async () => {
    const user = userEvent.setup()
    const onAssigned = vi.fn()

    renderWithClient(
      <AssignmentModal
        open
        onOpenChange={() => {}}
        vehicleId={SECOND_VEHICLE!.id}
        vehicleLabel={`${SECOND_VEHICLE!.make} ${SECOND_VEHICLE!.model}`}
        drivers={[UNASSIGNED_DRIVER]}
        onAssigned={onAssigned}
      />,
    )

    await user.selectOptions(screen.getByLabelText('Conductor'), UNASSIGNED_DRIVER.id)
    await user.type(screen.getByLabelText('Fecha de inicio'), '2026-03-01')
    await user.click(screen.getByRole('button', { name: /^asignar$/i }))

    await waitFor(() =>
      expect(onAssigned).toHaveBeenCalledWith(
        expect.objectContaining({
          driverId: UNASSIGNED_DRIVER.id,
          vehicleId: SECOND_VEHICLE!.id,
          active: true,
        }),
      ),
    )
  })

  it('renders the paginated assignment history for a driver', async () => {
    renderWithClient(<AssignmentHistory workerId={FIRST_ASSIGNMENT!.driverId} canManage />)

    expect(await screen.findByText(FIRST_ASSIGNMENT!.vehicleLicensePlate!)).toBeInTheDocument()
    expect(screen.getByText('Activa')).toBeInTheDocument()
  })

  it('shows "<make> <model>" in the history when the vehicle has no license plate', async () => {
    const heavyMachineryAssignment = SEED_ASSIGNMENTS.find((assignment) => !assignment.vehicleLicensePlate)!

    renderWithClient(<AssignmentHistory workerId={heavyMachineryAssignment.driverId} canManage />)

    expect(
      await screen.findByText(`${heavyMachineryAssignment.vehicleMake} ${heavyMachineryAssignment.vehicleModel}`),
    ).toBeInTheDocument()
  })

  it('finalizing an assignment updates the history list', async () => {
    const user = userEvent.setup()
    renderWithClient(<AssignmentHistory workerId={FIRST_ASSIGNMENT!.driverId} canManage />)

    await screen.findByText('Activa')
    await user.click(screen.getByRole('button', { name: /finalizar asignación/i }))

    await waitFor(() => expect(screen.queryByText('Activa')).not.toBeInTheDocument())
  })

  it('hides the finalize action for the DRIVER role', async () => {
    renderWithClient(<AssignmentHistory workerId={FIRST_ASSIGNMENT!.driverId} canManage={false} />)

    await screen.findByText('Activa')
    expect(screen.queryByRole('button', { name: /finalizar asignación/i })).not.toBeInTheDocument()
  })

  it('shows the Spanish error message when the assignment conflicts', async () => {
    const user = userEvent.setup()
    const onAssigned = vi.fn()
    const alreadyAssignedDriver = SEED_WORKERS.find((worker) => worker.id === FIRST_ASSIGNMENT!.driverId)!

    renderWithClient(
      <AssignmentModal
        open
        onOpenChange={() => {}}
        vehicleId={SECOND_VEHICLE!.id}
        vehicleLabel={`${SECOND_VEHICLE!.make} ${SECOND_VEHICLE!.model}`}
        drivers={[alreadyAssignedDriver]}
        onAssigned={onAssigned}
      />,
    )

    await user.selectOptions(screen.getByLabelText('Conductor'), alreadyAssignedDriver.id)
    await user.type(screen.getByLabelText('Fecha de inicio'), '2026-03-01')
    await user.click(screen.getByRole('button', { name: /^asignar$/i }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Este conductor ya tiene un vehículo asignado.')
    expect(onAssigned).not.toHaveBeenCalled()
  })

  it('shows the currently assigned driver for a vehicle on mount', async () => {
    renderWithClient(
      <VehicleAssignmentPanel
        vehicleId={FIRST_ASSIGNMENT!.vehicleId}
        vehicleLabel="Vehicle"
        canManage
      />,
    )

    expect(await screen.findByText(`Conductor asignado: ${FIRST_ASSIGNMENT!.driverName}`)).toBeInTheDocument()
  })
})
