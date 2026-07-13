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

// Validated against this app's actual dark surface (#0b1326) with the dataviz skill's palette
// validator — the app's own semantic tokens (primary/secondary/etc.) failed the categorical
// lightness-band check and are UI chrome only, not chart-safe. These two hex values passed
// lightness band, chroma floor, CVD separation (ΔE 69.8), and contrast (>=3:1). Recharts consumes
// SVG `fill`/`stroke` as plain strings, so a JS constant is simpler here than a CSS custom property.
const CHART_COLORS = {
  revenue: '#3987e5',
  costs: '#199e70',
} as const

// Muted axis ink and recessive gridline, matching the `text-on-surface-variant` /
// `outline-variant` tokens already used elsewhere (e.g. FleetKpiCards.tsx) — Recharts renders
// SVG text/stroke directly, so it needs the resolved hex value, not a Tailwind class.
const AXIS_TICK_COLOR = '#c1c7c9'
const GRID_STROKE_COLOR = '#42484a'

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
            contentStyle={{ backgroundColor: '#171f33', border: '1px solid #42484a' }}
            labelStyle={{ color: '#dae2fd' }}
          />
          <Legend />
          <Bar dataKey="revenue" name="Ingresos" fill={CHART_COLORS.revenue} />
          <Bar dataKey="costs" name="Gastos" fill={CHART_COLORS.costs} />
        </BarChart>
      </ResponsiveContainer>
    </div>
  )
}
