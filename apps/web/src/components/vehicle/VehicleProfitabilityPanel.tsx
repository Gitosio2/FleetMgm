import { useState } from 'react'
import { useVehicleMaintenanceHistory, useVehicleProfitability, useVehicleRevenue } from '@fleetmgm/hooks'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { MaintenanceStatusBadge } from '@/components/workshop/MaintenanceStatusBadge'

const MONTH_LABEL = [
  'Enero',
  'Febrero',
  'Marzo',
  'Abril',
  'Mayo',
  'Junio',
  'Julio',
  'Agosto',
  'Septiembre',
  'Octubre',
  'Noviembre',
  'Diciembre',
]

const selectClassName =
  'flex h-9 rounded-lg border border-outline-variant bg-surface-container-lowest px-3 text-sm text-on-surface focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-secondary-container'

type VehicleProfitabilityPanelProps = {
  vehicleId: string
  vehicleLabel: string
}

export function VehicleProfitabilityPanel({ vehicleId, vehicleLabel }: VehicleProfitabilityPanelProps) {
  const { data: profitability, isLoading, isError } = useVehicleProfitability(vehicleId)

  const now = new Date()
  const [year, setYear] = useState(now.getFullYear())
  const [month, setMonth] = useState(now.getMonth() + 1)
  const yearOptions = [year - 1, year, year + 1]

  const { data: maintenanceHistory } = useVehicleMaintenanceHistory(vehicleId, year, month)
  const { data: revenueHistory } = useVehicleRevenue(vehicleId, year, month)

  const maintenanceTotal = (maintenanceHistory ?? []).reduce((sum, record) => sum + (record.cost ?? 0), 0)
  const revenueTotal = (revenueHistory ?? []).reduce((sum, item) => sum + item.subtotal, 0)

  if (isLoading) {
    return <p className="text-on-surface-variant">Cargando rentabilidad…</p>
  }

  if (isError || !profitability) {
    return (
      <p className="text-on-surface-variant">
        No se pudo cargar la rentabilidad de {vehicleLabel}.
      </p>
    )
  }

  return (
    <div className="flex flex-col gap-4">
      <div className="flex flex-wrap items-center gap-3">
        <select
          aria-label="Mes"
          className={selectClassName}
          value={month}
          onChange={(e) => setMonth(Number(e.target.value))}
        >
          {MONTH_LABEL.map((label, index) => (
            <option key={label} value={index + 1}>
              {label}
            </option>
          ))}
        </select>
        <select
          aria-label="Año"
          className={selectClassName}
          value={year}
          onChange={(e) => setYear(Number(e.target.value))}
        >
          {yearOptions.map((yearOption) => (
            <option key={yearOption} value={yearOption}>
              {yearOption}
            </option>
          ))}
        </select>
      </div>

      <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
        <Card>
          <CardHeader>
            <CardTitle className="text-sm font-medium text-on-surface-variant">
              Historial de mantenimientos ({maintenanceTotal.toFixed(2)} €)
            </CardTitle>
          </CardHeader>
          <CardContent>
            {!maintenanceHistory || maintenanceHistory.length === 0 ? (
              <p className="text-sm text-on-surface-variant">Sin mantenimientos en este periodo</p>
            ) : (
              <ul className="flex flex-col gap-2">
                {maintenanceHistory.map((record) => (
                  <li key={record.id} className="flex items-center justify-between gap-2 text-sm">
                    <div className="flex flex-col">
                      <span className="font-medium">{record.type}</span>
                      <span className="text-on-surface-variant">
                        {record.workshopEntryDate
                          ? new Date(record.workshopEntryDate).toLocaleDateString('es-ES')
                          : 'Sin fecha'}
                      </span>
                    </div>
                    <div className="flex flex-col items-end gap-1">
                      <span className="font-medium">{(record.cost ?? 0).toFixed(2)} €</span>
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
              Historial de ingresos ({revenueTotal.toFixed(2)} €)
            </CardTitle>
          </CardHeader>
          <CardContent>
            {!revenueHistory || revenueHistory.length === 0 ? (
              <p className="text-sm text-on-surface-variant">Sin ingresos en este periodo</p>
            ) : (
              <ul className="flex flex-col gap-2">
                {revenueHistory.map((item, index) => (
                  <li key={`${item.invoiceNumber}-${index}`} className="flex items-center justify-between gap-2 text-sm">
                    <div className="flex flex-col">
                      <span className="font-medium">{item.invoiceNumber}</span>
                      <span className="text-on-surface-variant">{item.description}</span>
                    </div>
                    <div className="flex flex-col items-end">
                      <span className="font-medium">{item.subtotal.toFixed(2)} €</span>
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

      <h2 className="font-display text-lg font-semibold">Total histórico</h2>
      <div className="grid grid-cols-1 gap-3 sm:grid-cols-3">
        <Card>
          <CardHeader className="p-3 pb-0">
            <CardTitle className="text-xs font-medium text-on-surface-variant">Ingresos</CardTitle>
          </CardHeader>
          <CardContent className="px-3 pb-3">
            <p className="text-lg font-semibold">{profitability.revenue.toFixed(2)} €</p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="p-3 pb-0">
            <CardTitle className="text-xs font-medium text-on-surface-variant">Gastos</CardTitle>
          </CardHeader>
          <CardContent className="px-3 pb-3">
            <p className="text-lg font-semibold">{profitability.costs.toFixed(2)} €</p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="p-3 pb-0">
            <CardTitle className="text-xs font-medium text-on-surface-variant">Margen</CardTitle>
          </CardHeader>
          <CardContent className="px-3 pb-3">
            <p className="text-lg font-semibold">{profitability.margin.toFixed(2)} €</p>
          </CardContent>
        </Card>
      </div>
    </div>
  )
}
