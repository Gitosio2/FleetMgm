import { useQuery } from '@tanstack/react-query'
import { apiClient } from '@fleetmgm/api'
import type { MonthlyFinancial, PageResponse, Profitability } from '@fleetmgm/api'

export const PROFITABILITY_KEY = 'profitability'

export function useProfitability(page = 0, size = 20) {
  return useQuery({
    queryKey: [PROFITABILITY_KEY, { page, size }],
    queryFn: async () => {
      const { data } = await apiClient.get<PageResponse<Profitability>>('/reports/profitability', {
        params: { page, size },
      })
      return data
    },
  })
}

export const FINANCIAL_TREND_KEY = 'profitability-trend'

export function useFinancialTrend(months: number) {
  return useQuery({
    queryKey: [FINANCIAL_TREND_KEY, months],
    queryFn: async () => {
      const { data } = await apiClient.get<MonthlyFinancial[]>('/reports/profitability/trend', {
        params: { months },
      })
      return data
    },
  })
}
