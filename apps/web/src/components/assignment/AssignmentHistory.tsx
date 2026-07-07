import { useState } from 'react'
import { useEndAssignment, useWorkerAssignments } from '@fleetmgm/hooks'
import { Button } from '@/components/ui/button'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'

const PAGE_SIZE = 10

type AssignmentHistoryProps = {
  workerId: string
  canManage: boolean
}

export function AssignmentHistory({ workerId, canManage }: AssignmentHistoryProps) {
  const [page, setPage] = useState(0)
  const { data, isLoading } = useWorkerAssignments(workerId, page, PAGE_SIZE)
  const endAssignment = useEndAssignment()

  if (isLoading) {
    return <p className="text-on-surface-variant">Cargando historial…</p>
  }

  const assignments = data?.content ?? []

  return (
    <div className="flex flex-col gap-3">
      <h3 className="text-sm font-medium text-on-surface">Historial de asignaciones</h3>

      <Table>
        <TableHeader>
          <TableRow>
            <TableHead>Vehículo</TableHead>
            <TableHead>Inicio</TableHead>
            <TableHead>Fin</TableHead>
            <TableHead>Estado</TableHead>
            {canManage && <TableHead>Acciones</TableHead>}
          </TableRow>
        </TableHeader>
        <TableBody>
          {assignments.map((assignment) => (
            <TableRow key={assignment.id}>
              <TableCell>{assignment.vehicleLicensePlate ?? '—'}</TableCell>
              <TableCell>{assignment.startDate}</TableCell>
              <TableCell>{assignment.endDate ?? '—'}</TableCell>
              <TableCell>{assignment.active ? 'Activa' : 'Finalizada'}</TableCell>
              {canManage && (
                <TableCell>
                  {assignment.active && (
                    <Button
                      variant="ghost"
                      size="sm"
                      disabled={endAssignment.isPending}
                      onClick={() => endAssignment.mutate(assignment.id)}
                    >
                      Finalizar asignación
                    </Button>
                  )}
                </TableCell>
              )}
            </TableRow>
          ))}
        </TableBody>
      </Table>

      {data && data.totalPages > 1 && (
        <div className="flex items-center justify-between">
          <Button
            variant="outline"
            size="sm"
            disabled={page === 0}
            onClick={() => setPage((current) => current - 1)}
          >
            Anterior
          </Button>
          <span className="text-sm text-on-surface-variant">
            Página {page + 1} de {data.totalPages}
          </span>
          <Button
            variant="outline"
            size="sm"
            disabled={page + 1 >= data.totalPages}
            onClick={() => setPage((current) => current + 1)}
          >
            Siguiente
          </Button>
        </div>
      )}
    </div>
  )
}
