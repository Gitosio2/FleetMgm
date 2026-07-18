import type { VehicleStatus } from '@fleetmgm/api'

export const VEHICLE_STATUSES: VehicleStatus[] = ['ACTIVE', 'MAINTENANCE', 'INACTIVE', 'DECOMMISSIONED']

export const VEHICLE_STATUS_LABEL: Record<VehicleStatus, string> = {
  ACTIVE: 'Activo',
  MAINTENANCE: 'Mantenimiento',
  INACTIVE: 'Inactivo',
  DECOMMISSIONED: 'Dado de baja',
}
