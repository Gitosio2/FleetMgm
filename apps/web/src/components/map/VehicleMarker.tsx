import { useMemo } from 'react'
import { renderToStaticMarkup } from 'react-dom/server'
import L from 'leaflet'
import { Car, Tractor, Truck, type LucideIcon } from 'lucide-react'
import { Marker } from 'react-leaflet'
import type { GpsPosition, VehicleCategory } from '@fleetmgm/api'
import { VehiclePopover } from './VehiclePopover'

const CATEGORY_ICON: Record<VehicleCategory, LucideIcon> = {
  LIGHT_VEHICLE: Car,
  HEAVY_VEHICLE: Truck,
  HEAVY_MACHINERY: Tractor,
}

function buildDivIcon(vehicleId: string, category: VehicleCategory) {
  const IconComponent = CATEGORY_ICON[category]
  // No `color` prop — lucide-react defaults to stroke="currentColor", which then follows the
  // wrapping div's `text-on-tertiary` class instead of being hardcoded here.
  const iconMarkup = renderToStaticMarkup(<IconComponent size={24} />)

  return L.divIcon({
    html: `<div data-testid="vehicle-marker-${vehicleId}" class="flex size-10 items-center justify-center rounded-full bg-tertiary text-on-tertiary shadow-md">${iconMarkup}</div>`,
    className: '',
    iconSize: [40, 40],
    iconAnchor: [20, 20],
    popupAnchor: [0, -20],
  })
}

type VehicleMarkerProps = {
  position: GpsPosition
}

export function VehicleMarker({ position }: VehicleMarkerProps) {
  // Memoized so a re-render that doesn't actually change the vehicle (e.g. a sibling query
  // settling, or the 10s GPS poll refreshing lat/lng) doesn't rebuild the divIcon — a new icon
  // instance makes react-leaflet call Leaflet's setIcon(), which destroys and recreates the
  // marker's DOM node instead of just moving it.
  const icon = useMemo(
    () => buildDivIcon(position.vehicleId, position.vehicleCategory),
    [position.vehicleId, position.vehicleCategory],
  )

  return (
    <Marker position={[position.latitude, position.longitude]} icon={icon}>
      <VehiclePopover position={position} />
    </Marker>
  )
}
