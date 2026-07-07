import { useState } from 'react'
import { useWorkers } from '@fleetmgm/hooks'
import { Button } from '@/components/ui/button'
import { AssignmentModal } from './AssignmentModal'
import { AssignmentHistory } from './AssignmentHistory'

type VehicleAssignmentPanelProps = {
  vehicleId: string
  vehicleLabel: string
  canManage: boolean
}

export function VehicleAssignmentPanel({ vehicleId, vehicleLabel, canManage }: VehicleAssignmentPanelProps) {
  const [modalOpen, setModalOpen] = useState(false)
  const [driverId, setDriverId] = useState<string>()
  const [driverName, setDriverName] = useState<string>()

  const { data: workersPage } = useWorkers(0, 100)
  const drivers = (workersPage?.content ?? []).filter(
    (worker) => worker.workerRole === 'DRIVER' || worker.workerRole === 'BOTH',
  )

  return (
    <div className="flex flex-col gap-4">
      <div className="flex items-center justify-between">
        <p className="text-sm text-on-surface-variant">
          {driverName ? `Conductor asignado: ${driverName}` : 'Sin conductor asignado en esta sesión'}
        </p>
        {canManage && (
          <Button size="sm" onClick={() => setModalOpen(true)} disabled={drivers.length === 0}>
            Asignar conductor
          </Button>
        )}
      </div>

      {driverId && <AssignmentHistory workerId={driverId} canManage={canManage} />}

      {canManage && (
        <AssignmentModal
          open={modalOpen}
          onOpenChange={setModalOpen}
          vehicleId={vehicleId}
          vehicleLabel={vehicleLabel}
          drivers={drivers}
          onAssigned={(assignment) => {
            setDriverId(assignment.driverId)
            setDriverName(assignment.driverName)
          }}
        />
      )}
    </div>
  )
}
