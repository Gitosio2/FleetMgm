import type { MaintenanceRecord, MaintenanceStatus, WorkshopSchedule, WorkshopStatus } from '@fleetmgm/api'
import { useMaintenanceRecords, useWorkshopSchedules } from '@fleetmgm/hooks'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { formatVehicleLabel } from '@/lib/vehicle-label'
import { MaintenanceStatusBadge } from './MaintenanceStatusBadge'
import { ScheduleStatusBadge } from './ScheduleStatusBadge'

const PAGE_SIZE = 100

type DayScheduleRow =
  | {
      key: string
      time: string | null
      vehicleLabel: string
      description: string
      source: 'Agenda'
      status: WorkshopStatus
    }
  | {
      key: string
      time: string | null
      vehicleLabel: string
      description: string
      source: 'Mantenimiento'
      status: MaintenanceStatus
    }

function todayISODate(): string {
  const now = new Date()
  const year = now.getFullYear()
  const month = String(now.getMonth() + 1).padStart(2, '0')
  const day = String(now.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

// Stable comparator: entries without a time sort after every entry that has one, without
// disturbing the relative order of entries that share the same "no time" bucket or of entries
// that do have a time (Array.prototype.sort is stable in every JS engine this project targets).
function compareByTime(a: DayScheduleRow, b: DayScheduleRow): number {
  if (a.time == null && b.time == null) {
    return 0
  }
  if (a.time == null) {
    return 1
  }
  if (b.time == null) {
    return -1
  }
  // "HH:mm:ss" strings compare correctly lexicographically.
  return a.time.localeCompare(b.time)
}

function fromSchedule(schedule: WorkshopSchedule): DayScheduleRow {
  return {
    key: `schedule-${schedule.id}`,
    time: schedule.scheduledStartTime,
    vehicleLabel: formatVehicleLabel(schedule),
    description: schedule.type,
    source: 'Agenda',
    status: schedule.status,
  }
}

function fromMaintenance(record: MaintenanceRecord): DayScheduleRow {
  return {
    key: `maintenance-${record.id}`,
    time: record.workshopEntryTime ?? record.workshopExitTime,
    vehicleLabel: formatVehicleLabel(record),
    description: record.type,
    source: 'Mantenimiento',
    status: record.status,
  }
}

// Self-contained "today" view of the workshop: combines the planned side (WorkshopSchedule,
// server-filtered to 'today') with the actual side (MaintenanceRecord, filtered client-side —
// the list endpoint has no date filter) into one chronological table. Read-only by design.
export function DaySchedule() {
  const {
    data: schedulesPage,
    isLoading: schedulesLoading,
    isError: schedulesError,
  } = useWorkshopSchedules('today', 0, PAGE_SIZE)
  const {
    data: maintenancePage,
    isLoading: maintenanceLoading,
    isError: maintenanceError,
  } = useMaintenanceRecords(0, PAGE_SIZE)

  if (schedulesLoading || maintenanceLoading) {
    return <p className="text-on-surface-variant">Cargando horario…</p>
  }

  if (schedulesError || maintenanceError) {
    return (
      <p role="alert" className="text-sm text-error">
        No se pudieron cargar los datos.
      </p>
    )
  }

  const today = todayISODate()
  const scheduleRows = (schedulesPage?.content ?? []).map(fromSchedule)
  const maintenanceRows = (maintenancePage?.content ?? [])
    .filter((record) => record.workshopEntryDate === today || record.workshopExitDate === today)
    .map(fromMaintenance)

  const rows = [...scheduleRows, ...maintenanceRows].sort(compareByTime)

  if (rows.length === 0) {
    return <p className="text-on-surface-variant">No hay trabajos programados ni realizados hoy.</p>
  }

  return (
    <Table>
      <TableHeader>
        <TableRow>
          <TableHead>Hora</TableHead>
          <TableHead>Vehículo</TableHead>
          <TableHead>Descripción</TableHead>
          <TableHead>Origen</TableHead>
          <TableHead>Estado</TableHead>
        </TableRow>
      </TableHeader>
      <TableBody>
        {rows.map((row) => (
          <TableRow key={row.key}>
            <TableCell>{row.time ? row.time.slice(0, 5) : '—'}</TableCell>
            <TableCell>{row.vehicleLabel}</TableCell>
            <TableCell>{row.description}</TableCell>
            <TableCell>{row.source}</TableCell>
            <TableCell>
              {row.source === 'Agenda' ? (
                <ScheduleStatusBadge status={row.status} />
              ) : (
                <MaintenanceStatusBadge status={row.status} />
              )}
            </TableCell>
          </TableRow>
        ))}
      </TableBody>
    </Table>
  )
}
