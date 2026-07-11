import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { useAuthStore } from '@fleetmgm/store'
import {
  resetClientsMock,
  resetInvoicesMock,
  resetSupplierInvoicesMock,
  resetVehiclesMock,
  SEED_INVOICES,
  SEED_SUPPLIER_INVOICES,
} from '@/mocks/handlers'
import { Billing } from './Billing'

function renderBilling() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  })

  return render(
    <QueryClientProvider client={queryClient}>
      <Billing />
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

describe('Billing', () => {
  beforeEach(() => {
    resetInvoicesMock()
    resetClientsMock()
    resetSupplierInvoicesMock()
    resetVehiclesMock()
    loginAs('ADMIN')
  })

  afterEach(() => {
    useAuthStore.getState().logout()
  })

  it('renders the invoice list with status badges', async () => {
    renderBilling()

    const [draft, issued, paid] = SEED_INVOICES

    expect(await screen.findByText(draft!.invoiceNumber)).toBeInTheDocument()
    expect(screen.getByText(issued!.invoiceNumber)).toBeInTheDocument()
    expect(screen.getByText(paid!.invoiceNumber)).toBeInTheDocument()

    expect(screen.getByText('Borrador')).toBeInTheDocument()
    expect(screen.getByText('Emitida')).toBeInTheDocument()
    // Scoped to the PAID client invoice's row — a supplier invoice can also be PAID and shares the
    // same "Pagada" label, so a page-wide lookup is ambiguous once the supplier section is present.
    const paidRow = screen.getByText(paid!.invoiceNumber).closest('tr')!
    expect(within(paidRow).getByText('Pagada')).toBeInTheDocument()
  })

  it('shows the issue date column in the invoice table, with a placeholder for DRAFT invoices', async () => {
    renderBilling()

    const [draft, issued, paid] = SEED_INVOICES

    const draftRow = (await screen.findByText(draft!.invoiceNumber)).closest('tr')!
    expect(within(draftRow).getByText('—')).toBeInTheDocument()

    const issuedRow = screen.getByText(issued!.invoiceNumber).closest('tr')!
    expect(within(issuedRow).getByText(issued!.issueDate!)).toBeInTheDocument()

    const paidRow = screen.getByText(paid!.invoiceNumber).closest('tr')!
    expect(within(paidRow).getByText(paid!.issueDate!)).toBeInTheDocument()
  })

  it('creates a new DRAFT invoice via the modal', async () => {
    const user = userEvent.setup()
    renderBilling()

    await screen.findByText(SEED_INVOICES[0]!.invoiceNumber)

    await user.click(screen.getByRole('button', { name: /^nueva factura$/i }))
    await user.selectOptions(screen.getByLabelText(/cliente/i), 'client-2')
    await user.click(screen.getByRole('button', { name: /crear factura/i }))

    await waitFor(() => {
      expect(screen.getByText('INV-2026-00004')).toBeInTheDocument()
    })
    const row = screen.getByText('INV-2026-00004').closest('tr')!
    expect(within(row).getByText('Borrador')).toBeInTheDocument()
  })

  it('shows the tax rate as a whole percentage and submits it as a fraction', async () => {
    const user = userEvent.setup()
    renderBilling()

    await screen.findByText(SEED_INVOICES[0]!.invoiceNumber)

    await user.click(screen.getByRole('button', { name: /^nueva factura$/i }))
    await user.selectOptions(screen.getByLabelText(/cliente/i), 'client-2')
    await user.type(screen.getByLabelText(/iva/i), '10')
    await user.click(screen.getByRole('button', { name: /crear factura/i }))

    await waitFor(() => {
      expect(screen.getByText('INV-2026-00004')).toBeInTheDocument()
    })
    let row = screen.getByText('INV-2026-00004').closest('tr')!

    // Reopening the form must show "10" (whole percent), not "0.1" (the raw
    // fraction) — proves the display-side conversion, not just that whatever
    // was typed comes back unchanged.
    await user.click(within(row).getByRole('button', { name: /editar/i }))
    expect(await screen.findByLabelText(/iva/i)).toHaveValue(10)
    await user.click(screen.getByRole('button', { name: /guardar cambios/i }))

    // Add a 100.00 line item and issue — if "10" had been sent to the backend
    // as a raw fraction (10) instead of 0.10, the resulting total would be
    // 1100.00 (100 + 1000%) instead of 110.00 (100 + 10%).
    row = screen.getByText('INV-2026-00004').closest('tr')!
    await user.click(within(row).getByRole('button', { name: /editar/i }))
    await user.type(screen.getByLabelText(/^descripción$/i), 'Servicio')
    await user.clear(screen.getByLabelText(/cantidad/i))
    await user.type(screen.getByLabelText(/cantidad/i), '1')
    await user.type(screen.getByLabelText(/precio unitario/i), '100')
    await user.click(screen.getByRole('button', { name: /agregar línea/i }))
    await waitFor(() => expect(screen.getByText('Servicio')).toBeInTheDocument())
    await user.click(screen.getByRole('button', { name: /close/i }))

    row = screen.getByText('INV-2026-00004').closest('tr')!
    await user.click(within(row).getByRole('button', { name: /emitir/i }))

    await waitFor(() => expect(within(row).getByText('110.00')).toBeInTheDocument())
  })

  it('shows the issue date as read-only for an ISSUED invoice, and hides it for a DRAFT invoice', async () => {
    const user = userEvent.setup()
    renderBilling()

    const draft = SEED_INVOICES[0]!
    const draftRow = (await screen.findByText(draft.invoiceNumber)).closest('tr')!
    await user.click(within(draftRow).getByRole('button', { name: /editar/i }))
    expect(await screen.findByRole('heading', { name: 'Editar factura' })).toBeInTheDocument()
    expect(screen.queryByText(/fecha de emisión/i)).not.toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: /close/i }))

    const issued = SEED_INVOICES[1]!
    const issuedRow = screen.getByText(issued.invoiceNumber).closest('tr')!
    await user.click(within(issuedRow).getByRole('button', { name: /editar/i }))
    const dialog = await screen.findByRole('dialog')
    expect(await within(dialog).findByRole('heading', { name: 'Editar factura' })).toBeInTheDocument()
    expect(within(dialog).getByText(/fecha de emisión/i)).toBeInTheDocument()
    expect(within(dialog).getByText(issued.issueDate!)).toBeInTheDocument()
  })

  it('adds a line item to a DRAFT invoice', async () => {
    const user = userEvent.setup()
    renderBilling()

    const draft = SEED_INVOICES[0]!
    const row = (await screen.findByText(draft.invoiceNumber)).closest('tr')!
    await user.click(within(row).getByRole('button', { name: /editar/i }))

    expect(await screen.findByRole('heading', { name: 'Editar factura' })).toBeInTheDocument()

    await user.type(screen.getByLabelText(/^descripción$/i), 'Peaje')
    await user.clear(screen.getByLabelText(/cantidad/i))
    await user.type(screen.getByLabelText(/cantidad/i), '1')
    await user.type(screen.getByLabelText(/precio unitario/i), '25')

    await user.click(screen.getByRole('button', { name: /agregar línea/i }))

    // Scoped to the dialog — the supplier invoice category filter's "Peaje" (TOLL) <option> is
    // always present in the DOM (native <select> options render regardless of open state) and
    // collides with this line item's own "Peaje" description otherwise.
    const dialog = screen.getByRole('dialog')
    await waitFor(() => expect(within(dialog).getByText('Peaje')).toBeInTheDocument())
  })

  it('issues an invoice with at least one line item, transitioning it to ISSUED', async () => {
    const user = userEvent.setup()
    renderBilling()

    const draft = SEED_INVOICES[0]!
    const row = (await screen.findByText(draft.invoiceNumber)).closest('tr')!
    expect(within(row).getByText('Borrador')).toBeInTheDocument()

    await user.click(within(row).getByRole('button', { name: /emitir/i }))

    await waitFor(() => expect(within(row).getByText('Emitida')).toBeInTheDocument())
  })

  it('surfaces the 409 error when issuing an invoice with no line items', async () => {
    const user = userEvent.setup()
    renderBilling()

    await screen.findByText(SEED_INVOICES[0]!.invoiceNumber)

    await user.click(screen.getByRole('button', { name: /^nueva factura$/i }))
    await user.selectOptions(screen.getByLabelText(/cliente/i), 'client-2')
    await user.click(screen.getByRole('button', { name: /crear factura/i }))

    const row = (await screen.findByText('INV-2026-00004')).closest('tr')!
    await user.click(within(row).getByRole('button', { name: /emitir/i }))

    expect(await within(row).findByRole('alert')).toHaveTextContent(
      /no se puede emitir una factura sin líneas de factura/i,
    )
    expect(within(row).getByText('Borrador')).toBeInTheDocument()
  })

  it('marks an ISSUED invoice as paid, transitioning it to PAID', async () => {
    const user = userEvent.setup()
    renderBilling()

    const issued = SEED_INVOICES[1]!
    const row = (await screen.findByText(issued.invoiceNumber)).closest('tr')!
    expect(within(row).getByText('Emitida')).toBeInTheDocument()

    await user.click(within(row).getByRole('button', { name: /marcar pagada/i }))

    await waitFor(() => expect(within(row).getByText('Pagada')).toBeInTheDocument())
  })

  it('triggers a PDF download request when the download button is clicked', async () => {
    const user = userEvent.setup()

    const createObjectURL = vi.fn(() => 'blob:mock-url')
    const revokeObjectURL = vi.fn()
    window.URL.createObjectURL = createObjectURL
    window.URL.revokeObjectURL = revokeObjectURL

    let clickedHref = ''
    let clickedDownload = ''
    const clickSpy = vi
      .spyOn(HTMLAnchorElement.prototype, 'click')
      .mockImplementation(function (this: HTMLAnchorElement) {
        clickedHref = this.href
        clickedDownload = this.download
      })

    renderBilling()

    const paid = SEED_INVOICES[2]!
    const row = (await screen.findByText(paid.invoiceNumber)).closest('tr')!

    await user.click(within(row).getByRole('button', { name: /descargar pdf/i }))

    await waitFor(() => expect(clickSpy).toHaveBeenCalledTimes(1))
    expect(createObjectURL).toHaveBeenCalledTimes(1)
    expect(clickedHref).toBe('blob:mock-url')
    expect(clickedDownload).toBe(`${paid.invoiceNumber}.pdf`)
    // revokeObjectURL is deferred (setTimeout) so the browser has time to actually start reading
    // the blob before the URL is invalidated — see useDownloadInvoicePdf for why.
    await waitFor(() => expect(revokeObjectURL).toHaveBeenCalledWith('blob:mock-url'))

    clickSpy.mockRestore()
  })

  it('renders the supplier invoice list', async () => {
    renderBilling()

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
    renderBilling()

    await screen.findByText(SEED_SUPPLIER_INVOICES[0]!.supplierName)

    await user.click(screen.getByRole('button', { name: /nueva factura de proveedor/i }))
    await user.type(screen.getByLabelText(/^proveedor$/i), 'Ferretería Central')
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
    renderBilling()

    await screen.findByText(SEED_SUPPLIER_INVOICES[0]!.supplierName)

    await user.click(screen.getByRole('button', { name: /nueva factura de proveedor/i }))
    await user.type(screen.getByLabelText(/^proveedor$/i), 'Taller Rápido')
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
    renderBilling()

    const withVehicle = SEED_SUPPLIER_INVOICES[0]!
    const row = (await screen.findByText(withVehicle.supplierName)).closest('tr')!
    await user.click(within(row).getByRole('button', { name: /editar/i }))

    expect(await screen.findByRole('heading', { name: 'Editar factura de proveedor' })).toBeInTheDocument()
    expect(screen.getByLabelText(/^proveedor$/i)).toHaveValue(withVehicle.supplierName)

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
    renderBilling()

    const withoutVehicle = SEED_SUPPLIER_INVOICES[1]!
    const row = (await screen.findByText(withoutVehicle.supplierName)).closest('tr')!
    expect(within(row).getByText('Pendiente')).toBeInTheDocument()

    await user.click(within(row).getByRole('button', { name: /marcar pagada/i }))

    await waitFor(() => expect(within(row).getByText('Pagada')).toBeInTheDocument())
    expect(within(row).queryByRole('button', { name: /marcar pagada/i })).not.toBeInTheDocument()
  })

  it('hides the "Marcar pagada" button for an already-PAID supplier invoice', async () => {
    renderBilling()

    const paid = SEED_SUPPLIER_INVOICES[2]!
    const row = (await screen.findByText(paid.supplierName)).closest('tr')!

    expect(within(row).queryByRole('button', { name: /marcar pagada/i })).not.toBeInTheDocument()
  })

  it('narrows the supplier invoice list with the category filter', async () => {
    const user = userEvent.setup()
    renderBilling()

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
