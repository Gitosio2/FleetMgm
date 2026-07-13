import type { MonthlyFinancial } from '@fleetmgm/api'
import {
  Bar,
  BarChart,
  CartesianGrid,
  Legend,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts'

type ProfitabilityChartProps = {
  data: MonthlyFinancial[]
}

// Defined in apps/web/src/index.css's @theme block (validated there against this app's actual
// dark surface with the dataviz skill's palette validator) — SVG presentation attributes like
// `fill` participate in the CSS cascade in modern browsers, so `var(--color-x)` resolves here the
// same way it would in a stylesheet.
const CHART_COLORS = {
  revenue: 'var(--color-chart-revenue)',
  costs: 'var(--color-chart-costs)',
} as const

// Muted axis ink and recessive gridline, reusing this app's existing design tokens.
const AXIS_TICK_COLOR = 'var(--color-on-surface-variant)'
const GRID_STROKE_COLOR = 'var(--color-outline-variant)'

function monthLabel(month: string) {
  return new Date(`${month}-01`).toLocaleDateString('es-ES', { month: 'short', year: 'numeric' })
}

function formatCurrency(value: number) {
  return `${value.toFixed(2)} €`
}

export function ProfitabilityChart({ data }: ProfitabilityChartProps) {
  const chartData = data.map((entry) => ({
    label: monthLabel(entry.month),
    revenue: entry.revenue,
    costs: entry.costs,
  }))

  return (
    <div className="h-96 w-full">
      <ResponsiveContainer width="100%" height="100%">
        <BarChart data={chartData}>
          <CartesianGrid strokeDasharray="3 3" stroke={GRID_STROKE_COLOR} strokeOpacity={0.4} />
          <XAxis dataKey="label" tick={{ fill: AXIS_TICK_COLOR }} />
          <YAxis tick={{ fill: AXIS_TICK_COLOR }} />
          <Tooltip
            formatter={(value) => formatCurrency(Number(value))}
            contentStyle={{
              backgroundColor: 'var(--color-surface-container)',
              border: '1px solid var(--color-outline-variant)',
            }}
            labelStyle={{ color: 'var(--color-on-surface)' }}
          />
          <Legend />
          <Bar dataKey="revenue" name="Ingresos" fill={CHART_COLORS.revenue} />
          <Bar dataKey="costs" name="Gastos" fill={CHART_COLORS.costs} />
        </BarChart>
      </ResponsiveContainer>
    </div>
  )
}
