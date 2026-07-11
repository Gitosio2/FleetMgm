import type { Invoice } from '@fleetmgm/api'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { InvoiceStatusBadge } from './InvoiceStatusBadge'
import { InvoiceActionButtons } from './InvoiceActionButtons'

type InvoiceTableProps = {
  invoices: Invoice[]
  onEdit: (invoice: Invoice) => void
}

export function InvoiceTable({ invoices, onEdit }: InvoiceTableProps) {
  return (
    <Table>
      <TableHeader>
        <TableRow>
          <TableHead>Número</TableHead>
          <TableHead>Cliente</TableHead>
          <TableHead>Estado</TableHead>
          <TableHead>Total</TableHead>
          <TableHead>Emisión</TableHead>
          <TableHead>Vencimiento</TableHead>
          <TableHead>Acciones</TableHead>
        </TableRow>
      </TableHeader>
      <TableBody>
        {invoices.map((invoice) => (
          <TableRow key={invoice.id}>
            <TableCell>{invoice.invoiceNumber}</TableCell>
            <TableCell>{invoice.clientName}</TableCell>
            <TableCell>
              <InvoiceStatusBadge status={invoice.status} />
            </TableCell>
            <TableCell>{invoice.total.toFixed(2)}</TableCell>
            <TableCell>{invoice.issueDate ?? '—'}</TableCell>
            <TableCell>{invoice.dueDate ?? '—'}</TableCell>
            <TableCell>
              <InvoiceActionButtons invoice={invoice} onEdit={onEdit} />
            </TableCell>
          </TableRow>
        ))}
      </TableBody>
    </Table>
  )
}
