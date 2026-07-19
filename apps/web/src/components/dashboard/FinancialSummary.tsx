import type { FinancialSummary as FinancialSummaryData, UpcomingInvoice } from '@fleetmgm/api'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { ClientInfoLink } from '@/components/client/ClientInfoLink'
import { SupplierInfoLink } from '@/components/supplier/SupplierInfoLink'
import { InvoiceInfoLink } from '@/components/billing/InvoiceInfoLink'
import { SupplierInvoiceInfoLink } from '@/components/billing/SupplierInvoiceInfoLink'
import { PayInvoiceButton } from '@/components/billing/PayInvoiceButton'
import { PaySupplierInvoiceButton } from '@/components/billing/PaySupplierInvoiceButton'
import { formatCurrency } from '@/lib/currency'

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
  return (
    <div className="grid grid-cols-1 gap-4 lg:grid-cols-3">
      <Card>
        <CardHeader>
          <CardTitle className="text-sm font-medium text-on-surface-variant">Costes del mes</CardTitle>
        </CardHeader>
        <CardContent>
          <p className="text-3xl font-semibold">{formatCurrency(summary.monthlyCosts)}</p>
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
