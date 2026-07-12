import 'leaflet/dist/leaflet.css'
import { MapContainer, TileLayer } from 'react-leaflet'
import type { VehicleCategory } from '@fleetmgm/api'
import { useGps } from '@fleetmgm/hooks'
import { VehicleMarker } from './VehicleMarker'

// Same base coordinates as GpsMockScheduler.BASE_LATITUDE/BASE_LONGITUDE on the backend —
// the map starts centered where the mock fleet actually appears.
const FLEET_BASE_POSITION: [number, number] = [40.4168, -3.7038]

type FleetMapProps = {
  category?: VehicleCategory
  vehicleId?: string
}

export function FleetMap({ category, vehicleId }: FleetMapProps) {
  const { data: positions } = useGps(category, vehicleId)

  return (
    <MapContainer center={FLEET_BASE_POSITION} zoom={12} className="h-full min-h-[32rem] w-full rounded-lg">
      <TileLayer
        attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
        url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
      />
      {positions?.map((position) => <VehicleMarker key={position.id} position={position} />)}
    </MapContainer>
  )
}
