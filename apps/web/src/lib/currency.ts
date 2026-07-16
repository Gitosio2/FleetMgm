const currencyFormatter = new Intl.NumberFormat('es-ES', {
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
  useGrouping: true,
})

export function formatCurrency(value: number): string {
  return `${currencyFormatter.format(value)}€`
}
