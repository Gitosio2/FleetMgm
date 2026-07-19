type VehicleLabelSource = {
  vehicleLicensePlate: string | null
  vehicleMake: string | null
  vehicleModel: string | null
}

// Falls back to "<make> <model>" for vehicles without a plate (e.g. heavy machinery)
export function formatVehicleLabel(source: VehicleLabelSource): string {
  if (source.vehicleLicensePlate) {
    return source.vehicleLicensePlate
  }
  if (source.vehicleMake || source.vehicleModel) {
    return [source.vehicleMake, source.vehicleModel].filter(Boolean).join(' ')
  }
  return '—'
}

type VehicleSelectSource = {
  make: string
  model: string
  licensePlate: string | null
}

// "<plate> - <make> <model>", or just "<make> <model>" for vehicles without a plate (heavy
// machinery) — shared by JobFormModal's and JobFilters' vehicle <select>s (same rationale as
// WORKER_ROLE_LABEL in worker-shared.ts) so both stay in sync from one place.
export function formatVehicleSelectLabel(vehicle: VehicleSelectSource): string {
  const makeModel = `${vehicle.make} ${vehicle.model}`
  return vehicle.licensePlate ? `${vehicle.licensePlate} - ${makeModel}` : makeModel
}
