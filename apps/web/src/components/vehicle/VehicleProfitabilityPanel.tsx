import { useState } from 'react'
import {
  useVehicleMaintenanceHistory,
  useVehicleProfitability,
  useVehicleRevenue,
  useVehicleSupplierExpenses,
} from '@fleetmgm/hooks'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { formatCurrency } from '@/lib/currency'

// Merged "Historial de gastos" row shape (Hito 45) — maintenance records and supplier-invoice cost
// sources normalized to the same {description, date, amount} shape so they can be sorted into one
// date-descending list. `date` is nullable because a scheduled maintenance record may have no
// workshopEntryDate yet; those rows sort to the end rather than crashing the comparator.
type ExpenseRow = {
  key: string
  description: string
  date: string | null
  amount: number
}

function sortByDateDescending(a: ExpenseRow, b: ExpenseRow): number {
  if (!a.date && !b.date) return 0
  if (!a.date) return 1
  if (!b.date) return -1
  return b.date.localeCompare(a.date)
}

// Margen and Beneficio/km cards — a strong visual flag for negative profitability, not the same
// lightweight text-only treatment as FinancialSummary's month-over-month margin comparison.
// Beneficio/km can be null ("Sin datos suficientes"), unlike margin — null stays uncolored.
function signCardClassName(value: number | null): string {
  if (value == null) return ''
  if (value < 0) return 'bg-error-container text-error'
  if (value > 0) return 'bg-success/15 text-success'
  return ''
}

// Same inline Desde/Hasta pattern as AuditLogFilters.tsx — visible "Desde"/"Hasta" label text
// wrapping an aria-labelled date input.
const inputClassName =
  'flex h-9 rounded-lg border border-outline-variant bg-surface-container-lowest px-3 text-sm text-on-surface focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-secondary-container'

type VehicleProfitabilityPanelProps = {
  vehicleId: string
  vehicleLabel: string
}

export function VehicleProfitabilityPanel({ vehicleId, vehicleLabel }: VehicleProfitabilityPanelProps) {
  // Optional Desde/Hasta range — empty string means unbounded, so all three sections default to
  // full history, matching this app-wide filter convention (Jobs/Invoices/Maintenance-Orders/
  // Vehicles/Workers/Suppliers/AuditLog).
  const [from, setFrom] = useState('')
  const [to, setTo] = useState('')

  const { data: profitability, isLoading, isError } = useVehicleProfitability(
    vehicleId,
    from || undefined,
    to || undefined,
  )
  const { data: maintenanceHistory } = useVehicleMaintenanceHistory(vehicleId, from || undefined, to || undefined)
  const { data: revenueHistory } = useVehicleRevenue(vehicleId, from || undefined, to || undefined)
  const { data: supplierExpenses } = useVehicleSupplierExpenses(vehicleId, from || undefined, to || undefined)

  const maintenanceTotal = (maintenanceHistory ?? []).reduce((sum, record) => sum + (record.cost ?? 0), 0)
  const supplierExpensesTotal = (supplierExpenses ?? []).reduce((sum, expense) => sum + expense.amount, 0)
  const revenueTotal = (revenueHistory ?? []).reduce((sum, item) => sum + item.subtotal, 0)
  const usageUnitLabel = profitability?.usageMeasure === 'HOURS' ? 'hora' : 'km'

  // Single merged, date-sorted-descending list (user's explicit choice over two separate
  // sub-lists) — mirrors ProfitabilityService.getExpensesByVehicle's Stream.concat + sort. Per-item
  // type-specific detail (e.g. the maintenance status badge) is intentionally dropped in the merged
  // view — an accepted tradeoff.
  const expenseRows: ExpenseRow[] = [
    ...(maintenanceHistory ?? []).map((record) => ({
      key: `maintenance-${record.id}`,
      description: record.type,
      date: record.workshopEntryDate,
      amount: record.cost ?? 0,
    })),
    ...(supplierExpenses ?? []).map((expense, index) => ({
      key: `supplier-expense-${index}-${expense.description}`,
      description: expense.description,
      date: expense.date,
      amount: expense.amount,
    })),
  ].sort(sortByDateDescending)

  // The Desde/Hasta filter bar renders unconditionally, outside the loading/error gate below —
  // useVehicleProfitability's query key now includes from/to, so picking a new range briefly makes
  // isLoading true again (no cached data yet for that key). Gating the whole return on isLoading
  // would unmount the filter inputs themselves mid-interaction; only the data-dependent sections
  // below the filter bar should show the loading/error placeholder.
  return (
    <div className="flex flex-col gap-4">
      <div className="flex flex-wrap items-center gap-3">
        <label className="flex items-center gap-2 text-sm text-on-surface-variant">
          Desde
          <input
            aria-label="Desde"
            type="date"
            className={inputClassName}
            value={from}
            onChange={(e) => setFrom(e.target.value)}
          />
        </label>
        <label className="flex items-center gap-2 text-sm text-on-surface-variant">
          Hasta
          <input
            aria-label="Hasta"
            type="date"
            className={inputClassName}
            value={to}
            onChange={(e) => setTo(e.target.value)}
          />
        </label>
      </div>

      {isLoading ? (
        <p className="text-on-surface-variant">Cargando rentabilidad…</p>
      ) : isError || !profitability ? (
        <p className="text-on-surface-variant">No se pudo cargar la rentabilidad de {vehicleLabel}.</p>
      ) : (
        <>
          <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
            <Card>
              <CardHeader>
                <CardTitle className="text-sm font-medium text-on-surface-variant">
                  Historial de gastos ({formatCurrency(maintenanceTotal + supplierExpensesTotal)})
                </CardTitle>
              </CardHeader>
              <CardContent>
                {expenseRows.length === 0 ? (
                  <p className="text-sm text-on-surface-variant">Sin gastos en este periodo</p>
                ) : (
                  <ul className="flex max-h-72 flex-col gap-2 overflow-y-auto pr-1">
                    {expenseRows.map((expense) => (
                      <li key={expense.key} className="flex items-center justify-between gap-2 text-sm">
                        <div className="flex min-w-0 flex-col">
                          <span className="truncate font-medium">{expense.description}</span>
                          <span className="text-on-surface-variant">
                            {expense.date ? new Date(expense.date).toLocaleDateString('es-ES') : 'Sin fecha'}
                          </span>
                        </div>
                        <span className="shrink-0 font-medium">{formatCurrency(expense.amount)}</span>
                      </li>
                    ))}
                  </ul>
                )}
              </CardContent>
            </Card>

            <Card>
              <CardHeader>
                <CardTitle className="text-sm font-medium text-on-surface-variant">
                  Historial de ingresos ({formatCurrency(revenueTotal)})
                </CardTitle>
              </CardHeader>
              <CardContent>
                {!revenueHistory || revenueHistory.length === 0 ? (
                  <p className="text-sm text-on-surface-variant">Sin ingresos en este periodo</p>
                ) : (
                  <ul className="flex max-h-72 flex-col gap-2 overflow-y-auto pr-1">
                    {revenueHistory.map((item, index) => (
                      <li
                        key={`${item.invoiceNumber}-${index}`}
                        className="flex items-center justify-between gap-2 text-sm"
                      >
                        <div className="flex flex-col">
                          <span className="font-medium">{item.invoiceNumber}</span>
                          <span className="text-on-surface-variant">{item.description}</span>
                        </div>
                        <div className="flex flex-col items-end">
                          <span className="font-medium">{formatCurrency(item.subtotal)}</span>
                          <span className="text-on-surface-variant">
                            {new Date(item.issueDate).toLocaleDateString('es-ES')}
                          </span>
                        </div>
                      </li>
                    ))}
                  </ul>
                )}
              </CardContent>
            </Card>
          </div>

          <h2 className="font-display text-lg font-semibold">Totales</h2>
          <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-5">
            <Card>
              <CardHeader className="p-3 pb-0">
                <CardTitle className="text-xs font-medium text-on-surface-variant">Ingresos</CardTitle>
              </CardHeader>
              <CardContent className="px-3 pb-3">
                <p className="text-lg font-semibold">{formatCurrency(profitability.revenue)}</p>
              </CardContent>
            </Card>

            <Card>
              <CardHeader className="p-3 pb-0">
                <CardTitle className="text-xs font-medium text-on-surface-variant">Gastos</CardTitle>
              </CardHeader>
              <CardContent className="px-3 pb-3">
                <p className="text-lg font-semibold">{formatCurrency(profitability.costs)}</p>
              </CardContent>
            </Card>

            <Card className={signCardClassName(profitability.margin)}>
              <CardHeader className="p-3 pb-0">
                <CardTitle className="text-xs font-medium text-on-surface-variant">Margen</CardTitle>
              </CardHeader>
              <CardContent className="px-3 pb-3">
                <p className="text-lg font-semibold">{formatCurrency(profitability.margin)}</p>
              </CardContent>
            </Card>

            <Card>
              <CardHeader className="p-3 pb-0">
                <CardTitle className="text-xs font-medium text-on-surface-variant">
                  Coste/{usageUnitLabel}
                </CardTitle>
              </CardHeader>
              <CardContent className="px-3 pb-3">
                {profitability.costPerUsageUnit == null ? (
                  <p className="text-sm text-on-surface-variant">Sin datos suficientes</p>
                ) : (
                  <p className="text-lg font-semibold">{formatCurrency(profitability.costPerUsageUnit)}</p>
                )}
              </CardContent>
            </Card>

            <Card className={signCardClassName(profitability.profitPerUsageUnit)}>
              <CardHeader className="p-3 pb-0">
                <CardTitle className="text-xs font-medium text-on-surface-variant">
                  Beneficio/{usageUnitLabel}
                </CardTitle>
              </CardHeader>
              <CardContent className="px-3 pb-3">
                {profitability.profitPerUsageUnit == null ? (
                  <p className="text-sm text-on-surface-variant">Sin datos suficientes</p>
                ) : (
                  <p className="text-lg font-semibold">{formatCurrency(profitability.profitPerUsageUnit)}</p>
                )}
              </CardContent>
            </Card>
          </div>
        </>
      )}
    </div>
  )
}
