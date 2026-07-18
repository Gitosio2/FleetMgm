import { useState } from 'react'
import { Plus } from 'lucide-react'
import type { ScheduleRange, WorkshopSchedule } from '@fleetmgm/api'
import { useWorkshopSchedules } from '@fleetmgm/hooks'
import { Button } from '@/components/ui/button'
import { ScheduleRangeSelector } from '@/components/workshop/ScheduleRangeSelector'
import { ScheduleTable } from '@/components/workshop/ScheduleTable'
import { ScheduleFormModal } from '@/components/workshop/ScheduleFormModal'

const PAGE_SIZE = 20

export function Workshop() {
  const [range, setRange] = useState<ScheduleRange>('today')
  const [schedulePage, setSchedulePage] = useState(0)
  const [scheduleFormOpen, setScheduleFormOpen] = useState(false)
  const [editingSchedule, setEditingSchedule] = useState<WorkshopSchedule | undefined>(undefined)

  const {
    data: schedulesPage,
    isLoading: schedulesLoading,
    isError: schedulesError,
  } = useWorkshopSchedules(range, schedulePage, PAGE_SIZE)

  function changeRange(nextRange: ScheduleRange) {
    setRange(nextRange)
    setSchedulePage(0)
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
      <div className="flex items-center justify-between">
        <div>
          <h1 className="font-display text-2xl font-semibold">Agenda</h1>
          <p className="text-on-surface-variant">Reservas y trabajos del taller — crear, iniciar, completar y cancelar.</p>
        </div>
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
