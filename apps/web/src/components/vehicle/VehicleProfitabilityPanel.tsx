import { useState } from 'react'
import { useVehicleMaintenanceHistory, useVehicleProfitability, useVehicleRevenue } from '@fleetmgm/hooks'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { MaintenanceStatusBadge } from '@/components/workshop/MaintenanceStatusBadge'
import { formatCurrency } from '@/lib/currency'

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

  const maintenanceTotal = (maintenanceHistory ?? []).reduce((sum, record) => sum + (record.cost ?? 0), 0)
  const revenueTotal = (revenueHistory ?? []).reduce((sum, item) => sum + item.subtotal, 0)
  const usageUnitLabel = profitability?.usageMeasure === 'HOURS' ? 'hora' : 'km'

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
                  Historial de mantenimientos ({formatCurrency(maintenanceTotal)})
                </CardTitle>
              </CardHeader>
              <CardContent>
                {!maintenanceHistory || maintenanceHistory.length === 0 ? (
                  <p className="text-sm text-on-surface-variant">Sin mantenimientos en este periodo</p>
                ) : (
                  <ul className="flex max-h-72 flex-col gap-2 overflow-y-auto pr-1">
                    {maintenanceHistory.map((record) => (
                      <li key={record.id} className="flex items-center justify-between gap-2 text-sm">
                        <div className="flex min-w-0 flex-col">
                          <span className="truncate font-medium">{record.type}</span>
                          <span className="text-on-surface-variant">
                            {record.workshopEntryDate
                              ? new Date(record.workshopEntryDate).toLocaleDateString('es-ES')
                              : 'Sin fecha'}
                          </span>
                        </div>
                        <div className="flex shrink-0 flex-col items-end gap-1">
                          <span className="font-medium">{formatCurrency(record.cost ?? 0)}</span>
                          <MaintenanceStatusBadge status={record.status} />
                        </div>
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
          <div className="grid grid-cols-1 gap-3 sm:grid-cols-3 lg:grid-cols-5">
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

            <Card>
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

            <Card>
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
