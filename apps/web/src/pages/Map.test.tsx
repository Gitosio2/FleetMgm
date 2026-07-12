import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { useAuthStore } from '@fleetmgm/store'
import { resetGpsMock, resetVehiclesMock, SEED_GPS_POSITIONS } from '@/mocks/handlers'
import { server } from '@/mocks/server'
import { Map } from './Map'

function renderMap() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  })

  return render(
    <QueryClientProvider client={queryClient}>
      <Map />
    </QueryClientProvider>,
  )
}

function loginAsAdmin() {
  useAuthStore.getState().login({
    email: 'admin@fleetmgm.com',
    role: 'ADMIN',
    accessToken: 'token',
    refreshToken: 'refresh',
  })
}

describe('Map', () => {
  beforeEach(() => {
    resetGpsMock()
    resetVehiclesMock()
    useAuthStore.getState().logout()
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('renders a marker for each vehicle position returned by the API', async () => {
    loginAsAdmin()
    renderMap()

    for (const position of SEED_GPS_POSITIONS) {
      expect(await screen.findByTestId(`vehicle-marker-${position.vehicleId}`)).toBeInTheDocument()
    }
  })

  it('shows license plate, make/model, and speed in the popover when a marker is clicked', async () => {
    loginAsAdmin()
    const user = userEvent.setup()
    renderMap()

    const firstPosition = SEED_GPS_POSITIONS[0]!
    const marker = await screen.findByTestId(`vehicle-marker-${firstPosition.vehicleId}`)
    await user.click(marker)

    // Scoped to the popover — the vehicle filter <select> can render the same license plate
    // text as one of its <option> labels, which would otherwise make screen.findByText ambiguous.
    const popover = within(await screen.findByTestId('vehicle-popover'))
    expect(popover.getByText(firstPosition.licensePlate!)).toBeInTheDocument()
    expect(popover.getByText(`${firstPosition.vehicleMake} ${firstPosition.vehicleModel}`)).toBeInTheDocument()
    expect(popover.getByText(`${firstPosition.speed} km/h`)).toBeInTheDocument()
  })

  it('shows "make model" in the popover for a vehicle without a license plate', async () => {
    loginAsAdmin()
    const user = userEvent.setup()
    renderMap()

    const machineryPosition = SEED_GPS_POSITIONS.find((position) => position.licensePlate === null)!
    const marker = await screen.findByTestId(`vehicle-marker-${machineryPosition.vehicleId}`)
    await user.click(marker)

    const popover = within(await screen.findByTestId('vehicle-popover'))
    expect(
      popover.getByText(`${machineryPosition.vehicleMake} ${machineryPosition.vehicleModel}`),
    ).toBeInTheDocument()
  })

  it('filters markers by vehicle category', async () => {
    loginAsAdmin()
    const user = userEvent.setup()
    renderMap()

    const lightVehiclePosition = SEED_GPS_POSITIONS[0]!
    const machineryPosition = SEED_GPS_POSITIONS[1]!
    await screen.findByTestId(`vehicle-marker-${lightVehiclePosition.vehicleId}`)

    const categorySelect = screen.getByLabelText(/filtrar por tipo de vehículo/i)
    await user.selectOptions(categorySelect, 'HEAVY_MACHINERY')

    expect(await screen.findByTestId(`vehicle-marker-${machineryPosition.vehicleId}`)).toBeInTheDocument()
    await waitFor(() => {
      expect(screen.queryByTestId(`vehicle-marker-${lightVehiclePosition.vehicleId}`)).not.toBeInTheDocument()
    })
  })

  it('filters markers by a specific vehicle', async () => {
    loginAsAdmin()
    const user = userEvent.setup()
    renderMap()

    const targetPosition = SEED_GPS_POSITIONS[0]!
    const otherPosition = SEED_GPS_POSITIONS[1]!
    await screen.findByTestId(`vehicle-marker-${otherPosition.vehicleId}`)

    const vehicleSelect = screen.getByLabelText(/filtrar por vehículo/i)
    await waitFor(() => expect(within(vehicleSelect).getAllByRole('option').length).toBeGreaterThan(1))
    await user.selectOptions(vehicleSelect, targetPosition.vehicleId)

    expect(await screen.findByTestId(`vehicle-marker-${targetPosition.vehicleId}`)).toBeInTheDocument()
    await waitFor(() => {
      expect(screen.queryByTestId(`vehicle-marker-${otherPosition.vehicleId}`)).not.toBeInTheDocument()
    })
  })

  it('polls /api/v1/gps/latest every 10 seconds', async () => {
    let requestCount = 0
    server.events.on('request:start', ({ request }) => {
      if (new URL(request.url).pathname === '/api/v1/gps/latest') {
        requestCount++
      }
    })
    loginAsAdmin()
    vi.useFakeTimers({ shouldAdvanceTime: true })

    renderMap()

    await vi.waitFor(() => expect(requestCount).toBe(1))

    await vi.advanceTimersByTimeAsync(10_000)

    await vi.waitFor(() => expect(requestCount).toBe(2))
  })
})
