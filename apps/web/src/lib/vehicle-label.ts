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

// "<make> <model> - <plate>", or just "<make> <model>" for vehicles without a plate (heavy
// machinery) — the label JobFormModal's own vehicle <select> already used, centralized here
// (same rationale as WORKER_ROLE_LABEL in worker-shared.ts) so JobFilters' select matches it.
export function formatVehicleSelectLabel(vehicle: VehicleSelectSource): string {
  return `${vehicle.make} ${vehicle.model}${vehicle.licensePlate ? ` - ${vehicle.licensePlate}` : ''}`
}
