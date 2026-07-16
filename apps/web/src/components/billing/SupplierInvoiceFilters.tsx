import { useState } from 'react'
import { ChevronDown, Plus } from 'lucide-react'
import type { ExpenseCategory, Supplier, SupplierInvoiceStatus, Vehicle } from '@fleetmgm/api'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader } from '@/components/ui/card'
import { Collapsible, CollapsibleContent, CollapsibleTrigger } from '@/components/ui/collapsible'
import { EXPENSE_CATEGORY_LABEL, STATUS_LABEL } from './supplier-invoice-shared'

const selectClassName =
  'flex h-9 rounded-lg border border-outline-variant bg-surface-container-lowest px-3 text-sm text-on-surface focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-secondary-container'

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
  const [filtersOpen, setFiltersOpen] = useState(true)

  function toggleFilters() {
    setFiltersOpen((open) => !open)
  }

  return (
    <Card>
      <Collapsible open={filtersOpen} onOpenChange={setFiltersOpen}>
        <CardHeader className="flex-row items-center justify-between gap-4">
          <div
            role="button"
            tabIndex={0}
            className="cursor-pointer"
            onClick={toggleFilters}
            onKeyDown={(e) => {
              if (e.key === 'Enter' || e.key === ' ') {
                e.preventDefault()
                toggleFilters()
              }
            }}
          >
            <h1 className="font-display text-2xl font-semibold">Facturas de proveedor</h1>
            <p className="text-on-surface-variant">Gestiona los gastos operativos y sus proveedores.</p>
          </div>
          <div className="flex items-center gap-2">
            <Button onClick={onCreate}>
              <Plus className="size-4" />
              Nueva factura de proveedor
            </Button>
            <CollapsibleTrigger asChild>
              <Button variant="outline" size="sm" className="group" aria-label="Mostrar u ocultar filtros">
                <ChevronDown className="size-4 transition-transform duration-200 group-data-[state=open]:rotate-180" />
              </Button>
            </CollapsibleTrigger>
          </div>
        </CardHeader>
        <CollapsibleContent>
          <CardContent>
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
              <div className="flex flex-col gap-1 text-sm text-on-surface-variant">
                <span>Proveedor</span>
                <select
                  aria-label="Filtrar por proveedor"
                  className={selectClassName}
                  value={supplierId}
                  onChange={(e) => onSupplierIdChange(e.target.value)}
                >
                  <option value="">Todos los proveedores</option>
                  {suppliers.map((supplier) => (
                    <option key={supplier.id} value={supplier.id}>
                      {supplier.name}
                    </option>
                  ))}
                </select>
              </div>

              <div className="flex flex-col gap-1 text-sm text-on-surface-variant">
                <span>Categoría</span>
                <select
                  aria-label="Filtrar por categoría"
                  className={selectClassName}
                  value={category}
                  onChange={(e) => onCategoryChange(e.target.value as ExpenseCategory | '')}
                >
                  <option value="">Todas las categorías</option>
                  {(Object.keys(EXPENSE_CATEGORY_LABEL) as ExpenseCategory[]).map((value) => (
                    <option key={value} value={value}>
                      {EXPENSE_CATEGORY_LABEL[value]}
                    </option>
                  ))}
                </select>
              </div>

              <div className="flex flex-col gap-1 text-sm text-on-surface-variant">
                <span>Vehículo</span>
                <select
                  aria-label="Filtrar por vehículo"
                  className={selectClassName}
                  value={vehicleId}
                  onChange={(e) => onVehicleIdChange(e.target.value)}
                >
                  <option value="">Todos los vehículos</option>
                  {vehicles.map((vehicle) => (
                    <option key={vehicle.id} value={vehicle.id}>
                      {vehicleLabel(vehicle)}
                    </option>
                  ))}
                </select>
              </div>

              <div className="flex flex-col gap-1 text-sm text-on-surface-variant">
                <span>Estado</span>
                <select
                  aria-label="Filtrar por estado"
                  className={selectClassName}
                  value={status}
                  onChange={(e) => onStatusChange(e.target.value as SupplierInvoiceStatus | '')}
                >
                  <option value="">Todos los estados</option>
                  {(Object.keys(STATUS_LABEL) as SupplierInvoiceStatus[]).map((value) => (
                    <option key={value} value={value}>
                      {STATUS_LABEL[value]}
                    </option>
                  ))}
                </select>
              </div>

              <div className="flex flex-col gap-1 text-sm text-on-surface-variant">
                <span>Fecha desde</span>
                <input
                  aria-label="Fecha desde"
                  type="date"
                  className={selectClassName}
                  value={invoiceDateFrom}
                  onChange={(e) => onInvoiceDateFromChange(e.target.value)}
                />
              </div>

              <div className="flex flex-col gap-1 text-sm text-on-surface-variant">
                <span>Fecha hasta</span>
                <input
                  aria-label="Fecha hasta"
                  type="date"
                  className={selectClassName}
                  value={invoiceDateTo}
                  onChange={(e) => onInvoiceDateToChange(e.target.value)}
                />
              </div>

              <div className="flex flex-col gap-1 text-sm text-on-surface-variant">
                <span>Vencimiento desde</span>
                <input
                  aria-label="Vencimiento desde"
                  type="date"
                  className={selectClassName}
                  value={dueDateFrom}
                  onChange={(e) => onDueDateFromChange(e.target.value)}
                />
              </div>

              <div className="flex flex-col gap-1 text-sm text-on-surface-variant">
                <span>Vencimiento hasta</span>
                <input
                  aria-label="Vencimiento hasta"
                  type="date"
                  className={selectClassName}
                  value={dueDateTo}
                  onChange={(e) => onDueDateToChange(e.target.value)}
                />
              </div>

              <div className="flex flex-col gap-1 text-sm text-on-surface-variant">
                <span>Total mínimo</span>
                <input
                  aria-label="Total mínimo"
                  type="number"
                  step="0.01"
                  min="0"
                  className={selectClassName}
                  value={totalMin}
                  onChange={(e) => onTotalMinChange(e.target.value)}
                />
              </div>

              <div className="flex flex-col gap-1 text-sm text-on-surface-variant">
                <span>Total máximo</span>
                <input
                  aria-label="Total máximo"
                  type="number"
                  step="0.01"
                  min="0"
                  className={selectClassName}
                  value={totalMax}
                  onChange={(e) => onTotalMaxChange(e.target.value)}
                />
              </div>
            </div>
          </CardContent>
        </CollapsibleContent>
      </Collapsible>
    </Card>
  )
}
