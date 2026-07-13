import type { FleetSummary } from '@fleetmgm/api'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'

type FleetKpiCardsProps = {
  summary: FleetSummary
}

export function FleetKpiCards({ summary }: FleetKpiCardsProps) {
  const activeRatio =
    summary.totalVehicles > 0
      ? Math.min(100, Math.round((summary.activeVehicles / summary.totalVehicles) * 100))
      : 0

  return (
    <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
      <Card>
        <CardHeader>
          <CardTitle className="text-sm font-medium text-on-surface-variant">Vehículos activos</CardTitle>
        </CardHeader>
        <CardContent>
          <p className="text-3xl font-semibold">
            {summary.activeVehicles}
            <span className="text-base font-normal text-on-surface-variant"> / {summary.totalVehicles}</span>
          </p>
          <div className="mt-3 h-2 w-full rounded-full bg-surface-container-high" role="progressbar">
            <div className="h-2 rounded-full bg-primary" style={{ width: `${activeRatio}%` }} />
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle className="text-sm font-medium text-on-surface-variant">En taller</CardTitle>
        </CardHeader>
        <CardContent>
          <p className="text-3xl font-semibold">{summary.inWorkshop}</p>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle className="text-sm font-medium text-on-surface-variant">Mantenimiento pendiente</CardTitle>
        </CardHeader>
        <CardContent>
          <p className="text-3xl font-semibold">{summary.pendingMaintenance}</p>
          {summary.pendingMaintenanceDueSoon > 0 && (
            <p className="mt-1 text-sm text-tertiary">{summary.pendingMaintenanceDueSoon} vencen en 48 horas</p>
          )}
        </CardContent>
      </Card>
    </div>
  )
}
