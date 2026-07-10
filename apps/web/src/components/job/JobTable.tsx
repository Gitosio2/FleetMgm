import { Pencil } from 'lucide-react'
import type { Job } from '@fleetmgm/api'
import { Button } from '@/components/ui/button'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { formatVehicleLabel } from '@/lib/vehicle-label'
import { JobStatusBadge } from './JobStatusBadge'
import { JobActionButtons } from './JobActionButtons'

type JobTableProps = {
  jobs: Job[]
  canManage: boolean
  onEdit: (job: Job) => void
}

export function JobTable({ jobs, canManage, onEdit }: JobTableProps) {
  return (
    <Table>
      <TableHeader>
        <TableRow>
          <TableHead>Título</TableHead>
          <TableHead>Ruta</TableHead>
          <TableHead>Vehículo</TableHead>
          <TableHead>Conductor</TableHead>
          <TableHead>Estado</TableHead>
          <TableHead>Acciones</TableHead>
        </TableRow>
      </TableHeader>
      <TableBody>
        {jobs.map((job) => (
          <TableRow key={job.id}>
            <TableCell>{job.title}</TableCell>
            <TableCell>
              {job.originLocation} → {job.destinationLocation}
            </TableCell>
            <TableCell>{formatVehicleLabel(job)}</TableCell>
            <TableCell>{job.assignedDriverName ?? '—'}</TableCell>
            <TableCell>
              <JobStatusBadge status={job.status} />
            </TableCell>
            <TableCell>
              <div className="flex items-center gap-1">
                {canManage && (
                  <Button
                    variant="ghost"
                    size="sm"
                    aria-label="Editar trabajo"
                    onClick={() => onEdit(job)}
                  >
                    <Pencil className="size-4" />
                  </Button>
                )}
                <JobActionButtons job={job} />
              </div>
            </TableCell>
          </TableRow>
        ))}
      </TableBody>
    </Table>
  )
}
