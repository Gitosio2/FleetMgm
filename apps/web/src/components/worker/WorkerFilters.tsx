import type { WorkerRole } from '@fleetmgm/api'
import { FilterBar, type FilterField } from '@/components/billing/FilterBar'
import { WORKER_ROLES, WORKER_ROLE_LABEL } from './worker-shared'

type WorkerFiltersProps = {
  name: string
  onNameChange: (value: string) => void
  nationalId: string
  onNationalIdChange: (value: string) => void
  workerRole: WorkerRole | ''
  onWorkerRoleChange: (value: WorkerRole | '') => void
  onCreate: () => void
  canCreate: boolean
}

export function WorkerFilters({
  name,
  onNameChange,
  nationalId,
  onNationalIdChange,
  workerRole,
  onWorkerRoleChange,
  onCreate,
  canCreate,
}: WorkerFiltersProps) {
  const fields: FilterField[] = [
    {
      type: 'text',
      key: 'name',
      label: 'Nombre',
      ariaLabel: 'Buscar por nombre',
      value: name,
      onChange: onNameChange,
      placeholder: 'Nombre del trabajador',
    },
    {
      type: 'text',
      key: 'nationalId',
      label: 'Documento',
      ariaLabel: 'Buscar por documento',
      value: nationalId,
      onChange: onNationalIdChange,
      placeholder: '12345678A',
    },
    {
      type: 'select',
      key: 'workerRole',
      label: 'Rol',
      ariaLabel: 'Filtrar por rol',
      value: workerRole,
      onChange: (value) => onWorkerRoleChange(value as WorkerRole | ''),
      placeholder: 'Todos los roles',
      options: WORKER_ROLES.map((role) => ({ value: role, label: WORKER_ROLE_LABEL[role] })),
    },
  ]

  return (
    <FilterBar
      title="Trabajadores"
      description="Gestiona los conductores y técnicos de la flota."
      createLabel="Nuevo trabajador"
      onCreate={onCreate}
      fields={fields}
      canCreate={canCreate}
    />
  )
}
