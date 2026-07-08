import type { Assignment } from '@fleetmgm/api'

// Falls back to "<make> <model>" for vehicles without a plate (e.g. heavy machinery)
export function formatVehicleLabel(
  assignment: Pick<Assignment, 'vehicleLicensePlate' | 'vehicleMake' | 'vehicleModel'>,
): string {
  if (assignment.vehicleLicensePlate) {
    return assignment.vehicleLicensePlate
  }
  if (assignment.vehicleMake || assignment.vehicleModel) {
    return [assignment.vehicleMake, assignment.vehicleModel].filter(Boolean).join(' ')
  }
  return '—'
}
