import { useState } from 'react'
import { Plus } from 'lucide-react'
import type { ScheduleRange } from '@fleetmgm/api'
import { useMaintenanceRecords, useWorkshopSchedules } from '@fleetmgm/hooks'
import { Button } from '@/components/ui/button'
import { ScheduleRangeSelector } from '@/components/workshop/ScheduleRangeSelector'
import { ScheduleTable } from '@/components/workshop/ScheduleTable'
import { MaintenanceTable } from '@/components/workshop/MaintenanceTable'
import { MaintenanceFormModal } from '@/components/workshop/MaintenanceFormModal'
import { ScheduleFormModal } from '@/components/workshop/ScheduleFormModal'

const PAGE_SIZE = 20

export function Workshop() {
  const [range, setRange] = useState<ScheduleRange>('today')
  const [formOpen, setFormOpen] = useState(false)
  const [scheduleFormOpen, setScheduleFormOpen] = useState(false)

  const {
    data: schedulesPage,
    isLoading: schedulesLoading,
    isError: schedulesError,
  } = useWorkshopSchedules(range, 0, PAGE_SIZE)
  const {
    data: maintenancePage,
    isLoading: maintenanceLoading,
    isError: maintenanceError,
  } = useMaintenanceRecords(0, PAGE_SIZE)

  function openCreateForm() {
    setFormOpen(true)
  }

  return (
    <div className="flex flex-col gap-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="font-display text-2xl font-semibold">Taller</h1>
          <p className="text-on-surface-variant">Agenda del taller y órdenes de mantenimiento.</p>
        </div>
        <Button onClick={openCreateForm}>
          <Plus className="size-4" />
          Nueva orden
        </Button>
      </div>

      <section className="flex flex-col gap-3">
        <div className="flex items-center justify-between">
          <h2 className="font-display text-lg font-semibold">Agenda</h2>
          <div className="flex items-center gap-3">
            <ScheduleRangeSelector value={range} onChange={setRange} />
            <Button variant="outline" size="sm" onClick={() => setScheduleFormOpen(true)}>
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
          <ScheduleTable schedules={schedulesPage?.content ?? []} />
        )}
      </section>

      <section className="flex flex-col gap-3">
        <h2 className="font-display text-lg font-semibold">Órdenes de mantenimiento</h2>
        {maintenanceLoading ? (
          <p className="text-on-surface-variant">Cargando mantenimientos…</p>
        ) : maintenanceError ? (
          <p role="alert" className="text-sm text-error">
            No se pudieron cargar los datos.
          </p>
        ) : (
          <MaintenanceTable records={maintenancePage?.content ?? []} />
        )}
      </section>

      <MaintenanceFormModal open={formOpen} onOpenChange={setFormOpen} />
      <ScheduleFormModal open={scheduleFormOpen} onOpenChange={setScheduleFormOpen} />
    </div>
  )
}
