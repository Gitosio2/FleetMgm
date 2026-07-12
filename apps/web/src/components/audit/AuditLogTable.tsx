import type { AuditLog } from '@fleetmgm/api'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { AUDIT_ACTION_LABEL, AUDIT_ENTITY_TYPE_LABEL } from './audit-log-shared'

type AuditLogTableProps = {
  entries: AuditLog[]
}

export function AuditLogTable({ entries }: AuditLogTableProps) {
  return (
    <Table>
      <TableHeader>
        <TableRow>
          <TableHead>Fecha</TableHead>
          <TableHead>Entidad</TableHead>
          <TableHead>Acción</TableHead>
          <TableHead>Usuario</TableHead>
          <TableHead>IP</TableHead>
          <TableHead>Detalle</TableHead>
        </TableRow>
      </TableHeader>
      <TableBody>
        {entries.map((entry) => (
          <TableRow key={entry.id}>
            <TableCell>{new Date(entry.performedAt).toLocaleString('es-ES')}</TableCell>
            <TableCell>{AUDIT_ENTITY_TYPE_LABEL[entry.entityType] ?? entry.entityType}</TableCell>
            <TableCell>{AUDIT_ACTION_LABEL[entry.action]}</TableCell>
            <TableCell>{entry.performedByEmail ?? '—'}</TableCell>
            <TableCell>{entry.ipAddress ?? '—'}</TableCell>
            <TableCell>{entry.details ?? '—'}</TableCell>
          </TableRow>
        ))}
      </TableBody>
    </Table>
  )
}
