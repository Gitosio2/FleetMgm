import { useQuery } from '@tanstack/react-query'
import { apiClient } from '@fleetmgm/api'
import type {
  CreateMaintenanceRequest,
  MaintenanceCategory,
  MaintenanceRecord,
  MaintenanceStatus,
  PageResponse,
  UpdateMaintenanceRequest,
} from '@fleetmgm/api'
import { createCrudHooks } from './createCrudHooks'

export const MAINTENANCE_KEY = 'maintenance'

const maintenanceHooks = createCrudHooks<MaintenanceRecord, CreateMaintenanceRequest, UpdateMaintenanceRequest>(
  MAINTENANCE_KEY,
  '/maintenance',
)

export type MaintenanceFilters = {
  vehicleId?: string
  type?: string
  category?: MaintenanceCategory
  status?: MaintenanceStatus
  technicianId?: string
  costFrom?: number
  costTo?: number
}

// Hand-rolled instead of maintenanceHooks.useList — same rationale as useJobs/useVehicles/
// useWorkers: createCrudHooks has no filter support and stays that way. Uses the MAINTENANCE_KEY
// constant the factory was built with, so useUpdateMaintenance's
// invalidateQueries({ queryKey: [MAINTENANCE_KEY] }) still refreshes this list.
export function useMaintenanceRecords(filters: MaintenanceFilters = {}, page = 0, size = 20) {
  const { vehicleId, type, category, status, technicianId, costFrom, costTo } = filters

  return useQuery({
    queryKey: [MAINTENANCE_KEY, { ...filters, page, size }],
    queryFn: async () => {
      const { data } = await apiClient.get<PageResponse<MaintenanceRecord>>('/maintenance', {
        params: { vehicleId, type, category, status, technicianId, costFrom, costTo, page, size },
      })
      return data
    },
  })
}

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
