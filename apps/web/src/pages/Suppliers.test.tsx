import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { beforeEach, describe, expect, it } from 'vitest'
import { useAuthStore } from '@fleetmgm/store'
import { resetSuppliersMock, SEED_SUPPLIERS } from '@/mocks/handlers'
import { Suppliers } from './Suppliers'

function renderSuppliers() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  })

  return render(
    <QueryClientProvider client={queryClient}>
      <Suppliers />
    </QueryClientProvider>,
  )
}

const FIRST_SUPPLIER = SEED_SUPPLIERS[0]!

function loginAs(role: 'ADMIN' | 'DRIVER') {
  useAuthStore.getState().login({
    email: 'user@fleetmgm.com',
    role,
    accessToken: 'token',
    refreshToken: 'refresh',
  })
}

describe('Suppliers', () => {
  beforeEach(() => {
    resetSuppliersMock()
    useAuthStore.getState().logout()
  })

  it('renders the paginated supplier list', async () => {
    loginAs('ADMIN')
    renderSuppliers()

    for (const supplier of SEED_SUPPLIERS) {
      expect(await screen.findByText(supplier.name)).toBeInTheDocument()
    }
  })

  it('creating a supplier calls POST and adds it to the list', async () => {
    loginAs('ADMIN')
    const user = userEvent.setup()
    renderSuppliers()

    await screen.findByText(FIRST_SUPPLIER.name)

    await user.click(screen.getByRole('button', { name: /nuevo proveedor/i }))
    await user.type(screen.getByLabelText(/^nombre$/i), 'Recambios del Este')
    await user.click(screen.getByRole('button', { name: /crear proveedor/i }))

    expect(await screen.findByText('Recambios del Este')).toBeInTheDocument()
  })

  it('creating a supplier without a NIF succeeds', async () => {
    loginAs('ADMIN')
    const user = userEvent.setup()
    renderSuppliers()

    await screen.findByText(FIRST_SUPPLIER.name)

    await user.click(screen.getByRole('button', { name: /nuevo proveedor/i }))
    await user.type(screen.getByLabelText(/^nombre$/i), 'Proveedor Sin NIF')
    await user.click(screen.getByRole('button', { name: /crear proveedor/i }))

    expect(await screen.findByText('Proveedor Sin NIF')).toBeInTheDocument()
  })

  it('editing a supplier calls PUT and updates the row', async () => {
    loginAs('ADMIN')
    const user = userEvent.setup()
    renderSuppliers()

    await screen.findByText(FIRST_SUPPLIER.name)

    await user.click(screen.getAllByLabelText('Editar proveedor')[0]!)
    const nameInput = screen.getByLabelText(/^nombre$/i)
    await user.clear(nameInput)
    await user.type(nameInput, 'Taller Mecánico Norte Renombrado')
    await user.click(screen.getByRole('button', { name: /guardar cambios/i }))

    expect(await screen.findByText('Taller Mecánico Norte Renombrado')).toBeInTheDocument()
    expect(screen.queryByText(FIRST_SUPPLIER.name)).not.toBeInTheDocument()
  })

  it('deleting a supplier asks for confirmation before calling DELETE', async () => {
    loginAs('ADMIN')
    const user = userEvent.setup()
    renderSuppliers()

    await screen.findByText(FIRST_SUPPLIER.name)

    await user.click(screen.getAllByLabelText('Eliminar proveedor')[0]!)
    expect(
      screen.getByText(new RegExp(`¿Eliminar a ${FIRST_SUPPLIER.name}\\?`)),
    ).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'Eliminar' }))

    await waitFor(() =>
      expect(screen.queryByText(FIRST_SUPPLIER.name)).not.toBeInTheDocument(),
    )
  })

  it('hides management actions for the DRIVER role', async () => {
    loginAs('DRIVER')
    renderSuppliers()

    await screen.findByText(FIRST_SUPPLIER.name)

    expect(screen.queryByRole('button', { name: /nuevo proveedor/i })).not.toBeInTheDocument()
    expect(screen.queryByLabelText('Editar proveedor')).not.toBeInTheDocument()
    expect(screen.queryByLabelText('Eliminar proveedor')).not.toBeInTheDocument()
  })
})
