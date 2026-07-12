import { Popup } from 'react-leaflet'
import type { GpsPosition } from '@fleetmgm/api'
import { formatVehicleLabel } from '@/lib/vehicle-label'

type VehiclePopoverProps = {
  position: GpsPosition
}

export function VehiclePopover({ position }: VehiclePopoverProps) {
  const vehicleLabel = formatVehicleLabel({
    vehicleLicensePlate: position.licensePlate,
    vehicleMake: position.vehicleMake,
    vehicleModel: position.vehicleModel,
  })
  const vehicleMakeModel = [position.vehicleMake, position.vehicleModel].filter(Boolean).join(' ')
  // Only shown alongside the plate — for plateless vehicles, vehicleLabel already IS make/model.
  const showMakeModel = position.licensePlate != null && vehicleMakeModel.length > 0

  return (
    <Popup>
      <div className="flex flex-col gap-1 text-sm">
        <span className="font-semibold">{vehicleLabel}</span>
        {showMakeModel && <span>{vehicleMakeModel}</span>}
        <span>{position.speed ?? 0} km/h</span>
      </div>
    </Popup>
  )
}
