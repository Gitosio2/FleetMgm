import { useEffect, useRef } from 'react'
import { useMap } from 'react-leaflet'
import type { GpsPosition } from '@fleetmgm/api'

const VEHICLE_FOCUS_ZOOM = 14

type MapViewControllerProps = {
  vehicleId?: string
  positions?: GpsPosition[]
  fallbackCenter: [number, number]
  fallbackZoom: number
}

// react-leaflet's <MapContainer center/zoom> props only apply on initial mount — moving the
// view afterward requires the imperative Leaflet map instance (useMap), driven from an effect.
export function MapViewController({ vehicleId, positions, fallbackCenter, fallbackZoom }: MapViewControllerProps) {
  const map = useMap()
  const centeredVehicleId = useRef<string | undefined>(undefined)

  useEffect(() => {
    if (vehicleId == null) {
      if (centeredVehicleId.current != null) {
        map.flyTo(fallbackCenter, fallbackZoom)
      }
      centeredVehicleId.current = undefined
      return
    }

    if (vehicleId === centeredVehicleId.current) {
      return
    }

    const position = positions?.find((candidate) => candidate.vehicleId === vehicleId)
    if (position) {
      map.flyTo([position.latitude, position.longitude], VEHICLE_FOCUS_ZOOM)
      centeredVehicleId.current = vehicleId
    }
  }, [vehicleId, positions, map, fallbackCenter, fallbackZoom])

  return null
}
