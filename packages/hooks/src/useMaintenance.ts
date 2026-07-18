import { useQuery } from '@tanstack/react-query'
import { apiClient } from '@fleetmgm/api'
import type { CreateMaintenanceRequest, MaintenanceRecord, PageResponse, UpdateMaintenanceRequest } from '@fleetmgm/api'
import { createCrudHooks } from './createCrudHooks'

export const MAINTENANCE_KEY = 'maintenance'

const maintenanceHooks = createCrudHooks<MaintenanceRecord, CreateMaintenanceRequest, UpdateMaintenanceRequest>(
  MAINTENANCE_KEY,
  '/maintenance',
)

export const useMaintenanceRecords = maintenanceHooks.useList
export const useUpdateMaintenance = maintenanceHooks.useUpdate

// Bypasses createCrudHooks.useList (page/size only, no arbitrary filters) — same rationale as
// useVehicleProfitability being hand-written rather than reusing a factory. Fetches a single
// generous page since results are already scoped to one vehicle + one month.
export function useVehicleMaintenanceHistory(vehicleId: string, year?: number, month?: number) {
  return useQuery({
    queryKey: [MAINTENANCE_KEY, 'vehicle', vehicleId, { year, month }],
    queryFn: async () => {
      const { data } = await apiClient.get<PageResponse<MaintenanceRecord>>('/maintenance', {
        params: { vehicleId, year, month, size: 100 },
      })
      return data.content
    },
    enabled: Boolean(vehicleId),
  })
}
