const currencyFormatter = new Intl.NumberFormat('es-ES', {
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
  useGrouping: true,
})

export function formatCurrency(value: number): string {
  const safeValue = Number.isFinite(value) ? value : 0
  return `${currencyFormatter.format(safeValue)}€`
}
