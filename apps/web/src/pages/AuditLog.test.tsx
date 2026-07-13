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

// The performer email now also appears as an <option> in the "Filtrar por usuario" select (see
// AuditLogFilters), so text lookups must be scoped to the table — otherwise getByText/findByText
// match both the table cell and the select option. The table also unmounts while a filter change
// is loading (isLoading resets per distinct query key), so it must be re-queried fresh after each
// interaction rather than reusing an element captured beforehand.
function table() {
  return screen.getByRole('table')
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
    await screen.findByRole('table')

    const row1 = within(table()).getByText(first!.performedByEmail!).closest('tr')!
    expect(within(row1).getByText('Factura')).toBeInTheDocument()
    expect(within(row1).getByText('Creación')).toBeInTheDocument()

    const row2 = within(table()).getByText(second!.performedByEmail!).closest('tr')!
    expect(within(row2).getByText('Factura de proveedor')).toBeInTheDocument()
    expect(within(row2).getByText('Actualización')).toBeInTheDocument()

    const row3 = within(table()).getAllByText(third!.performedByEmail!)[0]!.closest('tr')!
    expect(within(row3).getByText('Usuario')).toBeInTheDocument()
    expect(within(row3).getByText('Inicio de sesión')).toBeInTheDocument()
  })

  it('narrows the list with the entity type filter', async () => {
    const user = userEvent.setup()
    renderAuditLog()

    const [first, second] = SEED_AUDIT_LOGS
    await screen.findByRole('table')
    within(table()).getByText(first!.performedByEmail!)

    await user.selectOptions(screen.getByLabelText(/tipo de entidad/i), second!.entityType)

    await waitFor(() => {
      expect(within(table()).getByText(second!.performedByEmail!)).toBeInTheDocument()
    })
    expect(within(table()).queryByText(first!.performedByEmail!)).not.toBeInTheDocument()
  })

  it('narrows the list with the action filter', async () => {
    const user = userEvent.setup()
    renderAuditLog()

    const [first] = SEED_AUDIT_LOGS
    await screen.findByRole('table')
    within(table()).getByText(first!.performedByEmail!)

    await user.selectOptions(screen.getByLabelText(/acción/i), 'ACCESS_DENIED')

    await waitFor(() => {
      expect(within(table()).getByText('Acceso denegado')).toBeInTheDocument()
    })
    expect(within(table()).queryByText('Creación')).not.toBeInTheDocument()
  })

  it('narrows the list with the date range filter', async () => {
    const user = userEvent.setup()
    renderAuditLog()

    const [first] = SEED_AUDIT_LOGS
    await screen.findByRole('table')
    within(table()).getByText(first!.performedByEmail!)

    await user.type(screen.getByLabelText(/desde/i), '2026-07-12')

    await waitFor(() => {
      expect(within(table()).queryByText(first!.performedByEmail!)).not.toBeInTheDocument()
    })
  })

  it('narrows the list with the user filter', async () => {
    const user = userEvent.setup()
    renderAuditLog()

    const [first, second] = SEED_AUDIT_LOGS
    await screen.findByRole('table')
    within(table()).getByText(first!.performedByEmail!)

    await user.selectOptions(screen.getByLabelText(/filtrar por usuario/i), second!.performedByEmail!)

    await waitFor(() => {
      expect(within(table()).getByText(second!.performedByEmail!)).toBeInTheDocument()
    })
    expect(within(table()).queryByText(first!.performedByEmail!)).not.toBeInTheDocument()
  })
})
