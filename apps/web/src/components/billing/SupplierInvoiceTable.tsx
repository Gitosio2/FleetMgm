import type { SupplierInvoice } from '@fleetmgm/api'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { SupplierInvoiceStatusBadge } from './SupplierInvoiceStatusBadge'
import { SupplierInvoiceActionButtons } from './SupplierInvoiceActionButtons'
import { EXPENSE_CATEGORY_LABEL } from './supplier-invoice-shared'

type SupplierInvoiceTableProps = {
  invoices: SupplierInvoice[]
  onEdit: (invoice: SupplierInvoice) => void
}

export function SupplierInvoiceTable({ invoices, onEdit }: SupplierInvoiceTableProps) {
  return (
    <Table>
      <TableHeader>
        <TableRow>
          <TableHead>Proveedor</TableHead>
          <TableHead>Categoría</TableHead>
          <TableHead>Vehículo</TableHead>
          <TableHead>Estado</TableHead>
          <TableHead>Total</TableHead>
          <TableHead>Fecha</TableHead>
          <TableHead>Vencimiento</TableHead>
          <TableHead>Acciones</TableHead>
        </TableRow>
      </TableHeader>
      <TableBody>
        {invoices.map((invoice) => (
          <TableRow key={invoice.id}>
            <TableCell>{invoice.supplierName}</TableCell>
            <TableCell>{EXPENSE_CATEGORY_LABEL[invoice.category]}</TableCell>
            <TableCell>{invoice.vehicleLicensePlate ?? '—'}</TableCell>
            <TableCell>
              <SupplierInvoiceStatusBadge status={invoice.status} />
            </TableCell>
            <TableCell>{invoice.total.toFixed(2)}</TableCell>
            <TableCell>{invoice.invoiceDate}</TableCell>
            <TableCell>{invoice.dueDate ?? '—'}</TableCell>
            <TableCell>
              <SupplierInvoiceActionButtons invoice={invoice} onEdit={onEdit} />
            </TableCell>
          </TableRow>
        ))}
      </TableBody>
    </Table>
  )
}
