import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { useAuthStore } from '@fleetmgm/store'
import { resetClientsMock, resetInvoicesMock, SEED_INVOICES } from '@/mocks/handlers'
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

    expect(await screen.findByText(draft!.invoiceNumber)).toBeInTheDocument()
    expect(screen.getByText(issued!.invoiceNumber)).toBeInTheDocument()
    expect(screen.getByText(paid!.invoiceNumber)).toBeInTheDocument()

    expect(screen.getByText('Borrador')).toBeInTheDocument()
    expect(screen.getByText('Emitida')).toBeInTheDocument()
    expect(screen.getByText('Pagada')).toBeInTheDocument()
  })

  it('creates a new DRAFT invoice via the modal', async () => {
    const user = userEvent.setup()
    renderBilling()

    await screen.findByText(SEED_INVOICES[0]!.invoiceNumber)

    await user.click(screen.getByRole('button', { name: /nueva factura/i }))
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

    await user.click(screen.getByRole('button', { name: /nueva factura/i }))
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
    expect(revokeObjectURL).toHaveBeenCalledWith('blob:mock-url')
    expect(clickedHref).toBe('blob:mock-url')
    expect(clickedDownload).toBe(`${paid.invoiceNumber}.pdf`)

    clickSpy.mockRestore()
  })
})
