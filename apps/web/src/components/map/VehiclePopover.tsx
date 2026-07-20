import { Popup } from 'react-leaflet'
import type { GpsPosition } from '@fleetmgm/api'
import { useVehicleAssignment } from '@fleetmgm/hooks'
import { formatVehicleLabel } from '@/lib/vehicle-label'
import { CATEGORY_ICON } from '@/lib/vehicle-category-icon'

type VehiclePopoverProps = {
  position: GpsPosition
  open: boolean
}

// "Marta Ruiz" -> "MR" — no existing initials helper in the codebase; kept local since this is
// the only consumer.
function initials(name: string): string {
  return name
    .split(' ')
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0]!.toUpperCase())
    .join('')
}

export function VehiclePopover({ position, open }: VehiclePopoverProps) {
  const vehicleLabel = formatVehicleLabel({
    vehicleLicensePlate: position.licensePlate,
    vehicleMake: position.vehicleMake,
    vehicleModel: position.vehicleModel,
  })
  const vehicleMakeModel = [position.vehicleMake, position.vehicleModel].filter(Boolean).join(' ')
  // Only shown alongside the plate — for plateless vehicles, vehicleLabel already IS make/model.
  const showMakeModel = position.licensePlate != null && vehicleMakeModel.length > 0

  // Gated behind `open` — see VehicleMarker's comment on why an unconditional call here would
  // fire one lookup per marker on every map load instead of only when this popup is clicked.
  const { data: assignment } = useVehicleAssignment(open ? position.vehicleId : '')

  const Icon = CATEGORY_ICON[position.vehicleCategory]

  return (
    <Popup>
      <div
        data-testid="vehicle-popover"
        className="flex w-56 max-w-[calc(100vw-2rem)] flex-col gap-2 rounded-2xl bg-surface-bright p-3.5 text-sm text-on-surface shadow-lg"
      >
        <div className="flex items-center justify-between gap-2">
          <div className="flex items-center gap-2.5">
            <span className="flex size-11 shrink-0 items-center justify-center rounded-xl bg-tertiary text-on-tertiary">
              <Icon size={22} />
            </span>
            <span className="font-semibold text-on-surface">{vehicleLabel}</span>
          </div>
          <span className="inline-flex shrink-0 items-baseline gap-1 rounded-full bg-tertiary px-3 py-1 font-bold text-on-tertiary">
            {Math.round(position.speed ?? 0)}
            <span className="text-[10px] font-medium opacity-85">km/h</span>
          </span>
        </div>

        {showMakeModel && <span className="text-on-surface-variant">{vehicleMakeModel}</span>}

        <div className="flex items-center gap-2 border-t border-outline-variant pt-2">
          {assignment ? (
            <>
              <span className="flex size-7 shrink-0 items-center justify-center rounded-full bg-tertiary text-xs font-bold text-on-tertiary">
                {initials(assignment.driverName)}
              </span>
              <span className="font-medium text-on-surface">{assignment.driverName}</span>
            </>
          ) : (
            <span className="italic text-on-surface-variant">Sin conductor asignado</span>
          )}
        </div>
      </div>
    </Popup>
  )
}
