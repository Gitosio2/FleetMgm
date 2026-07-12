import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import { useAuthStore } from '@fleetmgm/store'
import {
  resetSupplierInvoicesMock,
  resetSuppliersMock,
  resetVehiclesMock,
  SEED_SUPPLIER_INVOICES,
  SEED_SUPPLIERS,
} from '@/mocks/handlers'
import { SupplierInvoices } from './SupplierInvoices'

function renderSupplierInvoices() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  })

  return render(
    <QueryClientProvider client={queryClient}>
      <SupplierInvoices />
    </QueryClientProvider>,
  )
}

function loginAs(role: 'ADMIN') {
  useAuthStore.getState().login({
    email: 'admin@fleetmgm.com',
    role,
    accessToken: 'token',
    refreshToken: 'refresh',
  })
}

describe('SupplierInvoices', () => {
  beforeEach(() => {
    resetSupplierInvoicesMock()
    resetSuppliersMock()
    resetVehiclesMock()
    loginAs('ADMIN')
  })

  afterEach(() => {
    useAuthStore.getState().logout()
  })

  it('renders the supplier invoice list', async () => {
    renderSupplierInvoices()

    const [withVehicle, withoutVehicle, paid] = SEED_SUPPLIER_INVOICES

    const row1 = (await screen.findByText(withVehicle!.supplierName)).closest('tr')!
    expect(within(row1).getByText('Pendiente')).toBeInTheDocument()
    expect(within(row1).getByText(withVehicle!.vehicleLicensePlate!)).toBeInTheDocument()

    const row2 = screen.getByText(withoutVehicle!.supplierName).closest('tr')!
    expect(within(row2).getByText('Pendiente')).toBeInTheDocument()
    expect(within(row2).getByText('—')).toBeInTheDocument()

    const row3 = screen.getByText(paid!.supplierName).closest('tr')!
    expect(within(row3).getByText('Pagada')).toBeInTheDocument()
  })

  it('creates a new PENDING supplier invoice without a vehicle', async () => {
    const user = userEvent.setup()
    renderSupplierInvoices()

    await screen.findByText(SEED_SUPPLIER_INVOICES[0]!.supplierName)

    const newSupplier = SEED_SUPPLIERS.find((supplier) => supplier.name === 'Ferretería Central')!

    await user.click(screen.getByRole('button', { name: /nueva factura de proveedor/i }))
    await user.selectOptions(screen.getByLabelText(/^proveedor$/i), newSupplier.id)
    await user.selectOptions(screen.getByLabelText(/^categoría$/i), 'OTHER')
    await user.type(screen.getByLabelText(/fecha de factura/i), '2026-07-10')
    await user.type(screen.getByLabelText(/^subtotal$/i), '40')
    await user.type(screen.getByLabelText(/^iva$/i), '8.4')
    await user.type(screen.getByLabelText(/^total$/i), '48.4')
    await user.click(screen.getByRole('button', { name: /crear factura/i }))

    await waitFor(() => {
      expect(screen.getByText('Ferretería Central')).toBeInTheDocument()
    })
    const row = screen.getByText('Ferretería Central').closest('tr')!
    expect(within(row).getByText('Pendiente')).toBeInTheDocument()
    // Both the "Vehículo" and "Vencimiento" columns fall back to the placeholder here, since
    // neither a vehicle nor a due date was entered in this test.
    expect(within(row).getAllByText('—')).toHaveLength(2)
    expect(within(row).getByText('48.40')).toBeInTheDocument()
  })

  it('creates a new PENDING supplier invoice with a vehicle selected', async () => {
    const user = userEvent.setup()
    renderSupplierInvoices()

    await screen.findByText(SEED_SUPPLIER_INVOICES[0]!.supplierName)

    const newSupplier = SEED_SUPPLIERS.find((supplier) => supplier.name === 'Taller Rápido')!

    await user.click(screen.getByRole('button', { name: /nueva factura de proveedor/i }))
    await user.selectOptions(screen.getByLabelText(/^proveedor$/i), newSupplier.id)
    await user.selectOptions(screen.getByLabelText(/^categoría$/i), 'MAINTENANCE')
    await user.selectOptions(screen.getByLabelText(/^vehículo$/i), 'vehicle-1')
    await user.type(screen.getByLabelText(/fecha de factura/i), '2026-07-10')
    await user.type(screen.getByLabelText(/^subtotal$/i), '100')
    await user.type(screen.getByLabelText(/^iva$/i), '21')
    await user.type(screen.getByLabelText(/^total$/i), '121')
    await user.click(screen.getByRole('button', { name: /crear factura/i }))

    await waitFor(() => {
      expect(screen.getByText('Taller Rápido')).toBeInTheDocument()
    })
    const row = screen.getByText('Taller Rápido').closest('tr')!
    expect(within(row).getByText('1234ABC')).toBeInTheDocument()
  })

  it('edits a PENDING supplier invoice', async () => {
    const user = userEvent.setup()
    renderSupplierInvoices()

    const withVehicle = SEED_SUPPLIER_INVOICES[0]!
    const row = (await screen.findByText(withVehicle.supplierName)).closest('tr')!
    await user.click(within(row).getByRole('button', { name: /editar/i }))

    expect(await screen.findByRole('heading', { name: 'Editar factura de proveedor' })).toBeInTheDocument()
    expect(screen.getByLabelText(/^proveedor$/i)).toHaveValue(withVehicle.supplierId)

    await user.clear(screen.getByLabelText(/^total$/i))
    await user.type(screen.getByLabelText(/^total$/i), '999')
    await user.click(screen.getByRole('button', { name: /guardar cambios/i }))

    await waitFor(() => {
      const updatedRow = screen.getByText(withVehicle.supplierName).closest('tr')!
      expect(within(updatedRow).getByText('999.00')).toBeInTheDocument()
    })
  })

  it('marks a PENDING supplier invoice as paid, transitioning it to PAID', async () => {
    const user = userEvent.setup()
    renderSupplierInvoices()

    const withoutVehicle = SEED_SUPPLIER_INVOICES[1]!
    const row = (await screen.findByText(withoutVehicle.supplierName)).closest('tr')!
    expect(within(row).getByText('Pendiente')).toBeInTheDocument()

    await user.click(within(row).getByRole('button', { name: /marcar pagada/i }))

    await waitFor(() => expect(within(row).getByText('Pagada')).toBeInTheDocument())
    expect(within(row).queryByRole('button', { name: /marcar pagada/i })).not.toBeInTheDocument()
  })

  it('hides the "Marcar pagada" button for an already-PAID supplier invoice', async () => {
    renderSupplierInvoices()

    const paid = SEED_SUPPLIER_INVOICES[2]!
    const row = (await screen.findByText(paid.supplierName)).closest('tr')!

    expect(within(row).queryByRole('button', { name: /marcar pagada/i })).not.toBeInTheDocument()
  })

  it('narrows the supplier invoice list with the category filter', async () => {
    const user = userEvent.setup()
    renderSupplierInvoices()

    const [withVehicle, withoutVehicle, paid] = SEED_SUPPLIER_INVOICES
    await screen.findByText(withVehicle!.supplierName)

    await user.selectOptions(screen.getByLabelText(/filtrar por categoría/i), withoutVehicle!.category)

    await waitFor(() => {
      expect(screen.getByText(withoutVehicle!.supplierName)).toBeInTheDocument()
    })
    expect(screen.queryByText(withVehicle!.supplierName)).not.toBeInTheDocument()
    expect(screen.queryByText(paid!.supplierName)).not.toBeInTheDocument()
  })
})
