import type { ReactNode } from 'react'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { resetVehiclesMock, SEED_JOBS } from '@/mocks/handlers'
import { JobUsageValueModal } from './JobUsageValueModal'

function renderWithClient(ui: ReactNode) {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  })

  return render(<QueryClientProvider client={queryClient}>{ui}</QueryClientProvider>)
}

const KM_JOB = SEED_JOBS.find((job) => job.id === 'job-1')! // vehicle-1, usageMeasure KILOMETERS
const HOURS_JOB = SEED_JOBS.find((job) => job.id === 'job-4')! // vehicle-2, usageMeasure HOURS

describe('JobUsageValueModal', () => {
  beforeEach(() => {
    resetVehiclesMock()
  })

  it('shows the "Iniciar trabajo" title and "Iniciar" confirm label in start mode', async () => {
    renderWithClient(
      <JobUsageValueModal
        open
        onOpenChange={() => {}}
        job={KM_JOB}
        mode="start"
        onConfirm={() => {}}
        isPending={false}
      />,
    )

    expect(await screen.findByRole('heading', { name: 'Iniciar trabajo' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /^iniciar$/i })).toBeInTheDocument()
  })

  it('shows the "Completar trabajo" title and "Completar" confirm label in complete mode', async () => {
    renderWithClient(
      <JobUsageValueModal
        open
        onOpenChange={() => {}}
        job={KM_JOB}
        mode="complete"
        onConfirm={() => {}}
        isPending={false}
      />,
    )

    expect(await screen.findByRole('heading', { name: 'Completar trabajo' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /^completar$/i })).toBeInTheDocument()
  })

  it('labels the field "Kilómetros actuales" for a vehicle measured in kilometers', async () => {
    renderWithClient(
      <JobUsageValueModal
        open
        onOpenChange={() => {}}
        job={KM_JOB}
        mode="start"
        onConfirm={() => {}}
        isPending={false}
      />,
    )

    expect(await screen.findByLabelText('Kilómetros actuales')).toBeInTheDocument()
  })

  it('labels the field "Horas actuales" for a vehicle measured in hours', async () => {
    renderWithClient(
      <JobUsageValueModal
        open
        onOpenChange={() => {}}
        job={HOURS_JOB}
        mode="start"
        onConfirm={() => {}}
        isPending={false}
      />,
    )

    expect(await screen.findByLabelText('Horas actuales')).toBeInTheDocument()
  })

  it('calls onOpenChange(false) and not onConfirm when Cancelar is clicked', async () => {
    const user = userEvent.setup()
    const onOpenChange = vi.fn()
    const onConfirm = vi.fn()

    renderWithClient(
      <JobUsageValueModal
        open
        onOpenChange={onOpenChange}
        job={KM_JOB}
        mode="start"
        onConfirm={onConfirm}
        isPending={false}
      />,
    )

    await user.click(await screen.findByRole('button', { name: /cancelar/i }))

    expect(onOpenChange).toHaveBeenCalledWith(false)
    expect(onConfirm).not.toHaveBeenCalled()
  })

  it('calls onConfirm(null) when confirmed with the field left empty', async () => {
    const user = userEvent.setup()
    const onConfirm = vi.fn()

    renderWithClient(
      <JobUsageValueModal
        open
        onOpenChange={() => {}}
        job={KM_JOB}
        mode="start"
        onConfirm={onConfirm}
        isPending={false}
      />,
    )

    await user.click(await screen.findByRole('button', { name: /^iniciar$/i }))

    expect(onConfirm).toHaveBeenCalledWith(null)
  })

  it('calls onConfirm with the typed number when a value is entered', async () => {
    const user = userEvent.setup()
    const onConfirm = vi.fn()

    renderWithClient(
      <JobUsageValueModal
        open
        onOpenChange={() => {}}
        job={KM_JOB}
        mode="start"
        onConfirm={onConfirm}
        isPending={false}
      />,
    )

    await user.type(await screen.findByLabelText('Kilómetros actuales'), '15200')
    await user.click(screen.getByRole('button', { name: /^iniciar$/i }))

    expect(onConfirm).toHaveBeenCalledWith(15200)
  })

  it('disables the confirm button while isPending is true', async () => {
    renderWithClient(
      <JobUsageValueModal
        open
        onOpenChange={() => {}}
        job={KM_JOB}
        mode="start"
        onConfirm={() => {}}
        isPending
      />,
    )

    expect(await screen.findByRole('button', { name: /^iniciar$/i })).toBeDisabled()
  })

  it('blocks submission and shows an error when completing with a value below the vehicle\'s current usage', async () => {
    const user = userEvent.setup()
    const onConfirm = vi.fn()
    const onOpenChange = vi.fn()

    // job-2 (Reparto semanal) uses vehicle-1, currentKm 15000, startUsageValue 14500 — floor is 15000.
    const IN_PROGRESS_JOB = SEED_JOBS.find((job) => job.id === 'job-2')!

    renderWithClient(
      <JobUsageValueModal
        open
        onOpenChange={onOpenChange}
        job={IN_PROGRESS_JOB}
        mode="complete"
        onConfirm={onConfirm}
        isPending={false}
      />,
    )

    await user.type(await screen.findByLabelText('Kilómetros actuales'), '10000')
    await user.click(screen.getByRole('button', { name: /^completar$/i }))

    expect(await screen.findByRole('alert')).toHaveTextContent(
      /el valor ingresado es menor al que ya tiene registrado el vehículo/i,
    )
    expect(onConfirm).not.toHaveBeenCalled()
    expect(onOpenChange).not.toHaveBeenCalled()
  })

  it('allows submission when the value equals or exceeds the current usage', async () => {
    const user = userEvent.setup()
    const onConfirm = vi.fn()

    const IN_PROGRESS_JOB = SEED_JOBS.find((job) => job.id === 'job-2')!

    renderWithClient(
      <JobUsageValueModal
        open
        onOpenChange={() => {}}
        job={IN_PROGRESS_JOB}
        mode="complete"
        onConfirm={onConfirm}
        isPending={false}
      />,
    )

    await user.type(await screen.findByLabelText('Kilómetros actuales'), '15300')
    await user.click(screen.getByRole('button', { name: /^completar$/i }))

    expect(onConfirm).toHaveBeenCalledWith(15300)
  })

  it('does not apply the below-current check in start mode', async () => {
    const user = userEvent.setup()
    const onConfirm = vi.fn()

    renderWithClient(
      <JobUsageValueModal
        open
        onOpenChange={() => {}}
        job={KM_JOB}
        mode="start"
        onConfirm={onConfirm}
        isPending={false}
      />,
    )

    await user.type(await screen.findByLabelText('Kilómetros actuales'), '1')
    await user.click(screen.getByRole('button', { name: /^iniciar$/i }))

    expect(onConfirm).toHaveBeenCalledWith(1)
  })

  it('shows the backend error message when the error prop is set', async () => {
    const axiosLikeError = {
      isAxiosError: true,
      response: {
        data: {
          status: 409,
          code: 'JOB_USAGE_VALUE_BELOW_CURRENT',
          message: 'endUsageValue 100 for job job-2 is lower than the current recorded usage (15000)',
          correlationId: 'test-correlation-id',
        },
      },
    }

    renderWithClient(
      <JobUsageValueModal
        open
        onOpenChange={() => {}}
        job={KM_JOB}
        mode="complete"
        onConfirm={() => {}}
        isPending={false}
        error={axiosLikeError}
      />,
    )

    expect(await screen.findByRole('alert')).toHaveTextContent(
      /el valor ingresado es menor al que ya tiene registrado el vehículo/i,
    )
  })
})
