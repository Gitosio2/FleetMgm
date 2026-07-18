import { FilterBar, type FilterField } from '@/components/billing/FilterBar'

type ClientFiltersProps = {
  taxId: string
  onTaxIdChange: (value: string) => void
  name: string
  onNameChange: (value: string) => void
  onCreate: () => void
  canCreate: boolean
}

export function ClientFilters({
  taxId,
  onTaxIdChange,
  name,
  onNameChange,
  onCreate,
  canCreate,
}: ClientFiltersProps) {
  const fields: FilterField[] = [
    {
      type: 'text',
      key: 'taxId',
      label: 'ID fiscal',
      ariaLabel: 'Buscar por ID fiscal',
      value: taxId,
      onChange: onTaxIdChange,
      placeholder: 'B12345678',
    },
    {
      type: 'text',
      key: 'name',
      label: 'Nombre',
      ariaLabel: 'Buscar por nombre',
      value: name,
      onChange: onNameChange,
      placeholder: 'Nombre del cliente',
    },
  ]

  return (
    <FilterBar
      title="Clientes"
      description="Gestiona los clientes facturados a través de trabajos completados."
      createLabel="Nuevo cliente"
      onCreate={onCreate}
      fields={fields}
      canCreate={canCreate}
    />
  )
}
