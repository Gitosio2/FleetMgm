import type { MonthlyFinancial } from '@fleetmgm/api'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { cn } from '@/lib/utils'
import { formatCurrency } from '@/lib/currency'

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
          <p className="text-3xl font-semibold">{formatCurrency(totalRevenue)}</p>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle className="text-sm font-medium text-on-surface-variant">Gastos totales</CardTitle>
        </CardHeader>
        <CardContent>
          <p className="text-3xl font-semibold">{formatCurrency(totalCosts)}</p>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle className="text-sm font-medium text-on-surface-variant">Margen total</CardTitle>
        </CardHeader>
        <CardContent>
          <p className={cn('text-3xl font-semibold', totalMargin > 0 ? 'text-success' : 'text-error')}>
            {formatCurrency(totalMargin)}
          </p>
        </CardContent>
      </Card>
    </div>
  )
}
