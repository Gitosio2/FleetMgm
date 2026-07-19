import { Info } from 'lucide-react'
import type { FinancialSummary as FinancialSummaryData, UpcomingInvoice } from '@fleetmgm/api'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Tooltip, TooltipContent, TooltipProvider, TooltipTrigger } from '@/components/ui/tooltip'
import { ClientInfoLink } from '@/components/client/ClientInfoLink'
import { SupplierInfoLink } from '@/components/supplier/SupplierInfoLink'
import { InvoiceInfoLink } from '@/components/billing/InvoiceInfoLink'
import { SupplierInvoiceInfoLink } from '@/components/billing/SupplierInvoiceInfoLink'
import { PayInvoiceButton } from '@/components/billing/PayInvoiceButton'
import { PaySupplierInvoiceButton } from '@/components/billing/PaySupplierInvoiceButton'
import { formatCurrency } from '@/lib/currency'
import { cn } from '@/lib/utils'

// Callers normalize both margins with this before passing them to formatMarginChange/
// marginChangeColor — missing/undefined API fields (or NaN from an unrelated computation)
// otherwise propagate into "NaN%"/"NaN€" instead of a sensible zero-based fallback.
function toSafeNumber(value: number): number {
  return Number.isFinite(value) ? value : 0
}

// previousMargin === 0 would divide by zero, so that case shows the euro difference instead of
// a percentage. Color logic never divides — it always compares the two margins directly,
// independent of this display-only guard.
function formatMarginChange(currentMargin: number, previousMargin: number): string {
  if (previousMargin === 0) {
    const diff = currentMargin - previousMargin
    return diff > 0 ? `+${formatCurrency(diff)}` : formatCurrency(diff)
  }
  const change = ((currentMargin - previousMargin) / Math.abs(previousMargin)) * 100
  const sign = change > 0 ? '+' : ''
  return `${sign}${change.toFixed(1)}%`
}

function marginChangeColor(currentMargin: number, previousMargin: number): string {
  if (currentMargin > previousMargin) return 'text-success'
  if (currentMargin < previousMargin) return 'text-error'
  return 'text-on-surface-variant'
}

type FinancialSummaryProps = {
  summary: FinancialSummaryData
}

type UpcomingInvoicesCardProps = {
  title: string
  invoices: UpcomingInvoice[]
  counterpartyType: 'CLIENT' | 'SUPPLIER'
}

function UpcomingInvoicesCard({ title, invoices, counterpartyType }: UpcomingInvoicesCardProps) {
  return (
    <Card>
      <CardHeader>
        <CardTitle className="text-sm font-medium text-on-surface-variant">{title}</CardTitle>
      </CardHeader>
      <CardContent>
        {invoices.length === 0 ? (
          <p className="text-sm text-on-surface-variant">Sin facturas próximas</p>
        ) : (
          <ul className="flex flex-col gap-2">
            {invoices.map((invoice) => (
              <li key={invoice.id} className="flex items-center justify-between gap-2 text-sm">
                <div className="flex flex-col">
                  {counterpartyType === 'CLIENT' ? (
                    <InvoiceInfoLink invoiceId={invoice.id} invoiceNumber={invoice.number} />
                  ) : (
                    <SupplierInvoiceInfoLink supplierInvoiceId={invoice.id} supplierInvoiceNumber={invoice.number} />
                  )}
                  {counterpartyType === 'CLIENT' ? (
                    <ClientInfoLink clientId={invoice.counterpartyId} clientName={invoice.counterparty} />
                  ) : (
                    <SupplierInfoLink supplierId={invoice.counterpartyId} supplierName={invoice.counterparty} />
                  )}
                </div>
                <div className="flex items-center gap-2">
                  <div className="flex flex-col items-end">
                    <span className="font-medium">{formatCurrency(invoice.amount)}</span>
                    <span className="text-on-surface-variant">
                      {new Date(invoice.dueDate).toLocaleDateString('es-ES')}
                    </span>
                    {invoice.overdue && (
                      <span className="inline-flex items-center rounded-full bg-error-container/40 px-2.5 py-0.5 text-xs font-medium text-error">
                        Vencida
                      </span>
                    )}
                  </div>
                  {counterpartyType === 'CLIENT' ? (
                    <PayInvoiceButton invoiceId={invoice.id} />
                  ) : (
                    <PaySupplierInvoiceButton supplierInvoiceId={invoice.id} />
                  )}
                </div>
              </li>
            ))}
          </ul>
        )}
      </CardContent>
    </Card>
  )
}

export function FinancialSummary({ summary }: FinancialSummaryProps) {
  const monthlyMargin = toSafeNumber(summary.monthlyRevenue - summary.monthlyCosts)
  const previousMonthMargin = toSafeNumber(summary.previousMonthMargin)

  return (
    <div className="grid grid-cols-1 gap-4 lg:grid-cols-3">
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-1.5 text-sm font-medium text-on-surface-variant">
            Resumen del mes
            <TooltipProvider>
              <Tooltip>
                <TooltipTrigger
                  type="button"
                  aria-label="Qué se cuenta en este resumen"
                  className="text-on-surface-variant hover:text-on-surface"
                >
                  <Info className="size-3.5" />
                </TooltipTrigger>
                <TooltipContent>
                  <p>
                    <strong>Ingresos:</strong> facturas a clientes emitidas este mes (por fecha de emisión), en
                    cualquier estado salvo anuladas.
                  </p>
                  <p className="mt-1.5">
                    <strong>Gastos:</strong> mantenimientos y facturas de proveedores de este mes.
                  </p>
                  <p className="mt-1.5">
                    <strong>Cobros:</strong> facturas a clientes pagadas este mes (por fecha de pago), sea cual sea
                    el mes en que se emitieron.
                  </p>
                  <p className="mt-1.5">
                    <strong>Beneficio:</strong> ingresos − gastos de este mes, comparado con el del mes anterior.
                  </p>
                </TooltipContent>
              </Tooltip>
            </TooltipProvider>
          </CardTitle>
        </CardHeader>
        <CardContent>
          <div className="flex flex-col gap-2">
            <div className="flex items-center justify-between text-sm">
              <span className="text-on-surface-variant">Ingresos</span>
              <span className="font-medium">{formatCurrency(summary.monthlyRevenue)}</span>
            </div>
            <div className="flex items-center justify-between text-sm">
              <span className="text-on-surface-variant">Gastos</span>
              <span className="font-medium">{formatCurrency(summary.monthlyCosts)}</span>
            </div>
            <div className="flex items-center justify-between text-sm">
              <span className="text-on-surface-variant">Cobros</span>
              <span className="font-medium">{formatCurrency(summary.monthlyCollections)}</span>
            </div>
            <div className="flex items-center justify-between border-t border-outline-variant pt-2 text-sm">
              <span className="text-on-surface-variant">Beneficio vs. mes anterior</span>
              <span
                className={cn('text-lg font-semibold', marginChangeColor(monthlyMargin, previousMonthMargin))}
              >
                {formatMarginChange(monthlyMargin, previousMonthMargin)}
              </span>
            </div>
          </div>
        </CardContent>
      </Card>

      <UpcomingInvoicesCard
        title="Facturas por cobrar"
        invoices={summary.upcomingReceivables}
        counterpartyType="CLIENT"
      />
      <UpcomingInvoicesCard
        title="Facturas por pagar"
        invoices={summary.upcomingPayables}
        counterpartyType="SUPPLIER"
      />
    </div>
  )
}
