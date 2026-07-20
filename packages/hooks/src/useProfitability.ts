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

// from/to are optional Desde/Hasta bounds (VehicleProfitabilityPanel) — unset means full history.
export function useVehicleProfitability(vehicleId: string, from?: string, to?: string) {
  return useQuery({
    queryKey: [PROFITABILITY_KEY, 'vehicle', vehicleId, { from, to }],
    queryFn: async () => {
      const { data } = await apiClient.get<Profitability>(`/reports/profitability/${vehicleId}`, {
        params: { from, to },
      })
      return data
    },
    enabled: Boolean(vehicleId),
  })
}

export function useVehicleRevenue(vehicleId: string, from?: string, to?: string) {
  return useQuery({
    queryKey: [PROFITABILITY_KEY, 'vehicle', vehicleId, 'revenue', { from, to }],
    queryFn: async () => {
      const { data } = await apiClient.get<VehicleRevenueLineItem[]>(
        `/reports/profitability/${vehicleId}/revenue`,
        { params: { from, to } },
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
