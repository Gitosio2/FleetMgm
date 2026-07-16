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
import { formatCurrency } from '@/lib/currency'
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

    const row1 = (await screen.findByRole('button', { name: withVehicle!.supplierName })).closest('tr')!
    expect(within(row1).getByText('Pendiente')).toBeInTheDocument()
    expect(within(row1).getByText(withVehicle!.vehicleLicensePlate!)).toBeInTheDocument()

    const row2 = screen.getByRole('button', { name: withoutVehicle!.supplierName }).closest('tr')!
    expect(within(row2).getByText('Pendiente')).toBeInTheDocument()
    expect(within(row2).getByText('—')).toBeInTheDocument()

    const row3 = screen.getByRole('button', { name: paid!.supplierName }).closest('tr')!
    expect(within(row3).getByText('Pagada')).toBeInTheDocument()
  })

  it('creates a new PENDING supplier invoice without a vehicle', async () => {
    const user = userEvent.setup()
    renderSupplierInvoices()

    await screen.findByRole('button', { name: SEED_SUPPLIER_INVOICES[0]!.supplierName })

    const newSupplier = SEED_SUPPLIERS.find((supplier) => supplier.name === 'Ferretería Central')!

    await user.click(screen.getByRole('button', { name: /nueva factura de proveedor/i }))
    await user.selectOptions(screen.getByLabelText(/^proveedor$/i), newSupplier.id)
    await user.selectOptions(screen.getByLabelText(/^categoría$/i), 'OTHER')
    await user.type(screen.getByLabelText(/fecha de factura/i), '2026-07-10')
    await user.type(screen.getByLabelText(/^subtotal$/i), '40')
    await user.type(screen.getByLabelText(/^iva$/i), '8.4')
    // Total auto-calculates from Subtotal + IVA — verify it before submitting instead of retyping it.
    expect(screen.getByLabelText(/^total$/i)).toHaveValue(48.4)
    await user.click(screen.getByRole('button', { name: /crear factura/i }))

    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Ferretería Central' })).toBeInTheDocument()
    })
    const row = screen.getByRole('button', { name: 'Ferretería Central' }).closest('tr')!
    expect(within(row).getByText('Pendiente')).toBeInTheDocument()
    // Both the "Vehículo" and "Vencimiento" columns fall back to the placeholder here, since
    // neither a vehicle nor a due date was entered in this test.
    expect(within(row).getAllByText('—')).toHaveLength(2)
    expect(within(row).getByText(formatCurrency(48.4))).toBeInTheDocument()
  })

  it('creates a new PENDING supplier invoice with a vehicle selected', async () => {
    const user = userEvent.setup()
    renderSupplierInvoices()

    await screen.findByRole('button', { name: SEED_SUPPLIER_INVOICES[0]!.supplierName })

    const newSupplier = SEED_SUPPLIERS.find((supplier) => supplier.name === 'Taller Rápido')!

    await user.click(screen.getByRole('button', { name: /nueva factura de proveedor/i }))
    await user.selectOptions(screen.getByLabelText(/^proveedor$/i), newSupplier.id)
    await user.selectOptions(screen.getByLabelText(/^categoría$/i), 'MAINTENANCE')
    await user.selectOptions(screen.getByLabelText(/^vehículo$/i), 'vehicle-1')
    await user.type(screen.getByLabelText(/fecha de factura/i), '2026-07-10')
    await user.type(screen.getByLabelText(/^subtotal$/i), '100')
    await user.type(screen.getByLabelText(/^iva$/i), '21')
    expect(screen.getByLabelText(/^total$/i)).toHaveValue(121)
    await user.click(screen.getByRole('button', { name: /crear factura/i }))

    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Taller Rápido' })).toBeInTheDocument()
    })
    const row = screen.getByRole('button', { name: 'Taller Rápido' }).closest('tr')!
    expect(within(row).getByText('1234ABC')).toBeInTheDocument()
  })

  it('edits a PENDING supplier invoice', async () => {
    const user = userEvent.setup()
    renderSupplierInvoices()

    const withVehicle = SEED_SUPPLIER_INVOICES[0]!
    const row = (await screen.findByRole('button', { name: withVehicle.supplierName })).closest('tr')!
    await user.click(within(row).getByRole('button', { name: /editar/i }))

    expect(await screen.findByRole('heading', { name: 'Editar factura de proveedor' })).toBeInTheDocument()
    expect(screen.getByLabelText(/^proveedor$/i)).toHaveValue(withVehicle.supplierId)

    await user.clear(screen.getByLabelText(/^total$/i))
    await user.type(screen.getByLabelText(/^total$/i), '999')
    await user.click(screen.getByRole('button', { name: /guardar cambios/i }))

    await waitFor(() => {
      const updatedRow = screen.getByRole('button', { name: withVehicle.supplierName }).closest('tr')!
      expect(within(updatedRow).getByText(formatCurrency(999))).toBeInTheDocument()
    })
  })

  it('marks a PENDING supplier invoice as paid, transitioning it to PAID', async () => {
    const user = userEvent.setup()
    renderSupplierInvoices()

    const withoutVehicle = SEED_SUPPLIER_INVOICES[1]!
    const row = (await screen.findByRole('button', { name: withoutVehicle.supplierName })).closest('tr')!
    expect(within(row).getByText('Pendiente')).toBeInTheDocument()

    await user.click(within(row).getByRole('button', { name: /marcar pagada/i }))

    await waitFor(() => expect(within(row).getByText('Pagada')).toBeInTheDocument())
    expect(within(row).queryByRole('button', { name: /marcar pagada/i })).not.toBeInTheDocument()
  })

  it('hides the "Marcar pagada" button for an already-PAID supplier invoice', async () => {
    renderSupplierInvoices()

    const paid = SEED_SUPPLIER_INVOICES[2]!
    const row = (await screen.findByRole('button', { name: paid.supplierName })).closest('tr')!

    expect(within(row).queryByRole('button', { name: /marcar pagada/i })).not.toBeInTheDocument()
  })

  it('opens a PAID supplier invoice as a read-only view', async () => {
    const user = userEvent.setup()
    renderSupplierInvoices()

    const paid = SEED_SUPPLIER_INVOICES[2]!
    const row = (await screen.findByRole('button', { name: paid.supplierName })).closest('tr')!
    expect(within(row).getByRole('button', { name: /^ver$/i })).toBeInTheDocument()

    await user.click(within(row).getByRole('button', { name: /^ver$/i }))

    expect(await screen.findByRole('heading', { name: 'Factura de proveedor' })).toBeInTheDocument()
    expect(screen.getByLabelText(/^proveedor$/i)).toBeDisabled()
    expect(screen.getByLabelText(/^subtotal$/i)).toBeDisabled()
    expect(screen.queryByRole('button', { name: /guardar cambios/i })).not.toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: /^cerrar$/i }))
    expect(screen.queryByRole('heading', { name: 'Factura de proveedor' })).not.toBeInTheDocument()
  })

  it('adds a line item to a PENDING supplier invoice without a header vehicle, updating the allocation indicator', async () => {
    const user = userEvent.setup()
    renderSupplierInvoices()

    const withoutVehicle = SEED_SUPPLIER_INVOICES[1]!
    const row = (await screen.findByRole('button', { name: withoutVehicle.supplierName })).closest('tr')!
    await user.click(within(row).getByRole('button', { name: /editar/i }))

    expect(await screen.findByRole('heading', { name: 'Editar factura de proveedor' })).toBeInTheDocument()
    expect(screen.getByTestId('line-item-allocation-summary')).toHaveTextContent(
      `Asignado: ${formatCurrency(0)} / ${formatCurrency(withoutVehicle.subtotal)}`,
    )

    await user.selectOptions(screen.getByLabelText(/vehículo de la línea/i), 'vehicle-1')
    await user.type(screen.getByLabelText(/^descripción$/i), 'Gasoil - Toyota Hilux')
    await user.clear(screen.getByLabelText(/^cantidad$/i))
    await user.type(screen.getByLabelText(/^cantidad$/i), '10')
    await user.type(screen.getByLabelText(/^coste total$/i), '15.00')
    await user.click(screen.getByRole('button', { name: /agregar línea/i }))

    await waitFor(() => {
      expect(screen.getByText('Gasoil - Toyota Hilux')).toBeInTheDocument()
    })
    expect(screen.getByTestId('line-item-allocation-summary')).toHaveTextContent(
      `Asignado: ${formatCurrency(15)} / ${formatCurrency(withoutVehicle.subtotal)}`,
    )
  })

  it('disables the header vehicle select once the supplier invoice already has line items', async () => {
    const user = userEvent.setup()
    renderSupplierInvoices()

    const alreadySplit = SEED_SUPPLIER_INVOICES[3]!
    const row = (await screen.findByRole('button', { name: alreadySplit.supplierName })).closest('tr')!
    await user.click(within(row).getByRole('button', { name: /editar/i }))

    expect(await screen.findByRole('heading', { name: 'Editar factura de proveedor' })).toBeInTheDocument()
    expect(screen.getByLabelText(/^vehículo$/i)).toBeDisabled()
    expect(screen.getByText('Esta factura ya tiene vehículos asignados por línea.')).toBeInTheDocument()
    expect(screen.getByText(alreadySplit.lineItems[0]!.description)).toBeInTheDocument()
    expect(screen.getByText(alreadySplit.lineItems[1]!.description)).toBeInTheDocument()
  })

  it('shows edit/delete controls for line items on a PENDING supplier invoice split by vehicle', async () => {
    const user = userEvent.setup()
    renderSupplierInvoices()

    const alreadySplit = SEED_SUPPLIER_INVOICES[3]!
    const row = (await screen.findByRole('button', { name: alreadySplit.supplierName })).closest('tr')!
    await user.click(within(row).getByRole('button', { name: /editar/i }))

    expect(await screen.findByRole('heading', { name: 'Editar factura de proveedor' })).toBeInTheDocument()

    const lineItemRow = screen.getByText(alreadySplit.lineItems[0]!.description).closest('tr')!
    expect(within(lineItemRow).getByRole('button', { name: /editar línea/i })).toBeInTheDocument()
    expect(within(lineItemRow).getByRole('button', { name: /eliminar línea/i })).toBeInTheDocument()
  })

  it('edits a line item on a PENDING supplier invoice split by vehicle, updating the allocation indicator', async () => {
    const user = userEvent.setup()
    renderSupplierInvoices()

    const alreadySplit = SEED_SUPPLIER_INVOICES[3]!
    const row = (await screen.findByRole('button', { name: alreadySplit.supplierName })).closest('tr')!
    await user.click(within(row).getByRole('button', { name: /editar/i }))

    expect(await screen.findByRole('heading', { name: 'Editar factura de proveedor' })).toBeInTheDocument()

    const lineItemRow = screen.getByText(alreadySplit.lineItems[0]!.description).closest('tr')!
    await user.click(within(lineItemRow).getByRole('button', { name: /editar línea/i }))

    await user.clear(screen.getByLabelText(/coste total a editar/i))
    await user.type(screen.getByLabelText(/coste total a editar/i), '45.00')
    await user.click(screen.getByRole('button', { name: /^guardar$/i }))

    await waitFor(() => {
      const updatedRow = screen.getByText(alreadySplit.lineItems[0]!.description).closest('tr')!
      expect(within(updatedRow).getByText(formatCurrency(45))).toBeInTheDocument()
    })
    // Line 1's total cost updates directly to 45.00; line 2 stays at 30.00 -> 75.00 total allocated.
    expect(screen.getByTestId('line-item-allocation-summary')).toHaveTextContent(
      `Asignado: ${formatCurrency(75)} / ${formatCurrency(alreadySplit.subtotal)}`,
    )
  })

  it('deletes a line item from a PENDING supplier invoice after confirming, updating the allocation indicator', async () => {
    const user = userEvent.setup()
    renderSupplierInvoices()

    const alreadySplit = SEED_SUPPLIER_INVOICES[3]!
    const row = (await screen.findByRole('button', { name: alreadySplit.supplierName })).closest('tr')!
    await user.click(within(row).getByRole('button', { name: /editar/i }))

    expect(await screen.findByRole('heading', { name: 'Editar factura de proveedor' })).toBeInTheDocument()

    const lineItemRow = screen.getByText(alreadySplit.lineItems[1]!.description).closest('tr')!
    await user.click(within(lineItemRow).getByRole('button', { name: /eliminar línea/i }))
    await user.click(await screen.findByRole('button', { name: /^eliminar$/i }))

    await waitFor(() => {
      expect(screen.queryByText(alreadySplit.lineItems[1]!.description)).not.toBeInTheDocument()
    })
    // Only line 1 remains, subtotal 60.00, out of a 90.00 invoice subtotal.
    expect(screen.getByTestId('line-item-allocation-summary')).toHaveTextContent(
      `Asignado: ${formatCurrency(60)} / ${formatCurrency(alreadySplit.subtotal)}`,
    )
  })

  it('narrows the supplier invoice list with the category filter', async () => {
    const user = userEvent.setup()
    renderSupplierInvoices()

    const [withVehicle, withoutVehicle, paid] = SEED_SUPPLIER_INVOICES
    await screen.findByRole('button', { name: withVehicle!.supplierName })

    await user.selectOptions(screen.getByLabelText(/filtrar por categoría/i), withoutVehicle!.category)

    await waitFor(() => {
      expect(screen.getByRole('button', { name: withoutVehicle!.supplierName })).toBeInTheDocument()
    })
    expect(screen.queryByRole('button', { name: withVehicle!.supplierName })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: paid!.supplierName })).not.toBeInTheDocument()
  })

  it('narrows the supplier invoice list with the supplier filter', async () => {
    const user = userEvent.setup()
    renderSupplierInvoices()

    const [withVehicle, , paid] = SEED_SUPPLIER_INVOICES
    await screen.findByRole('button', { name: withVehicle!.supplierName })

    await user.selectOptions(screen.getByLabelText(/filtrar por proveedor/i), paid!.supplierId)

    await waitFor(() => {
      expect(screen.getByRole('button', { name: paid!.supplierName })).toBeInTheDocument()
    })
    expect(screen.queryByRole('button', { name: withVehicle!.supplierName })).not.toBeInTheDocument()
  })

  it('narrows the supplier invoice list with the vehicle filter', async () => {
    const user = userEvent.setup()
    renderSupplierInvoices()

    const [withVehicle, withoutVehicle] = SEED_SUPPLIER_INVOICES
    await screen.findByRole('button', { name: withVehicle!.supplierName })

    await user.selectOptions(screen.getByLabelText(/filtrar por vehículo/i), withVehicle!.vehicleId!)

    await waitFor(() => {
      expect(screen.getByRole('button', { name: withVehicle!.supplierName })).toBeInTheDocument()
    })
    expect(screen.queryByRole('button', { name: withoutVehicle!.supplierName })).not.toBeInTheDocument()
  })

  it('narrows the supplier invoice list with the status filter', async () => {
    const user = userEvent.setup()
    renderSupplierInvoices()

    const [withVehicle, , paid] = SEED_SUPPLIER_INVOICES
    await screen.findByRole('button', { name: withVehicle!.supplierName })

    await user.selectOptions(screen.getByLabelText(/filtrar por estado/i), 'PAID')

    await waitFor(() => {
      expect(screen.getByRole('button', { name: paid!.supplierName })).toBeInTheDocument()
    })
    expect(screen.queryByRole('button', { name: withVehicle!.supplierName })).not.toBeInTheDocument()
  })

  it('narrows the supplier invoice list with the invoice date range filter', async () => {
    const user = userEvent.setup()
    renderSupplierInvoices()

    const [withVehicle, , paid] = SEED_SUPPLIER_INVOICES
    await screen.findByRole('button', { name: withVehicle!.supplierName })

    await user.type(screen.getByLabelText(/^fecha desde$/i), '2026-06-01')
    await user.type(screen.getByLabelText(/^fecha hasta$/i), '2026-06-30')

    await waitFor(() => {
      expect(screen.getByRole('button', { name: paid!.supplierName })).toBeInTheDocument()
    })
    expect(screen.queryByRole('button', { name: withVehicle!.supplierName })).not.toBeInTheDocument()
  })

  it('narrows the supplier invoice list with the due date range filter', async () => {
    const user = userEvent.setup()
    renderSupplierInvoices()

    const [withVehicle, , , alreadySplit] = SEED_SUPPLIER_INVOICES
    await screen.findByRole('button', { name: withVehicle!.supplierName })

    await user.type(screen.getByLabelText(/^vencimiento desde$/i), '2026-08-06')
    await user.type(screen.getByLabelText(/^vencimiento hasta$/i), '2026-08-31')

    await waitFor(() => {
      expect(screen.getByRole('button', { name: alreadySplit!.supplierName })).toBeInTheDocument()
    })
    expect(screen.queryByRole('button', { name: withVehicle!.supplierName })).not.toBeInTheDocument()
  })

  it('narrows the supplier invoice list with the total amount range filter', async () => {
    const user = userEvent.setup()
    renderSupplierInvoices()

    const [withVehicle] = SEED_SUPPLIER_INVOICES
    await screen.findByRole('button', { name: withVehicle!.supplierName })

    await user.type(screen.getByLabelText(/^total mínimo$/i), '115')
    await user.type(screen.getByLabelText(/^total máximo$/i), '130')

    await waitFor(() => {
      expect(screen.getByRole('button', { name: withVehicle!.supplierName })).toBeInTheDocument()
    })
    expect(screen.getAllByRole('row')).toHaveLength(2) // header row + the one matching invoice
  })

  it('collapses and expands the filter panel when the trigger is clicked', async () => {
    const user = userEvent.setup()
    renderSupplierInvoices()

    await screen.findByRole('button', { name: SEED_SUPPLIER_INVOICES[0]!.supplierName })

    const trigger = screen.getByRole('button', { name: /mostrar u ocultar filtros/i })
    expect(trigger).toHaveAttribute('data-state', 'open')
    expect(screen.getByLabelText(/filtrar por categoría/i)).toBeInTheDocument()

    await user.click(trigger)

    expect(trigger).toHaveAttribute('data-state', 'closed')
    expect(screen.queryByLabelText(/filtrar por categoría/i)).not.toBeInTheDocument()
  })
})
