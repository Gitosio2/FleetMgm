import { Pencil } from 'lucide-react'
import type { Supplier } from '@fleetmgm/api'
import { Button } from '@/components/ui/button'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { SupplierDeleteButton } from './SupplierDeleteButton'

type SupplierTableProps = {
  suppliers: Supplier[]
  canManage: boolean
  onEdit: (supplier: Supplier) => void
}

export function SupplierTable({ suppliers, canManage, onEdit }: SupplierTableProps) {
  return (
    <Table>
      <TableHeader>
        <TableRow>
          <TableHead>Nombre</TableHead>
          <TableHead>NIF</TableHead>
          <TableHead>Correo electrónico</TableHead>
          <TableHead>Teléfono</TableHead>
          {canManage && <TableHead>Acciones</TableHead>}
        </TableRow>
      </TableHeader>
      <TableBody>
        {suppliers.map((supplier) => (
          <TableRow key={supplier.id}>
            <TableCell>{supplier.name}</TableCell>
            <TableCell>{supplier.taxId ?? '—'}</TableCell>
            <TableCell>{supplier.email ?? '—'}</TableCell>
            <TableCell>{supplier.phone ?? '—'}</TableCell>
            {canManage && (
              <TableCell>
                <div className="flex gap-1">
                  <Button
                    variant="ghost"
                    size="sm"
                    aria-label="Editar proveedor"
                    onClick={() => onEdit(supplier)}
                  >
                    <Pencil className="size-4" />
                  </Button>
                  <SupplierDeleteButton supplierId={supplier.id} supplierName={supplier.name} />
                </div>
              </TableCell>
            )}
          </TableRow>
        ))}
      </TableBody>
    </Table>
  )
}
