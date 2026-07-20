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
// useVehicleProfitability being hand-written rather than reusing a factory. Loops pages the same
// way useAllVehicles/useAllClients/useAllWorkers/useAllSuppliers do — a single size:100 page was
// safe back when this was scoped to one month via a year/month selector, but the panel's Desde/
// Hasta range now defaults to unbounded full history, so a vehicle with more than 100 maintenance
// records would silently lose older ones from both the list and its displayed total while
// "Totales" (a true backend aggregate over every row) stayed correct — the two figures would then
// disagree for no visible reason.
export function useVehicleMaintenanceHistory(vehicleId: string, from?: string, to?: string) {
  return useQuery({
    queryKey: [MAINTENANCE_KEY, 'vehicle', vehicleId, { from, to }],
    queryFn: async () => {
      const all: MaintenanceRecord[] = []
      let page = 0
      let totalPages = 1
      do {
        const { data } = await apiClient.get<PageResponse<MaintenanceRecord>>('/maintenance', {
          params: { vehicleId, workshopEntryDateFrom: from, workshopEntryDateTo: to, page, size: 200 },
        })
        all.push(...data.content)
        totalPages = data.totalPages
        page += 1
      } while (page < totalPages)
      return all
    },
    enabled: Boolean(vehicleId),
  })
}
