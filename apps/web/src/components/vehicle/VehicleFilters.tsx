import type { VehicleCategory, VehicleStatus } from '@fleetmgm/api'
import { FilterBar, type FilterField } from '@/components/billing/FilterBar'
import { VEHICLE_CATEGORIES, VEHICLE_CATEGORY_LABEL } from '@/lib/vehicle-category-label'
import { VEHICLE_STATUSES, VEHICLE_STATUS_LABEL } from '@/lib/vehicle-status-label'

type VehicleFiltersProps = {
  category: VehicleCategory | ''
  onCategoryChange: (value: VehicleCategory | '') => void
  status: VehicleStatus | ''
  onStatusChange: (value: VehicleStatus | '') => void
  licensePlate: string
  onLicensePlateChange: (value: string) => void
  vehicle: string
  onVehicleChange: (value: string) => void
  onCreate: () => void
  canCreate: boolean
}

export function VehicleFilters({
  category,
  onCategoryChange,
  status,
  onStatusChange,
  licensePlate,
  onLicensePlateChange,
  vehicle,
  onVehicleChange,
  onCreate,
  canCreate,
}: VehicleFiltersProps) {
  const fields: FilterField[] = [
    {
      type: 'select',
      key: 'category',
      label: 'Tipo',
      ariaLabel: 'Filtrar por tipo',
      value: category,
      onChange: (value) => onCategoryChange(value as VehicleCategory | ''),
      placeholder: 'Todos los tipos',
      options: VEHICLE_CATEGORIES.map((value) => ({ value, label: VEHICLE_CATEGORY_LABEL[value] })),
    },
    {
      type: 'select',
      key: 'status',
      label: 'Estado',
      ariaLabel: 'Filtrar por estado',
      value: status,
      onChange: (value) => onStatusChange(value as VehicleStatus | ''),
      placeholder: 'Todos los estados',
      options: VEHICLE_STATUSES.map((value) => ({ value, label: VEHICLE_STATUS_LABEL[value] })),
    },
    {
      type: 'text',
      key: 'licensePlate',
      label: 'Matrícula',
      ariaLabel: 'Buscar por matrícula',
      value: licensePlate,
      onChange: onLicensePlateChange,
      placeholder: '1234ABC',
    },
    {
      type: 'text',
      key: 'vehicle',
      label: 'Vehículo',
      ariaLabel: 'Buscar por vehículo',
      value: vehicle,
      onChange: onVehicleChange,
      placeholder: 'Marca o modelo',
    },
  ]

  return (
    <FilterBar
      title="Vehículos"
      description="Gestiona los vehículos y maquinaria pesada de la flota."
      createLabel="Nuevo vehículo"
      onCreate={onCreate}
      fields={fields}
      canCreate={canCreate}
    />
  )
}
