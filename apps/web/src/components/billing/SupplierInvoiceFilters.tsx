import type { ExpenseCategory, Supplier, SupplierInvoiceStatus, Vehicle } from '@fleetmgm/api'
import { FilterBar, type FilterField } from './FilterBar'
import { EXPENSE_CATEGORY_LABEL, STATUS_LABEL } from './supplier-invoice-shared'

type SupplierInvoiceFiltersProps = {
  supplierId: string
  onSupplierIdChange: (value: string) => void
  category: ExpenseCategory | ''
  onCategoryChange: (value: ExpenseCategory | '') => void
  vehicleId: string
  onVehicleIdChange: (value: string) => void
  status: SupplierInvoiceStatus | ''
  onStatusChange: (value: SupplierInvoiceStatus | '') => void
  invoiceDateFrom: string
  onInvoiceDateFromChange: (value: string) => void
  invoiceDateTo: string
  onInvoiceDateToChange: (value: string) => void
  dueDateFrom: string
  onDueDateFromChange: (value: string) => void
  dueDateTo: string
  onDueDateToChange: (value: string) => void
  totalMin: string
  onTotalMinChange: (value: string) => void
  totalMax: string
  onTotalMaxChange: (value: string) => void
  suppliers: Supplier[]
  vehicles: Vehicle[]
  onCreate: () => void
}

function vehicleLabel(vehicle: Vehicle) {
  return `${vehicle.make} ${vehicle.model}${vehicle.licensePlate ? ` - ${vehicle.licensePlate}` : ''}`
}

export function SupplierInvoiceFilters({
  supplierId,
  onSupplierIdChange,
  category,
  onCategoryChange,
  vehicleId,
  onVehicleIdChange,
  status,
  onStatusChange,
  invoiceDateFrom,
  onInvoiceDateFromChange,
  invoiceDateTo,
  onInvoiceDateToChange,
  dueDateFrom,
  onDueDateFromChange,
  dueDateTo,
  onDueDateToChange,
  totalMin,
  onTotalMinChange,
  totalMax,
  onTotalMaxChange,
  suppliers,
  vehicles,
  onCreate,
}: SupplierInvoiceFiltersProps) {
  const fields: FilterField[] = [
    {
      type: 'select',
      key: 'supplierId',
      label: 'Proveedor',
      ariaLabel: 'Filtrar por proveedor',
      value: supplierId,
      onChange: onSupplierIdChange,
      placeholder: 'Todos los proveedores',
      options: suppliers.map((supplier) => ({ value: supplier.id, label: supplier.name })),
    },
    {
      type: 'select',
      key: 'category',
      label: 'Categoría',
      ariaLabel: 'Filtrar por categoría',
      value: category,
      onChange: (value) => onCategoryChange(value as ExpenseCategory | ''),
      placeholder: 'Todas las categorías',
      options: (Object.keys(EXPENSE_CATEGORY_LABEL) as ExpenseCategory[]).map((value) => ({
        value,
        label: EXPENSE_CATEGORY_LABEL[value],
      })),
    },
    {
      type: 'select',
      key: 'vehicleId',
      label: 'Vehículo',
      ariaLabel: 'Filtrar por vehículo',
      value: vehicleId,
      onChange: onVehicleIdChange,
      placeholder: 'Todos los vehículos',
      options: vehicles.map((vehicle) => ({ value: vehicle.id, label: vehicleLabel(vehicle) })),
    },
    {
      type: 'select',
      key: 'status',
      label: 'Estado',
      ariaLabel: 'Filtrar por estado',
      value: status,
      onChange: (value) => onStatusChange(value as SupplierInvoiceStatus | ''),
      placeholder: 'Todos los estados',
      options: (Object.keys(STATUS_LABEL) as SupplierInvoiceStatus[]).map((value) => ({
        value,
        label: STATUS_LABEL[value],
      })),
    },
    {
      type: 'date',
      key: 'invoiceDateFrom',
      label: 'Fecha desde',
      ariaLabel: 'Fecha desde',
      value: invoiceDateFrom,
      onChange: onInvoiceDateFromChange,
    },
    {
      type: 'date',
      key: 'invoiceDateTo',
      label: 'Fecha hasta',
      ariaLabel: 'Fecha hasta',
      value: invoiceDateTo,
      onChange: onInvoiceDateToChange,
    },
    {
      type: 'date',
      key: 'dueDateFrom',
      label: 'Vencimiento desde',
      ariaLabel: 'Vencimiento desde',
      value: dueDateFrom,
      onChange: onDueDateFromChange,
    },
    {
      type: 'date',
      key: 'dueDateTo',
      label: 'Vencimiento hasta',
      ariaLabel: 'Vencimiento hasta',
      value: dueDateTo,
      onChange: onDueDateToChange,
    },
    {
      type: 'number',
      key: 'totalMin',
      label: 'Total mínimo',
      ariaLabel: 'Total mínimo',
      value: totalMin,
      onChange: onTotalMinChange,
    },
    {
      type: 'number',
      key: 'totalMax',
      label: 'Total máximo',
      ariaLabel: 'Total máximo',
      value: totalMax,
      onChange: onTotalMaxChange,
    },
  ]

  return (
    <FilterBar
      title="Facturas de proveedor"
      description="Gestiona los gastos operativos y sus proveedores."
      createLabel="Nueva factura de proveedor"
      onCreate={onCreate}
      fields={fields}
    />
  )
}
