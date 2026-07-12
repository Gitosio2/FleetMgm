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

  return (
    <Popup>
      <div className="flex flex-col gap-1 text-sm">
        <span className="font-semibold">{vehicleLabel}</span>
        <span>{position.speed ?? 0} km/h</span>
      </div>
    </Popup>
  )
}
