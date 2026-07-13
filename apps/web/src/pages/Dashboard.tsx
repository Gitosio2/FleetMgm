import { useFinancialSummary, useFleetSummary } from '@fleetmgm/hooks'
import { FleetKpiCards } from '@/components/dashboard/FleetKpiCards'
import { FinancialSummary } from '@/components/dashboard/FinancialSummary'

export function Dashboard() {
  const { data: fleetSummary, isLoading: fleetLoading, isError: fleetError } = useFleetSummary()
  const { data: financialSummary, isLoading: financialLoading, isError: financialError } = useFinancialSummary()

  return (
    <div className="flex flex-col gap-6">
      <div>
        <h1 className="font-display text-3xl font-semibold">Panel de control</h1>
        <p className="text-on-surface-variant">
          Rendimiento en tiempo real y estado operativo de tu red logística.
        </p>
      </div>

      <section className="flex flex-col gap-3">
        <h2 className="font-display text-lg font-semibold">Resumen de flota</h2>
        {fleetLoading ? (
          <p className="text-on-surface-variant">Cargando resumen de la flota…</p>
        ) : fleetError ? (
          <p role="alert" className="text-sm text-error">
            No se pudieron cargar los datos.
          </p>
        ) : (
          fleetSummary && <FleetKpiCards summary={fleetSummary} />
        )}
      </section>

      <section className="flex flex-col gap-3">
        <h2 className="font-display text-lg font-semibold">Resumen económico</h2>
        {financialLoading ? (
          <p className="text-on-surface-variant">Cargando resumen económico…</p>
        ) : financialError ? (
          <p role="alert" className="text-sm text-error">
            No se pudieron cargar los datos.
          </p>
        ) : (
          financialSummary && <FinancialSummary summary={financialSummary} />
        )}
      </section>
    </div>
  )
}
