import { useMutation, useQueryClient } from '@tanstack/react-query'
import { apiClient } from '@fleetmgm/api'
import type { CreateMaintenanceRequest, MaintenanceRecord, UpdateMaintenanceRequest } from '@fleetmgm/api'
import { createCrudHooks } from './createCrudHooks'
import { WORKSHOP_KEY } from './useWorkshop'

const MAINTENANCE_KEY = 'maintenance'

const maintenanceHooks = createCrudHooks<MaintenanceRecord, CreateMaintenanceRequest, UpdateMaintenanceRequest>(
  MAINTENANCE_KEY,
  '/maintenance',
)

export const useMaintenanceRecords = maintenanceHooks.useList
export const useCreateMaintenance = maintenanceHooks.useCreate
export const useUpdateMaintenance = maintenanceHooks.useUpdate

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
    onSuccess: () =>
      Promise.all([
        queryClient.invalidateQueries({ queryKey: [MAINTENANCE_KEY] }),
        queryClient.invalidateQueries({ queryKey: [WORKSHOP_KEY] }),
      ]),
  })
}
