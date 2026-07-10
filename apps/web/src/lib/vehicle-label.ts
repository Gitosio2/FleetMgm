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
