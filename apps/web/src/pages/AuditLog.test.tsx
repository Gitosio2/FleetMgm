import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import { useAuthStore } from '@fleetmgm/store'
import { SEED_AUDIT_LOGS } from '@/mocks/handlers'
import { AuditLog } from './AuditLog'

function renderAuditLog() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  })

  return render(
    <QueryClientProvider client={queryClient}>
      <AuditLog />
    </QueryClientProvider>,
  )
}

function loginAs(role: 'ADMIN' | 'MANAGER') {
  useAuthStore.getState().login({
    email: 'admin@fleetmgm.com',
    role,
    accessToken: 'token',
    refreshToken: 'refresh',
  })
}

describe('AuditLog', () => {
  beforeEach(() => {
    loginAs('ADMIN')
  })

  afterEach(() => {
    useAuthStore.getState().logout()
  })

  it('renders the audit log list', async () => {
    renderAuditLog()

    const [first, second, third] = SEED_AUDIT_LOGS

    const row1 = (await screen.findByText(first!.performedByEmail!)).closest('tr')!
    expect(within(row1).getByText('Factura')).toBeInTheDocument()
    expect(within(row1).getByText('Creación')).toBeInTheDocument()

    const row2 = screen.getByText(second!.performedByEmail!).closest('tr')!
    expect(within(row2).getByText('Factura de proveedor')).toBeInTheDocument()
    expect(within(row2).getByText('Actualización')).toBeInTheDocument()

    const row3 = screen.getAllByText(third!.performedByEmail!)[0]!.closest('tr')!
    expect(within(row3).getByText('Usuario')).toBeInTheDocument()
    expect(within(row3).getByText('Inicio de sesión')).toBeInTheDocument()
  })

  it('narrows the list with the entity type filter', async () => {
    const user = userEvent.setup()
    renderAuditLog()

    const [first, second] = SEED_AUDIT_LOGS
    await screen.findByText(first!.performedByEmail!)

    await user.selectOptions(screen.getByLabelText(/tipo de entidad/i), second!.entityType)

    await waitFor(() => {
      expect(screen.getByText(second!.performedByEmail!)).toBeInTheDocument()
    })
    expect(screen.queryByText(first!.performedByEmail!)).not.toBeInTheDocument()
  })

  it('narrows the list with the action filter', async () => {
    const user = userEvent.setup()
    renderAuditLog()

    const [first] = SEED_AUDIT_LOGS
    await screen.findByText(first!.performedByEmail!)

    await user.selectOptions(screen.getByLabelText(/acción/i), 'ACCESS_DENIED')

    const table = screen.getByRole('table')
    await waitFor(() => {
      expect(within(table).getByText('Acceso denegado')).toBeInTheDocument()
    })
    expect(within(table).queryByText('Creación')).not.toBeInTheDocument()
  })

  it('narrows the list with the date range filter', async () => {
    const user = userEvent.setup()
    renderAuditLog()

    const [first] = SEED_AUDIT_LOGS
    await screen.findByText(first!.performedByEmail!)

    await user.type(screen.getByLabelText(/desde/i), '2026-07-12')

    await waitFor(() => {
      expect(screen.queryByText(first!.performedByEmail!)).not.toBeInTheDocument()
    })
  })
})
