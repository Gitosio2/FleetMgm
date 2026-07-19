import type { MaintenanceCategory, MaintenanceStatus, Vehicle, Worker } from '@fleetmgm/api'
import { FilterBar, type FilterField } from '@/components/billing/FilterBar'
import { formatVehicleSelectLabel } from '@/lib/vehicle-label'
import { CATEGORY_LABEL, MAINTENANCE_STATUSES, STATUS_LABEL } from './form-shared'

const CATEGORIES: MaintenanceCategory[] = ['PREVENTIVE', 'CORRECTIVE']

type MaintenanceFiltersProps = {
  vehicleId: string
  onVehicleIdChange: (value: string) => void
  vehicles: Vehicle[]
  type: string
  onTypeChange: (value: string) => void
  category: MaintenanceCategory | ''
  onCategoryChange: (value: MaintenanceCategory | '') => void
  status: MaintenanceStatus | ''
  onStatusChange: (value: MaintenanceStatus | '') => void
  technicianId: string
  onTechnicianIdChange: (value: string) => void
  technicians: Worker[]
  costFrom: string
  onCostFromChange: (value: string) => void
  costTo: string
  onCostToChange: (value: string) => void
}

export function MaintenanceFilters({
  vehicleId,
  onVehicleIdChange,
  vehicles,
  type,
  onTypeChange,
  category,
  onCategoryChange,
  status,
  onStatusChange,
  technicianId,
  onTechnicianIdChange,
  technicians,
  costFrom,
  onCostFromChange,
  costTo,
  onCostToChange,
}: MaintenanceFiltersProps) {
  const fields: FilterField[] = [
    {
      type: 'select',
      key: 'vehicleId',
      label: 'Vehículo',
      ariaLabel: 'Filtrar por vehículo',
      value: vehicleId,
      onChange: onVehicleIdChange,
      placeholder: 'Todos los vehículos',
      options: vehicles.map((vehicle) => ({ value: vehicle.id, label: formatVehicleSelectLabel(vehicle) })),
    },
    {
      type: 'text',
      key: 'type',
      label: 'Tipo',
      ariaLabel: 'Buscar por tipo',
      value: type,
      onChange: onTypeChange,
      placeholder: 'Cambio de aceite',
    },
    {
      type: 'select',
      key: 'category',
      label: 'Categoría',
      ariaLabel: 'Filtrar por categoría',
      value: category,
      onChange: (value) => onCategoryChange(value as MaintenanceCategory | ''),
      placeholder: 'Todas las categorías',
      options: CATEGORIES.map((value) => ({ value, label: CATEGORY_LABEL[value] })),
    },
    {
      type: 'select',
      key: 'status',
      label: 'Estado',
      ariaLabel: 'Filtrar por estado',
      value: status,
      onChange: (value) => onStatusChange(value as MaintenanceStatus | ''),
      placeholder: 'Todos los estados',
      options: MAINTENANCE_STATUSES.map((value) => ({ value, label: STATUS_LABEL[value] })),
    },
    {
      type: 'select',
      key: 'technicianId',
      label: 'Técnico',
      ariaLabel: 'Filtrar por técnico',
      value: technicianId,
      onChange: onTechnicianIdChange,
      placeholder: 'Todos los técnicos',
      options: technicians.map((technician) => ({ value: technician.id, label: technician.fullName })),
    },
    {
      type: 'number',
      key: 'costFrom',
      label: 'Importe desde',
      ariaLabel: 'Importe desde',
      value: costFrom,
      onChange: onCostFromChange,
    },
    {
      type: 'number',
      key: 'costTo',
      label: 'Importe hasta',
      ariaLabel: 'Importe hasta',
      value: costTo,
      onChange: onCostToChange,
    },
  ]

  return (
    <FilterBar
      title="Órdenes de mantenimiento"
      description="Registro de trabajos realizados por vehículo — consulta de estado, categoría y coste."
      createLabel=""
      onCreate={() => {}}
      fields={fields}
      canCreate={false}
    />
  )
}
