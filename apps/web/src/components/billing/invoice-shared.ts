import type { Invoice, InvoiceStatus, LineItemResponse } from '@fleetmgm/api'

// Moved here from InvoiceStatusBadge for the same reason as the supplier-invoice equivalent
// (see supplier-invoice-shared.ts) — now also consumed by the status filter dropdown
// (InvoiceFilters).
export const STATUS_LABEL: Record<InvoiceStatus, string> = {
  DRAFT: 'Borrador',
  ISSUED: 'Emitida',
  PAID: 'Pagada',
  OVERDUE: 'Vencida',
}

export function sumLineItemSubtotals(lineItems: LineItemResponse[]): number {
  return lineItems.reduce((sum, item) => sum + item.subtotal, 0)
}

// InvoiceService.issue() computes taxAmount/total as BigDecimal, rounded HALF_UP to the cent at
// each step — plain `subtotal * taxRate` in JS double arithmetic can land a hair below a .5
// boundary that the exact decimal computation lands exactly on (e.g. subtotal=18.50, taxRate=0.21:
// the true product is exactly 3.885, but 18.5 * 0.21 evaluates to 3.884999999999999786... in IEEE
// 754, rounding down to 3.88 instead of the backend's HALF_UP 3.89 — a real, reproducible €0.01
// mismatch between this preview and the total issuing would actually produce). Working in integer
// cents/basis-points instead of floats avoids the representation error entirely: taxRate is always
// a multiple of 0.0001 server-side (BigDecimal(5,4)), so scaling it to an integer and multiplying
// two integers is always exact within JS's 2^53 safe-integer range for any realistic invoice size.
export function computeTaxAndTotal(subtotal: number, taxRate: number): { taxAmount: number; total: number } {
  const subtotalCents = Math.round(subtotal * 100)
  const taxRateBasisPoints = Math.round(taxRate * 10000)
  const taxAmountCents = Math.floor((subtotalCents * taxRateBasisPoints) / 10000 + 0.5)
  return { taxAmount: taxAmountCents / 100, total: (subtotalCents + taxAmountCents) / 100 }
}

// DRAFT invoices have subtotal/taxAmount/total stuck at 0 until InvoiceService.issue() computes
// them server-side from the line items — showing invoice.total directly for a DRAFT invoice would
// misleadingly read "0,00€" even with line items worth thousands (e.g. in InvoiceTable's Total
// column). This computes the same total client-side from the current line items and the persisted
// tax rate, mirroring what issuing would produce. ISSUED/PAID/OVERDUE invoices return the
// persisted total, which is final.
export function displayInvoiceTotal(invoice: Invoice): number {
  if (invoice.status !== 'DRAFT') {
    return invoice.total
  }
  const subtotal = sumLineItemSubtotals(invoice.lineItems)
  return computeTaxAndTotal(subtotal, invoice.taxRate).total
}
