import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { apiClient } from '@fleetmgm/api'
import type { CreateScheduleRequest, PageResponse, ScheduleRange, WorkshopSchedule } from '@fleetmgm/api'
import { MAINTENANCE_KEY } from './useMaintenance'

export const WORKSHOP_KEY = 'workshop'

export function useWorkshopSchedules(range: ScheduleRange, page = 0, size = 20) {
  return useQuery({
    queryKey: [WORKSHOP_KEY, range, { page, size }],
    queryFn: async () => {
      const { data } = await apiClient.get<PageResponse<WorkshopSchedule>>('/workshop/schedules', {
        params: { range, page, size },
      })
      return data
    },
  })
}

export function useCreateWorkshopSchedule() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: async (request: CreateScheduleRequest) => {
      const { data } = await apiClient.post<WorkshopSchedule>('/workshop/schedules', request)
      return data
    },
    onSuccess: () => queryClient.invalidateQueries({ queryKey: [WORKSHOP_KEY] }),
  })
}

export function useCancelWorkshopSchedule() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: async (id: string) => {
      const { data } = await apiClient.patch<WorkshopSchedule>(`/workshop/schedules/${id}/cancel`)
      return data
    },
    // Mirrors useCancelMaintenance/useCompleteMaintenance: cancelling a schedule can cascade to
    // a linked MaintenanceRecord server-side (unless it's already terminal), so invalidate both
    // feature keys — otherwise the "Órdenes de mantenimiento" table keeps showing a stale
    // pre-cancel status and still-clickable action buttons for a record that's actually
    // terminal server-side.
    onSuccess: () =>
      Promise.all([
        queryClient.invalidateQueries({ queryKey: [WORKSHOP_KEY] }),
        queryClient.invalidateQueries({ queryKey: [MAINTENANCE_KEY] }),
      ]),
  })
}
