import { FilterBar, type FilterField } from '@/components/billing/FilterBar'

type SupplierFiltersProps = {
  taxId: string
  onTaxIdChange: (value: string) => void
  name: string
  onNameChange: (value: string) => void
  onCreate: () => void
  canCreate: boolean
}

export function SupplierFilters({
  taxId,
  onTaxIdChange,
  name,
  onNameChange,
  onCreate,
  canCreate,
}: SupplierFiltersProps) {
  const fields: FilterField[] = [
    {
      type: 'text',
      key: 'taxId',
      label: 'NIF',
      ariaLabel: 'Buscar por NIF',
      value: taxId,
      onChange: onTaxIdChange,
      placeholder: 'B11122233',
    },
    {
      type: 'text',
      key: 'name',
      label: 'Nombre',
      ariaLabel: 'Buscar por nombre',
      value: name,
      onChange: onNameChange,
      placeholder: 'Nombre del proveedor',
    },
  ]

  return (
    <FilterBar
      title="Proveedores"
      description="Gestiona los proveedores a los que se les asocian gastos y facturas."
      createLabel="Nuevo proveedor"
      onCreate={onCreate}
      fields={fields}
      canCreate={canCreate}
    />
  )
}
