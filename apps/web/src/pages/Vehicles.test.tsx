import { render, screen } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { beforeEach, describe, expect, it } from 'vitest'
import { useAuthStore } from '@fleetmgm/store'
import { resetVehiclesMock, SEED_VEHICLES } from '@/mocks/handlers'
import { Vehicles } from './Vehicles'

function renderVehicles() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  })

  return render(
    <QueryClientProvider client={queryClient}>
      <Vehicles />
    </QueryClientProvider>,
  )
}

const [FIRST_VEHICLE, SECOND_VEHICLE] = SEED_VEHICLES

function loginAs(role: 'ADMIN' | 'DRIVER') {
  useAuthStore.getState().login({
    email: 'user@fleetmgm.com',
    role,
    accessToken: 'token',
    refreshToken: 'refresh',
  })
}

describe('Vehicles', () => {
  beforeEach(() => {
    resetVehiclesMock()
    useAuthStore.getState().logout()
  })

  it('renders the paginated vehicle list', async () => {
    loginAs('ADMIN')
    renderVehicles()

    for (const vehicle of SEED_VEHICLES) {
      expect(await screen.findByText(`${vehicle.make} ${vehicle.model}`)).toBeInTheDocument()
    }
  })

  it('shows the correct status badge per vehicle status', async () => {
    loginAs('ADMIN')
    renderVehicles()

    await screen.findByText(`${FIRST_VEHICLE!.make} ${FIRST_VEHICLE!.model}`)

    expect(await screen.findByText('Active')).toBeInTheDocument()
    expect(await screen.findByText('Maintenance')).toBeInTheDocument()
  })

  it('shows only the assigned vehicle for the DRIVER role', async () => {
    loginAs('DRIVER')
    renderVehicles()

    expect(await screen.findByText(`${FIRST_VEHICLE!.make} ${FIRST_VEHICLE!.model}`)).toBeInTheDocument()
    expect(screen.queryByText(`${SECOND_VEHICLE!.make} ${SECOND_VEHICLE!.model}`)).not.toBeInTheDocument()
  })

  it('hides management actions for the DRIVER role', async () => {
    loginAs('DRIVER')
    renderVehicles()

    await screen.findByText(`${FIRST_VEHICLE!.make} ${FIRST_VEHICLE!.model}`)

    expect(screen.queryByRole('button', { name: /new vehicle/i })).not.toBeInTheDocument()
    expect(screen.queryByLabelText('Edit vehicle')).not.toBeInTheDocument()
    expect(screen.queryByLabelText('Delete vehicle')).not.toBeInTheDocument()
  })
})
