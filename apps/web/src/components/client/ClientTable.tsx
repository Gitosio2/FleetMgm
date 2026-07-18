import { Pencil } from 'lucide-react'
import type { Client } from '@fleetmgm/api'
import { Button } from '@/components/ui/button'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { ClientDeleteButton } from './ClientDeleteButton'

type ClientTableProps = {
  clients: Client[]
  canManage: boolean
  onEdit: (client: Client) => void
}

export function ClientTable({ clients, canManage, onEdit }: ClientTableProps) {
  return (
    <Table>
      <TableHeader>
        <TableRow>
          <TableHead>Nombre</TableHead>
          <TableHead>ID fiscal</TableHead>
          <TableHead>Correo electrónico</TableHead>
          <TableHead>Teléfono</TableHead>
          {canManage && <TableHead>Acciones</TableHead>}
        </TableRow>
      </TableHeader>
      <TableBody>
        {clients.map((client) => (
          <TableRow key={client.id}>
            <TableCell>{client.name}</TableCell>
            <TableCell>{client.taxId}</TableCell>
            <TableCell>{client.email ?? '—'}</TableCell>
            <TableCell>{client.phone ?? '—'}</TableCell>
            {canManage && (
              <TableCell>
                <div className="flex gap-1">
                  <Button
                    variant="ghost"
                    size="sm"
                    aria-label="Editar cliente"
                    title="Editar cliente"
                    onClick={() => onEdit(client)}
                  >
                    <Pencil className="size-4" />
                  </Button>
                  <ClientDeleteButton clientId={client.id} clientName={client.name} />
                </div>
              </TableCell>
            )}
          </TableRow>
        ))}
      </TableBody>
    </Table>
  )
}
