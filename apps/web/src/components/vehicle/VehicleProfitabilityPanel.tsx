import { useVehicleProfitability } from '@fleetmgm/hooks'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'

type VehicleProfitabilityPanelProps = {
  vehicleId: string
  vehicleLabel: string
}

export function VehicleProfitabilityPanel({ vehicleId, vehicleLabel }: VehicleProfitabilityPanelProps) {
  const { data: profitability, isLoading, isError } = useVehicleProfitability(vehicleId)

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
    <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
      <Card>
        <CardHeader>
          <CardTitle className="text-sm font-medium text-on-surface-variant">Ingresos</CardTitle>
        </CardHeader>
        <CardContent>
          <p className="text-3xl font-semibold">{profitability.revenue.toFixed(2)} €</p>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle className="text-sm font-medium text-on-surface-variant">Gastos</CardTitle>
        </CardHeader>
        <CardContent>
          <p className="text-3xl font-semibold">{profitability.costs.toFixed(2)} €</p>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle className="text-sm font-medium text-on-surface-variant">Margen</CardTitle>
        </CardHeader>
        <CardContent>
          <p className="text-3xl font-semibold">{profitability.margin.toFixed(2)} €</p>
        </CardContent>
      </Card>
    </div>
  )
}
