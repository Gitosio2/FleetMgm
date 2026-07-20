import { useState } from 'react'
import type { VehicleCategory } from '@fleetmgm/api'
import { useVehicles } from '@fleetmgm/hooks'
import { FleetMap } from '@/components/map/FleetMap'
import { formatVehicleLabel } from '@/lib/vehicle-label'
import { VEHICLE_CATEGORIES, VEHICLE_CATEGORY_LABEL } from '@/lib/vehicle-category-label'

const VEHICLES_PAGE_SIZE = 100

const selectClassName =
  'flex h-9 rounded-lg border border-outline-variant bg-surface-container-lowest px-3 text-sm text-on-surface focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-secondary-container'

export function Map() {
  const [categoryFilter, setCategoryFilter] = useState<VehicleCategory | ''>('')
  const [vehicleFilter, setVehicleFilter] = useState('')

  const { data: vehiclesPage } = useVehicles({}, 0, VEHICLES_PAGE_SIZE)

  return (
    <div className="flex h-full flex-col gap-6">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="font-display text-2xl font-semibold">Mapa GPS</h1>
          <p className="text-on-surface-variant">Ubicación en tiempo real de la flota.</p>
        </div>
        <div className="flex flex-wrap items-center gap-3">
          <select
            aria-label="Filtrar por tipo de vehículo"
            className={selectClassName}
            value={categoryFilter}
            onChange={(e) => setCategoryFilter(e.target.value as VehicleCategory | '')}
          >
            <option value="">Todos los tipos</option>
            {VEHICLE_CATEGORIES.map((category) => (
              <option key={category} value={category}>
                {VEHICLE_CATEGORY_LABEL[category]}
              </option>
            ))}
          </select>

          <select
            aria-label="Filtrar por vehículo"
            className={selectClassName}
            value={vehicleFilter}
            onChange={(e) => setVehicleFilter(e.target.value)}
          >
            <option value="">Todos los vehículos</option>
            {vehiclesPage?.content.map((vehicle) => (
              <option key={vehicle.id} value={vehicle.id}>
                {formatVehicleLabel({
                  vehicleLicensePlate: vehicle.licensePlate,
                  vehicleMake: vehicle.make,
                  vehicleModel: vehicle.model,
                })}
              </option>
            ))}
          </select>
        </div>
      </div>
      <FleetMap
        category={categoryFilter === '' ? undefined : categoryFilter}
        vehicleId={vehicleFilter === '' ? undefined : vehicleFilter}
      />
    </div>
  )
}
