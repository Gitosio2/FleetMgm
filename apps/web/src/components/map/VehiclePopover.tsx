import { Popup } from 'react-leaflet'
import type { GpsPosition } from '@fleetmgm/api'

type VehiclePopoverProps = {
  position: GpsPosition
}

export function VehiclePopover({ position }: VehiclePopoverProps) {
  return (
    <Popup>
      <div className="flex flex-col gap-1 text-sm">
        <span className="font-semibold">{position.licensePlate ?? 'Sin matrícula'}</span>
        <span>{position.speed ?? 0} km/h</span>
      </div>
    </Popup>
  )
}
