import { useQuery } from '@tanstack/react-query'
import { apiClient } from '@fleetmgm/api'
import type { MonthlyFinancial, PageResponse, Profitability, VehicleRevenueLineItem } from '@fleetmgm/api'

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

export function useVehicleProfitability(vehicleId: string) {
  return useQuery({
    queryKey: [PROFITABILITY_KEY, 'vehicle', vehicleId],
    queryFn: async () => {
      const { data } = await apiClient.get<Profitability>(`/reports/profitability/${vehicleId}`)
      return data
    },
    enabled: Boolean(vehicleId),
  })
}

export function useVehicleRevenue(vehicleId: string, year?: number, month?: number) {
  return useQuery({
    queryKey: [PROFITABILITY_KEY, 'vehicle', vehicleId, 'revenue', { year, month }],
    queryFn: async () => {
      const { data } = await apiClient.get<VehicleRevenueLineItem[]>(
        `/reports/profitability/${vehicleId}/revenue`,
        { params: { year, month } },
      )
      return data
    },
    enabled: Boolean(vehicleId),
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
