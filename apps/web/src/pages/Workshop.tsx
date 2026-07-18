import { useState } from 'react'
import { Plus } from 'lucide-react'
import type { MaintenanceRecord, ScheduleRange, WorkshopSchedule } from '@fleetmgm/api'
import { useMaintenanceRecords, useWorkshopSchedules } from '@fleetmgm/hooks'
import { Button } from '@/components/ui/button'
import { DaySchedule } from '@/components/workshop/DaySchedule'
import { ScheduleRangeSelector } from '@/components/workshop/ScheduleRangeSelector'
import { ScheduleTable } from '@/components/workshop/ScheduleTable'
import { MaintenanceTable } from '@/components/workshop/MaintenanceTable'
import { MaintenanceFormModal } from '@/components/workshop/MaintenanceFormModal'
import { ScheduleFormModal } from '@/components/workshop/ScheduleFormModal'

const PAGE_SIZE = 20

export function Workshop() {
  const [range, setRange] = useState<ScheduleRange>('today')
  const [schedulePage, setSchedulePage] = useState(0)
  const [maintenancePageNumber, setMaintenancePageNumber] = useState(0)
  const [formOpen, setFormOpen] = useState(false)
  const [scheduleFormOpen, setScheduleFormOpen] = useState(false)
  const [editingMaintenance, setEditingMaintenance] = useState<MaintenanceRecord | undefined>(undefined)
  const [editingSchedule, setEditingSchedule] = useState<WorkshopSchedule | undefined>(undefined)

  const {
    data: schedulesPage,
    isLoading: schedulesLoading,
    isError: schedulesError,
  } = useWorkshopSchedules(range, schedulePage, PAGE_SIZE)
  const {
    data: maintenancePage,
    isLoading: maintenanceLoading,
    isError: maintenanceError,
  } = useMaintenanceRecords(maintenancePageNumber, PAGE_SIZE)

  function changeRange(nextRange: ScheduleRange) {
    setRange(nextRange)
    setSchedulePage(0)
  }

  function openCreateForm() {
    setEditingMaintenance(undefined)
    setFormOpen(true)
  }

  function openEditForm(record: MaintenanceRecord) {
    setEditingMaintenance(record)
    setFormOpen(true)
  }

  function openCreateScheduleForm() {
    setEditingSchedule(undefined)
    setScheduleFormOpen(true)
  }

  function openEditScheduleForm(schedule: WorkshopSchedule) {
    setEditingSchedule(schedule)
    setScheduleFormOpen(true)
  }

  return (
    <div className="flex flex-col gap-6">
      <div>
        <h1 className="font-display text-2xl font-semibold">Taller</h1>
        <p className="text-on-surface-variant">Agenda del taller y órdenes de mantenimiento.</p>
      </div>

      <section className="flex flex-col gap-3">
        <h2 className="font-display text-lg font-semibold">Horario del día</h2>
        <DaySchedule />
      </section>

      <section className="flex flex-col gap-3">
        <div className="flex items-center justify-between">
          <h2 className="font-display text-lg font-semibold">Agenda</h2>
          <div className="flex items-center gap-3">
            <ScheduleRangeSelector value={range} onChange={changeRange} />
            <Button size="sm" onClick={openCreateScheduleForm}>
              <Plus className="size-4" />
              Nueva entrada
            </Button>
          </div>
        </div>
        {schedulesLoading ? (
          <p className="text-on-surface-variant">Cargando agenda…</p>
        ) : schedulesError ? (
          <p role="alert" className="text-sm text-error">
            No se pudieron cargar los datos.
          </p>
        ) : (
          <ScheduleTable schedules={schedulesPage?.content ?? []} onEdit={openEditScheduleForm} />
        )}
        {schedulesPage && schedulesPage.totalPages > 1 && (
          <div className="flex items-center justify-between">
            <Button
              variant="outline"
              size="sm"
              disabled={schedulePage === 0}
              onClick={() => setSchedulePage((current) => current - 1)}
            >
              Anterior
            </Button>
            <span className="text-sm text-on-surface-variant">
              Página {schedulePage + 1} de {schedulesPage.totalPages}
            </span>
            <Button
              variant="outline"
              size="sm"
              disabled={schedulePage + 1 >= schedulesPage.totalPages}
              onClick={() => setSchedulePage((current) => current + 1)}
            >
              Siguiente
            </Button>
          </div>
        )}
      </section>

      <section className="flex flex-col gap-3">
        <div className="flex items-center justify-between">
          <h2 className="font-display text-lg font-semibold">Órdenes de mantenimiento</h2>
          <Button size="sm" onClick={openCreateForm}>
            <Plus className="size-4" />
            Nueva orden
          </Button>
        </div>
        {maintenanceLoading ? (
          <p className="text-on-surface-variant">Cargando mantenimientos…</p>
        ) : maintenanceError ? (
          <p role="alert" className="text-sm text-error">
            No se pudieron cargar los datos.
          </p>
        ) : (
          <MaintenanceTable records={maintenancePage?.content ?? []} onEdit={openEditForm} />
        )}
        {maintenancePage && maintenancePage.totalPages > 1 && (
          <div className="flex items-center justify-between">
            <Button
              variant="outline"
              size="sm"
              disabled={maintenancePageNumber === 0}
              onClick={() => setMaintenancePageNumber((current) => current - 1)}
            >
              Anterior
            </Button>
            <span className="text-sm text-on-surface-variant">
              Página {maintenancePageNumber + 1} de {maintenancePage.totalPages}
            </span>
            <Button
              variant="outline"
              size="sm"
              disabled={maintenancePageNumber + 1 >= maintenancePage.totalPages}
              onClick={() => setMaintenancePageNumber((current) => current + 1)}
            >
              Siguiente
            </Button>
          </div>
        )}
      </section>

      <MaintenanceFormModal
        open={formOpen}
        onOpenChange={(open) => {
          setFormOpen(open)
          if (!open) {
            setEditingMaintenance(undefined)
          }
        }}
        record={editingMaintenance}
      />
      <ScheduleFormModal
        open={scheduleFormOpen}
        onOpenChange={(open) => {
          setScheduleFormOpen(open)
          if (!open) {
            setEditingSchedule(undefined)
          }
        }}
        schedule={editingSchedule}
      />
    </div>
  )
}
