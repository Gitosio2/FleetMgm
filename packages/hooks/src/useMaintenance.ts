import { useMutation, useQueryClient } from '@tanstack/react-query'
import { apiClient } from '@fleetmgm/api'
import type { CreateMaintenanceRequest, MaintenanceRecord, UpdateMaintenanceRequest } from '@fleetmgm/api'
import { createCrudHooks } from './createCrudHooks'
import { invalidateQueryKeys } from './invalidateQueryKeys'
import { WORKSHOP_KEY } from './useWorkshop'

export const MAINTENANCE_KEY = 'maintenance'

const maintenanceHooks = createCrudHooks<MaintenanceRecord, CreateMaintenanceRequest, UpdateMaintenanceRequest>(
  MAINTENANCE_KEY,
  '/maintenance',
)

export const useMaintenanceRecords = maintenanceHooks.useList

export function useCreateMaintenance() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: async (request: CreateMaintenanceRequest) => {
      const { data } = await apiClient.post<MaintenanceRecord>('/maintenance', request)
      return data
    },
    // Creating a maintenance order also creates its linked WorkshopSchedule server-side
    // (MaintenanceService.create() -> WorkshopScheduleService.create(), deployment-gated by
    // WORKSHOP_AUTO_CREATE_SCHEDULE) — invalidate both feature keys so the agenda reflects the
    // new entry without a manual refresh. Mirrors useCompleteMaintenance/useCancelMaintenance.
    onSuccess: () => invalidateQueryKeys(queryClient, [MAINTENANCE_KEY, WORKSHOP_KEY]),
  })
}

export function useStartMaintenance() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: async ({ id, usageAtService }: { id: string; usageAtService?: number | null }) => {
      const { data } = await apiClient.patch<MaintenanceRecord>(`/maintenance/${id}/start`, { usageAtService })
      return data
    },
    onSuccess: () => queryClient.invalidateQueries({ queryKey: [MAINTENANCE_KEY] }),
  })
}

export function useCompleteMaintenance() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: async ({ id, cost }: { id: string; cost?: number | null }) => {
      const { data } = await apiClient.patch<MaintenanceRecord>(`/maintenance/${id}/complete`, { cost })
      return data
    },
    // Completing a maintenance record can transition a linked WorkshopSchedule to COMPLETED
    // server-side (MaintenanceCompletedEvent -> ScheduleCompletionListener, Hito 26) — the
    // agenda has no manual "/complete" endpoint of its own, so this is the only mutation that
    // can change a schedule's status to COMPLETED. Invalidate both feature keys so the
    // Workshop page's unified view (agenda + orders) reflects it without a manual refresh.
    onSuccess: () => invalidateQueryKeys(queryClient, [MAINTENANCE_KEY, WORKSHOP_KEY]),
  })
}

export function useCancelMaintenance() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: async (id: string) => {
      const { data } = await apiClient.patch<MaintenanceRecord>(`/maintenance/${id}/cancel`)
      return data
    },
    // Mirrors useCompleteMaintenance: cancelling a maintenance record can cascade to a linked
    // WorkshopSchedule server-side, so invalidate both feature keys.
    onSuccess: () => invalidateQueryKeys(queryClient, [MAINTENANCE_KEY, WORKSHOP_KEY]),
  })
}
