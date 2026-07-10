import { useState } from 'react'
import { Plus } from 'lucide-react'
import type { ScheduleRange } from '@fleetmgm/api'
import { useMaintenanceRecords, useWorkshopSchedules } from '@fleetmgm/hooks'
import { useAuthStore } from '@fleetmgm/store'
import { Button } from '@/components/ui/button'
import { ScheduleRangeSelector } from '@/components/workshop/ScheduleRangeSelector'
import { ScheduleTable } from '@/components/workshop/ScheduleTable'
import { MaintenanceTable } from '@/components/workshop/MaintenanceTable'
import { MaintenanceFormModal } from '@/components/workshop/MaintenanceFormModal'
import { MANAGEMENT_ROLES } from '@/components/layout/nav-items'

const PAGE_SIZE = 20
const WORKSHOP_MANAGE_ROLES = [...MANAGEMENT_ROLES, 'WORKSHOP_STAFF']

export function Workshop() {
  const [range, setRange] = useState<ScheduleRange>('today')
  const [formOpen, setFormOpen] = useState(false)

  const role = useAuthStore((state) => state.role)
  const canManage = role != null && WORKSHOP_MANAGE_ROLES.includes(role)

  const { data: schedulesPage, isLoading: schedulesLoading } = useWorkshopSchedules(range, 0, PAGE_SIZE)
  const { data: maintenancePage, isLoading: maintenanceLoading } = useMaintenanceRecords(0, PAGE_SIZE)

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
        {canManage && (
          <Button onClick={openCreateForm}>
            <Plus className="size-4" />
            Nueva orden
          </Button>
        )}
      </div>

      <section className="flex flex-col gap-3">
        <div className="flex items-center justify-between">
          <h2 className="font-display text-lg font-semibold">Agenda</h2>
          <ScheduleRangeSelector value={range} onChange={setRange} />
        </div>
        {schedulesLoading ? (
          <p className="text-on-surface-variant">Cargando agenda…</p>
        ) : (
          <ScheduleTable schedules={schedulesPage?.content ?? []} />
        )}
      </section>

      <section className="flex flex-col gap-3">
        <h2 className="font-display text-lg font-semibold">Órdenes de mantenimiento</h2>
        {maintenanceLoading ? (
          <p className="text-on-surface-variant">Cargando mantenimientos…</p>
        ) : (
          <MaintenanceTable records={maintenancePage?.content ?? []} />
        )}
      </section>

      {canManage && <MaintenanceFormModal open={formOpen} onOpenChange={setFormOpen} />}
    </div>
  )
}
