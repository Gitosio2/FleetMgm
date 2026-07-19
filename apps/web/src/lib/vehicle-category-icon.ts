import { Car, Tractor, Truck, type LucideIcon } from 'lucide-react'
import type { VehicleCategory } from '@fleetmgm/api'

export const CATEGORY_ICON: Record<VehicleCategory, LucideIcon> = {
  LIGHT_VEHICLE: Car,
  HEAVY_VEHICLE: Truck,
  HEAVY_MACHINERY: Tractor,
}
