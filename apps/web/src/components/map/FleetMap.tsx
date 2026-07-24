import 'leaflet/dist/leaflet.css'
import { MapContainer, TileLayer } from 'react-leaflet'
import type { VehicleCategory } from '@fleetmgm/api'
import { useGps } from '@fleetmgm/hooks'
import { MapViewController } from './MapViewController'
import { VehicleMarker } from './VehicleMarker'

// Initial viewport shows the whole country (not zoomed to the mock fleet's Madrid base in
// GpsMockScheduler) — markers still appear wherever their actual lat/lng puts them.
const SPAIN_CENTER: [number, number] = [40.0, -3.7]
const SPAIN_ZOOM = 6

type FleetMapProps = {
  category?: VehicleCategory
  vehicleId?: string
}

export function FleetMap({ category, vehicleId }: FleetMapProps) {
  const { data: positions } = useGps(category, vehicleId)

  return (
    <MapContainer
      center={SPAIN_CENTER}
      zoom={SPAIN_ZOOM}
      className="isolate h-full min-h-[32rem] w-full rounded-lg"
    >
      <TileLayer
        attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
        url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
      />
      <MapViewController
        vehicleId={vehicleId}
        positions={positions}
        fallbackCenter={SPAIN_CENTER}
        fallbackZoom={SPAIN_ZOOM}
      />
      {positions?.map((position) => <VehicleMarker key={position.id} position={position} />)}
    </MapContainer>
  )
}
