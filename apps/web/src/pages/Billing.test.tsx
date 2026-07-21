import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { useAuthStore } from '@fleetmgm/store'
import { resetClientsMock, resetInvoicesMock, SEED_CLIENTS, SEED_INVOICES } from '@/mocks/handlers'
import { formatCurrency } from '@/lib/currency'
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
    loginAs('ADMIN')
  })

  afterEach(() => {
    useAuthStore.getState().logout()
  })

  it('renders the invoice list with status badges', async () => {
    renderBilling()

    const [draft, issued, paid] = SEED_INVOICES

    const draftRow = (await screen.findByText(draft!.invoiceNumber)).closest('tr')!
    const issuedRow = screen.getByText(issued!.invoiceNumber).closest('tr')!
    const paidRow = screen.getByText(paid!.invoiceNumber).closest('tr')!

    // Scoped to each row — the status filter dropdown also renders these same status labels
    // as <option> text, so an unscoped query would match twice.
    expect(within(draftRow).getByText('Borrador')).toBeInTheDocument()
    expect(within(issuedRow).getByText('Emitida')).toBeInTheDocument()
    expect(within(paidRow).getByText('Pagada')).toBeInTheDocument()
  })

  it('shows a computed total for a DRAFT invoice, not the persisted 0 it has until issued', async () => {
    renderBilling()

    const draft = SEED_INVOICES[0]!
    // The seed DRAFT invoice's persisted total is 0 (matches the real backend, where subtotal/
    // taxAmount/total stay 0 until issue() computes them) — the table must show a preview computed
    // from the two line items (2*150 + 1*80 = 380) and taxRate (0.21) instead: 380 * 1.21 = 459.80.
    expect(draft.total).toBe(0)
    const row = (await screen.findByText(draft.invoiceNumber)).closest('tr')!

    expect(within(row).getByText(formatCurrency(459.8))).toBeInTheDocument()
  })

  it('shows the issue date column in the invoice table, with a placeholder for DRAFT invoices', async () => {
    renderBilling()

    const [draft, issued, paid] = SEED_INVOICES

    // DRAFT has neither issueDate nor paymentDate — both cells fall back to the placeholder.
    const draftRow = (await screen.findByText(draft!.invoiceNumber)).closest('tr')!
    expect(within(draftRow).getAllByText('—')).toHaveLength(2)

    const issuedRow = screen.getByText(issued!.invoiceNumber).closest('tr')!
    expect(within(issuedRow).getByText(issued!.issueDate!)).toBeInTheDocument()

    const paidRow = screen.getByText(paid!.invoiceNumber).closest('tr')!
    expect(within(paidRow).getByText(paid!.issueDate!)).toBeInTheDocument()
  })

  it('shows the payment date column in the invoice table, with a placeholder for unpaid invoices', async () => {
    renderBilling()

    const [, issued, paid] = SEED_INVOICES

    const issuedRow = (await screen.findByText(issued!.invoiceNumber)).closest('tr')!
    expect(within(issuedRow).getByText('—')).toBeInTheDocument()

    const paidRow = screen.getByText(paid!.invoiceNumber).closest('tr')!
    expect(within(paidRow).getByText(paid!.paymentDate!)).toBeInTheDocument()
  })

  it('opens a read-only client modal from the invoice table, with no save button', async () => {
    const user = userEvent.setup()
    renderBilling()

    const client = SEED_CLIENTS[0]!
    const draft = SEED_INVOICES[0]!
    const row = (await screen.findByText(draft.invoiceNumber)).closest('tr')!

    await user.click(within(row).getByRole('button', { name: client.name }))

    const dialog = await screen.findByRole('dialog')
    expect(within(dialog).getByRole('heading', { name: 'Datos del cliente' })).toBeInTheDocument()
    const phoneInput = within(dialog).getByLabelText(/teléfono/i)
    await waitFor(() => expect(phoneInput).toHaveValue(client.phone))
    expect(within(dialog).getByLabelText(/correo electrónico/i)).toHaveValue(client.email)
    expect(phoneInput).toBeDisabled()
    expect(within(dialog).queryByRole('button', { name: /guardar cambios/i })).not.toBeInTheDocument()

    // Two "Cerrar" buttons exist in a read-only dialog now: the footer button and the icon-only
    // dialog close (its sr-only label is also "Cerrar" per the i18n fix), so getAllByRole is
    // required — the footer button renders first in DOM order.
    await user.click(within(dialog).getAllByRole('button', { name: /cerrar/i })[0]!)
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
  })

  it('creates a new DRAFT invoice via the modal', async () => {
    const user = userEvent.setup()
    renderBilling()

    await screen.findByText(SEED_INVOICES[0]!.invoiceNumber)

    await user.click(screen.getByRole('button', { name: /nueva factura/i }))
    await user.selectOptions(screen.getByLabelText(/^cliente$/i), 'client-2')
    await user.click(screen.getByRole('button', { name: /crear factura/i }))

    await waitFor(() => {
      expect(screen.getByText('INV-2026-00004')).toBeInTheDocument()
    })
    const row = screen.getByText('INV-2026-00004').closest('tr')!
    expect(within(row).getByText('Borrador')).toBeInTheDocument()
  })

  it('switches to edit mode after creating an invoice even when the active filters exclude it from the list', async () => {
    const user = userEvent.setup()
    renderBilling()

    await screen.findByText(SEED_INVOICES[0]!.invoiceNumber)

    // A brand-new invoice is always DRAFT, so filtering to PAID guarantees it won't appear in the
    // filtered list query — this is exactly the scenario that broke the old
    // data.content.find(editingInvoiceId) lookup: the modal would silently revert to "Nueva
    // factura" instead of switching to edit mode, and a second submit created a duplicate invoice.
    await user.selectOptions(screen.getByLabelText(/filtrar por estado/i), 'PAID')
    await waitFor(() => expect(screen.queryByText(SEED_INVOICES[0]!.invoiceNumber)).not.toBeInTheDocument())

    await user.click(screen.getByRole('button', { name: /nueva factura/i }))
    await user.selectOptions(screen.getByLabelText(/^cliente$/i), 'client-2')
    await user.click(screen.getByRole('button', { name: /crear factura/i }))

    expect(await screen.findByText('Editar factura')).toBeInTheDocument()
    expect(screen.getByLabelText(/^cliente$/i)).toHaveValue('client-2')
  })

  it('computes the DRAFT preview total with the same rounding issuing will produce (not float-truncated)', async () => {
    const user = userEvent.setup()
    renderBilling()

    await screen.findByText(SEED_INVOICES[0]!.invoiceNumber)

    await user.click(screen.getByRole('button', { name: /nueva factura/i }))
    await user.selectOptions(screen.getByLabelText(/^cliente$/i), 'client-2')
    await user.click(screen.getByRole('button', { name: /crear factura/i }))
    expect(await screen.findByText('Editar factura')).toBeInTheDocument()

    // 18.50 at the default 21% tax rate is exactly the boundary case where plain JS float
    // arithmetic (18.5 * 0.21 = 3.884999999999999786...) rounds down to 3.88/22.38 instead of the
    // backend's exact BigDecimal HALF_UP 3.89/22.39 — a real, reproducible €0.01 mismatch.
    await user.click(screen.getByRole('button', { name: /añadir línea/i }))
    await user.type(screen.getByLabelText(/^descripción$/i), 'Servicio')
    await user.clear(screen.getByLabelText(/cantidad/i))
    await user.type(screen.getByLabelText(/cantidad/i), '1')
    await user.type(screen.getByLabelText(/precio unitario/i), '18.50')
    await user.click(screen.getByRole('button', { name: /agregar línea/i }))
    await waitFor(() => expect(screen.getByText('Servicio')).toBeInTheDocument())

    // Preview panel (still DRAFT, computed client-side) must already show 22,39€, not 22,38€.
    // Scoped to the dialog — the table row behind it shows the same DRAFT preview total too, via
    // the same displayInvoiceTotal(), which would otherwise make this an ambiguous match.
    const dialog = screen.getByRole('dialog')
    expect(within(dialog).getByText(formatCurrency(22.39))).toBeInTheDocument()
    expect(within(dialog).queryByText(formatCurrency(22.38))).not.toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: /cerrar/i }))
    const row = screen.getByText('INV-2026-00004').closest('tr')!
    await user.click(within(row).getByRole('button', { name: /emitir/i }))

    await waitFor(() => expect(within(row).getByText(formatCurrency(22.39))).toBeInTheDocument())
  })

  it('shows the tax rate as a whole percentage and submits it as a fraction', async () => {
    const user = userEvent.setup()
    renderBilling()

    await screen.findByText(SEED_INVOICES[0]!.invoiceNumber)

    await user.click(screen.getByRole('button', { name: /nueva factura/i }))
    await user.selectOptions(screen.getByLabelText(/^cliente$/i), 'client-2')
    await user.type(screen.getByLabelText(/iva/i), '10')
    await user.click(screen.getByRole('button', { name: /crear factura/i }))

    // Creating no longer closes the modal — it switches into edit mode for the newly created
    // invoice instead, so the user can add line items in the same flow. The refetched invoice
    // re-populating the IVA field with "10" (whole percent, not "0.1" the raw fraction) proves
    // the same round trip the old reopen-via-"Editar" step used to prove.
    expect(await screen.findByText('Editar factura')).toBeInTheDocument()
    expect(await screen.findByLabelText(/iva/i)).toHaveValue(10)

    // Add a 100.00 line item and issue — if "10" had been sent to the backend
    // as a raw fraction (10) instead of 0.10, the resulting total would be
    // 1100.00 (100 + 1000%) instead of 110.00 (100 + 10%).
    await user.click(screen.getByRole('button', { name: /añadir línea/i }))
    await user.type(screen.getByLabelText(/^descripción$/i), 'Servicio')
    await user.clear(screen.getByLabelText(/cantidad/i))
    await user.type(screen.getByLabelText(/cantidad/i), '1')
    await user.type(screen.getByLabelText(/precio unitario/i), '100')
    await user.click(screen.getByRole('button', { name: /agregar línea/i }))
    await waitFor(() => expect(screen.getByText('Servicio')).toBeInTheDocument())
    await user.click(screen.getByRole('button', { name: /cerrar/i }))

    const row = screen.getByText('INV-2026-00004').closest('tr')!
    await user.click(within(row).getByRole('button', { name: /emitir/i }))

    await waitFor(() => expect(within(row).getByText(formatCurrency(110))).toBeInTheDocument())
  })

  it('shows the issue date as read-only for an ISSUED invoice, and hides it for a DRAFT invoice', async () => {
    const user = userEvent.setup()
    renderBilling()

    const draft = SEED_INVOICES[0]!
    const draftRow = (await screen.findByText(draft.invoiceNumber)).closest('tr')!
    await user.click(within(draftRow).getByRole('button', { name: /editar/i }))
    expect(await screen.findByRole('heading', { name: 'Editar factura' })).toBeInTheDocument()
    expect(screen.queryByText(/fecha de emisión/i)).not.toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: /cerrar/i }))

    const issued = SEED_INVOICES[1]!
    const issuedRow = screen.getByText(issued.invoiceNumber).closest('tr')!
    await user.click(within(issuedRow).getByRole('button', { name: /ver factura/i }))
    const dialog = await screen.findByRole('dialog')
    // ISSUED invoices are read-only (InvoiceService.update() rejects non-DRAFT edits) — the modal
    // shows this with the generic "Factura" heading instead of "Editar factura", and the row
    // action shows the "view" icon/label instead of "edit" to match.
    expect(await within(dialog).findByRole('heading', { name: 'Factura' })).toBeInTheDocument()
    expect(within(dialog).getByText(/fecha de emisión/i)).toBeInTheDocument()
    expect(within(dialog).getByText(issued.issueDate!)).toBeInTheDocument()
  })

  it('locks the top-level fields and shows a Cerrar button for a non-DRAFT invoice', async () => {
    const user = userEvent.setup()
    renderBilling()

    const issued = SEED_INVOICES[1]!
    const row = (await screen.findByText(issued.invoiceNumber)).closest('tr')!
    await user.click(within(row).getByRole('button', { name: /ver factura/i }))

    const dialog = await screen.findByRole('dialog')
    expect(await within(dialog).findByRole('heading', { name: 'Factura' })).toBeInTheDocument()
    expect(within(dialog).getByLabelText(/^cliente$/i)).toBeDisabled()
    expect(within(dialog).getByLabelText(/fecha de vencimiento/i)).toBeDisabled()
    expect(within(dialog).getByLabelText(/iva/i)).toBeDisabled()
    expect(within(dialog).getByLabelText(/notas/i)).toBeDisabled()
    expect(within(dialog).queryByRole('button', { name: /guardar cambios/i })).not.toBeInTheDocument()

    // Same disambiguation as above — the footer "Cerrar" button and the icon-only dialog close
    // both have the accessible name "Cerrar" now; the footer button is first in DOM order.
    await user.click(within(dialog).getAllByRole('button', { name: /cerrar/i })[0]!)
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
  })

  it('adds a line item to a DRAFT invoice', async () => {
    const user = userEvent.setup()
    renderBilling()

    const draft = SEED_INVOICES[0]!
    const row = (await screen.findByText(draft.invoiceNumber)).closest('tr')!
    await user.click(within(row).getByRole('button', { name: /editar/i }))

    expect(await screen.findByRole('heading', { name: 'Editar factura' })).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: /añadir línea/i }))
    await user.type(screen.getByLabelText(/^descripción$/i), 'Peaje')
    await user.clear(screen.getByLabelText(/cantidad/i))
    await user.type(screen.getByLabelText(/cantidad/i), '1')
    await user.type(screen.getByLabelText(/precio unitario/i), '25')

    await user.click(screen.getByRole('button', { name: /agregar línea/i }))

    await waitFor(() => expect(screen.getByText('Peaje')).toBeInTheDocument())
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

    await user.click(screen.getByRole('button', { name: /nueva factura/i }))
    await user.selectOptions(screen.getByLabelText(/^cliente$/i), 'client-2')
    await user.click(screen.getByRole('button', { name: /crear factura/i }))

    // Creating no longer closes the modal — it switches into edit mode for the newly created
    // invoice. Close it here so the underlying table (inert while the dialog is open) becomes
    // interactive again for the "Emitir" click below.
    expect(await screen.findByText('Editar factura')).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: /cerrar/i }))

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

    await user.click(within(row).getByRole('button', { name: /marcar factura como pagada/i }))

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

  it('narrows the invoice list with the invoice number search', async () => {
    const user = userEvent.setup()
    renderBilling()

    const [draft, issued] = SEED_INVOICES
    await screen.findByText(draft!.invoiceNumber)

    await user.type(screen.getByLabelText(/buscar por número de factura/i), '00002')

    await waitFor(() => {
      expect(screen.getByText(issued!.invoiceNumber)).toBeInTheDocument()
    })
    expect(screen.queryByText(draft!.invoiceNumber)).not.toBeInTheDocument()
  })

  it('narrows the invoice list with the client filter', async () => {
    const user = userEvent.setup()
    renderBilling()

    const [draft, issued] = SEED_INVOICES
    await screen.findByText(draft!.invoiceNumber)

    await user.selectOptions(screen.getByLabelText(/filtrar por cliente/i), issued!.clientId)

    await waitFor(() => {
      expect(screen.getByText(issued!.invoiceNumber)).toBeInTheDocument()
    })
    expect(screen.queryByText(draft!.invoiceNumber)).not.toBeInTheDocument()
  })

  it('narrows the invoice list with the status filter', async () => {
    const user = userEvent.setup()
    renderBilling()

    const [draft, , paid] = SEED_INVOICES
    await screen.findByText(draft!.invoiceNumber)

    await user.selectOptions(screen.getByLabelText(/filtrar por estado/i), 'PAID')

    await waitFor(() => {
      expect(screen.getByText(paid!.invoiceNumber)).toBeInTheDocument()
    })
    expect(screen.queryByText(draft!.invoiceNumber)).not.toBeInTheDocument()
  })

  it('narrows the invoice list with the issue date range filter', async () => {
    const user = userEvent.setup()
    renderBilling()

    const [draft, issued, paid] = SEED_INVOICES
    await screen.findByText(draft!.invoiceNumber)

    await user.type(screen.getByLabelText(/^emisión desde$/i), '2026-06-01')
    await user.type(screen.getByLabelText(/^emisión hasta$/i), '2026-06-30')

    await waitFor(() => {
      expect(screen.getByText(paid!.invoiceNumber)).toBeInTheDocument()
    })
    expect(screen.queryByText(issued!.invoiceNumber)).not.toBeInTheDocument()
    expect(screen.queryByText(draft!.invoiceNumber)).not.toBeInTheDocument()
  })

  it('narrows the invoice list with the due date range filter', async () => {
    const user = userEvent.setup()
    renderBilling()

    const [draft, issued, paid] = SEED_INVOICES
    await screen.findByText(draft!.invoiceNumber)

    await user.type(screen.getByLabelText(/^vencimiento desde$/i), '2026-07-25')
    await user.type(screen.getByLabelText(/^vencimiento hasta$/i), '2026-08-03')

    await waitFor(() => {
      expect(screen.getByText(draft!.invoiceNumber)).toBeInTheDocument()
    })
    expect(screen.queryByText(issued!.invoiceNumber)).not.toBeInTheDocument()
    expect(screen.queryByText(paid!.invoiceNumber)).not.toBeInTheDocument()
  })

  it('narrows the invoice list with the payment date range filter', async () => {
    const user = userEvent.setup()
    renderBilling()

    const [draft, issued, paid] = SEED_INVOICES
    await screen.findByText(draft!.invoiceNumber)

    await user.type(screen.getByLabelText(/^pago desde$/i), '2026-06-25')
    await user.type(screen.getByLabelText(/^pago hasta$/i), '2026-06-30')

    await waitFor(() => {
      expect(screen.getByText(paid!.invoiceNumber)).toBeInTheDocument()
    })
    expect(screen.queryByText(issued!.invoiceNumber)).not.toBeInTheDocument()
    expect(screen.queryByText(draft!.invoiceNumber)).not.toBeInTheDocument()
  })

  it('narrows the invoice list with the total amount range filter', async () => {
    const user = userEvent.setup()
    renderBilling()

    const [draft, issued] = SEED_INVOICES
    await screen.findByText(draft!.invoiceNumber)

    await user.type(screen.getByLabelText(/^total mínimo$/i), '600')
    await user.type(screen.getByLabelText(/^total máximo$/i), '700')

    await waitFor(() => {
      expect(screen.getByText(issued!.invoiceNumber)).toBeInTheDocument()
    })
    expect(screen.getAllByRole('row')).toHaveLength(2) // header row + the one matching invoice
  })

  it('collapses and expands the filter panel when the trigger is clicked', async () => {
    const user = userEvent.setup()
    renderBilling()

    await screen.findByText(SEED_INVOICES[0]!.invoiceNumber)

    const trigger = screen.getByRole('button', { name: /mostrar u ocultar filtros/i })
    expect(trigger).toHaveAttribute('data-state', 'open')
    expect(screen.getByLabelText(/filtrar por estado/i)).toBeInTheDocument()

    await user.click(trigger)

    expect(trigger).toHaveAttribute('data-state', 'closed')
    expect(screen.queryByLabelText(/filtrar por estado/i)).not.toBeInTheDocument()
  })
})
