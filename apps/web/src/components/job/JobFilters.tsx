import type { JobStatus, Vehicle, Worker } from '@fleetmgm/api'
import { FilterBar, type FilterField } from '@/components/billing/FilterBar'
import { formatVehicleSelectLabel } from '@/lib/vehicle-label'
import { JOB_STATUSES, JOB_STATUS_LABEL } from './job-shared'

type JobFiltersProps = {
  title: string
  onTitleChange: (value: string) => void
  originLocation: string
  onOriginLocationChange: (value: string) => void
  destinationLocation: string
  onDestinationLocationChange: (value: string) => void
  vehicleId: string
  onVehicleIdChange: (value: string) => void
  vehicles: Vehicle[]
  assignedDriverId: string
  onAssignedDriverIdChange: (value: string) => void
  drivers: Worker[]
  status: JobStatus | ''
  onStatusChange: (value: JobStatus | '') => void
  actualStartFrom: string
  onActualStartFromChange: (value: string) => void
  actualStartTo: string
  onActualStartToChange: (value: string) => void
  actualEndFrom: string
  onActualEndFromChange: (value: string) => void
  actualEndTo: string
  onActualEndToChange: (value: string) => void
  onCreate: () => void
  canCreate: boolean
}

export function JobFilters({
  title,
  onTitleChange,
  originLocation,
  onOriginLocationChange,
  destinationLocation,
  onDestinationLocationChange,
  vehicleId,
  onVehicleIdChange,
  vehicles,
  assignedDriverId,
  onAssignedDriverIdChange,
  drivers,
  status,
  onStatusChange,
  actualStartFrom,
  onActualStartFromChange,
  actualStartTo,
  onActualStartToChange,
  actualEndFrom,
  onActualEndFromChange,
  actualEndTo,
  onActualEndToChange,
  onCreate,
  canCreate,
}: JobFiltersProps) {
  const fields: FilterField[] = [
    {
      type: 'text',
      key: 'title',
      label: 'Título',
      ariaLabel: 'Buscar por título',
      value: title,
      onChange: onTitleChange,
      placeholder: 'Título del trabajo',
    },
    {
      type: 'text',
      key: 'originLocation',
      label: 'Origen',
      ariaLabel: 'Buscar por origen',
      value: originLocation,
      onChange: onOriginLocationChange,
      placeholder: 'Ubicación de origen',
    },
    {
      type: 'text',
      key: 'destinationLocation',
      label: 'Destino',
      ariaLabel: 'Buscar por destino',
      value: destinationLocation,
      onChange: onDestinationLocationChange,
      placeholder: 'Ubicación de destino',
    },
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
      type: 'select',
      key: 'assignedDriverId',
      label: 'Conductor',
      ariaLabel: 'Filtrar por conductor',
      value: assignedDriverId,
      onChange: onAssignedDriverIdChange,
      placeholder: 'Todos los conductores',
      options: drivers.map((driver) => ({ value: driver.id, label: driver.fullName })),
    },
    {
      type: 'select',
      key: 'status',
      label: 'Estado',
      ariaLabel: 'Filtrar por estado',
      value: status,
      onChange: (value) => onStatusChange(value as JobStatus | ''),
      placeholder: 'Todos los estados',
      options: JOB_STATUSES.map((value) => ({ value, label: JOB_STATUS_LABEL[value] })),
    },
    {
      type: 'date',
      key: 'actualStartFrom',
      label: 'Inicio desde',
      ariaLabel: 'Inicio desde',
      value: actualStartFrom,
      onChange: onActualStartFromChange,
    },
    {
      type: 'date',
      key: 'actualStartTo',
      label: 'Inicio hasta',
      ariaLabel: 'Inicio hasta',
      value: actualStartTo,
      onChange: onActualStartToChange,
    },
    {
      type: 'date',
      key: 'actualEndFrom',
      label: 'Fin desde',
      ariaLabel: 'Fin desde',
      value: actualEndFrom,
      onChange: onActualEndFromChange,
    },
    {
      type: 'date',
      key: 'actualEndTo',
      label: 'Fin hasta',
      ariaLabel: 'Fin hasta',
      value: actualEndTo,
      onChange: onActualEndToChange,
    },
  ]

  return (
    <FilterBar
      title="Trabajos"
      description="Gestiona los trabajos y su ciclo de vida."
      createLabel="Nuevo trabajo"
      onCreate={onCreate}
      fields={fields}
      canCreate={canCreate}
    />
  )
}
