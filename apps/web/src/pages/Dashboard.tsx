import { useState } from 'react'
import { useFinancialSummary, useFinancialTrend, useFleetSummary } from '@fleetmgm/hooks'
import { Button } from '@/components/ui/button'
import { FleetKpiCards } from '@/components/dashboard/FleetKpiCards'
import { FinancialSummary } from '@/components/dashboard/FinancialSummary'
import { ProfitabilityChart } from '@/components/dashboard/ProfitabilityChart'
import { ProfitabilitySummary } from '@/components/dashboard/ProfitabilitySummary'
import { Card, CardContent } from '@/components/ui/card'

const TREND_MONTHS_OPTIONS = [3, 6, 12] as const

export function Dashboard() {
  const { data: fleetSummary, isLoading: fleetLoading, isError: fleetError } = useFleetSummary()
  const { data: financialSummary, isLoading: financialLoading, isError: financialError } = useFinancialSummary()

  // Both the chart and the summary tiles read the SAME N-month window — the tiles sum it, the
  // chart plots it per-month (Hito 43 redesign, replaces the earlier per-vehicle comparison).
  const [trendMonths, setTrendMonths] = useState<number>(6)
  const {
    data: financialTrend,
    isLoading: trendLoading,
    isError: trendError,
  } = useFinancialTrend(trendMonths)

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

      <section className="flex flex-col gap-3">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <h2 className="font-display text-lg font-semibold">Ingresos y gastos</h2>
          <div className="flex items-center gap-2 text-sm text-on-surface-variant">
            <span>Ver:</span>
            {TREND_MONTHS_OPTIONS.map((months) => (
              <Button
                key={months}
                type="button"
                size="sm"
                variant={trendMonths === months ? 'default' : 'outline'}
                aria-pressed={trendMonths === months}
                onClick={() => setTrendMonths(months)}
              >
                {months} meses
              </Button>
            ))}
          </div>
        </div>
        {trendLoading ? (
          <p className="text-on-surface-variant">Cargando ingresos y gastos…</p>
        ) : trendError ? (
          <p role="alert" className="text-sm text-error">
            No se pudieron cargar los datos.
          </p>
        ) : (
          financialTrend && (
            <>
              <ProfitabilitySummary data={financialTrend} />
              <Card>
                <CardContent className="pt-6">
                  <ProfitabilityChart data={financialTrend} />
                </CardContent>
              </Card>
            </>
          )
        )}
      </section>
    </div>
  )
}
