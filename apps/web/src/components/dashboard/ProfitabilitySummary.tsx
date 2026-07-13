import type { MonthlyFinancial } from '@fleetmgm/api'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'

type ProfitabilitySummaryProps = {
  data: MonthlyFinancial[]
}

export function ProfitabilitySummary({ data }: ProfitabilitySummaryProps) {
  const totalRevenue = data.reduce((sum, entry) => sum + entry.revenue, 0)
  const totalCosts = data.reduce((sum, entry) => sum + entry.costs, 0)
  const totalMargin = totalRevenue - totalCosts

  return (
    <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
      <Card>
        <CardHeader>
          <CardTitle className="text-sm font-medium text-on-surface-variant">Ingresos totales</CardTitle>
        </CardHeader>
        <CardContent>
          <p className="text-3xl font-semibold">{totalRevenue.toFixed(2)} €</p>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle className="text-sm font-medium text-on-surface-variant">Gastos totales</CardTitle>
        </CardHeader>
        <CardContent>
          <p className="text-3xl font-semibold">{totalCosts.toFixed(2)} €</p>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle className="text-sm font-medium text-on-surface-variant">Margen total</CardTitle>
        </CardHeader>
        <CardContent>
          <p className="text-3xl font-semibold">{totalMargin.toFixed(2)} €</p>
        </CardContent>
      </Card>
    </div>
  )
}
