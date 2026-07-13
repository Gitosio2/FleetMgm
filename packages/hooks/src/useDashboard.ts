import { useQuery } from '@tanstack/react-query'
import { apiClient } from '@fleetmgm/api'
import type { FinancialSummary, FleetSummary } from '@fleetmgm/api'

export const FLEET_SUMMARY_KEY = 'fleet-summary'
export const FINANCIAL_SUMMARY_KEY = 'financial-summary'

export function useFleetSummary() {
  return useQuery({
    queryKey: [FLEET_SUMMARY_KEY],
    queryFn: async () => {
      const { data } = await apiClient.get<FleetSummary>('/reports/fleet-summary')
      return data
    },
  })
}

export function useFinancialSummary() {
  return useQuery({
    queryKey: [FINANCIAL_SUMMARY_KEY],
    queryFn: async () => {
      const { data } = await apiClient.get<FinancialSummary>('/reports/financial-summary')
      return data
    },
  })
}
