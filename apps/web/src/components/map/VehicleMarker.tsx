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
  const iconMarkup = renderToStaticMarkup(<IconComponent size={16} />)

  return L.divIcon({
    html: `<div data-testid="vehicle-marker-${vehicleId}" class="flex size-7 items-center justify-center rounded-full bg-tertiary text-on-tertiary shadow-md">${iconMarkup}</div>`,
    className: '',
    iconSize: [28, 28],
    iconAnchor: [14, 14],
    popupAnchor: [0, -14],
  })
}

type VehicleMarkerProps = {
  position: GpsPosition
}

export function VehicleMarker({ position }: VehicleMarkerProps) {
  const icon = buildDivIcon(position.vehicleId, position.vehicleCategory)

  return (
    <Marker position={[position.latitude, position.longitude]} icon={icon}>
      <VehiclePopover position={position} />
    </Marker>
  )
}
