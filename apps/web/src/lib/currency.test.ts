import { describe, expect, it } from 'vitest'
import { formatCurrency } from './currency'

describe('formatCurrency', () => {
  it('formats a positive amount with 2 decimals and the € suffix', () => {
    expect(formatCurrency(1079.5)).toBe('1.079,50€')
  })

  it('formats zero as 0,00€', () => {
    expect(formatCurrency(0)).toBe('0,00€')
  })

  it('formats NaN as 0,00€ instead of the literal "NaN"', () => {
    expect(formatCurrency(NaN)).toBe('0,00€')
  })
})
