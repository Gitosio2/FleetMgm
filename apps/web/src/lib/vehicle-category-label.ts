import type { VehicleCategory } from '@fleetmgm/api'

export const VEHICLE_CATEGORIES: VehicleCategory[] = ['LIGHT_VEHICLE', 'HEAVY_VEHICLE', 'HEAVY_MACHINERY']

export const VEHICLE_CATEGORY_LABEL: Record<VehicleCategory, string> = {
  LIGHT_VEHICLE: 'Vehículo ligero',
  HEAVY_VEHICLE: 'Vehículo pesado',
  HEAVY_MACHINERY: 'Maquinaria pesada',
}
