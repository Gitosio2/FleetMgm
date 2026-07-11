import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { apiClient } from '@fleetmgm/api'
import type {
  CreateScheduleRequest,
  PageResponse,
  ScheduleRange,
  UpdateScheduleRequest,
  WorkshopSchedule,
} from '@fleetmgm/api'
import { invalidateQueryKeys } from './invalidateQueryKeys'
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

export function useUpdateWorkshopSchedule() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: async ({ id, request }: { id: string; request: UpdateScheduleRequest }) => {
      const { data } = await apiClient.put<WorkshopSchedule>(`/workshop/schedules/${id}`, request)
      return data
    },
    // Unlike useCancelWorkshopSchedule/useCompleteMaintenance, a plain update doesn't publish a
    // domain event server-side, so there's no cascade to the linked MaintenanceRecord — only
    // the workshop key needs to be invalidated.
    onSuccess: () => queryClient.invalidateQueries({ queryKey: [WORKSHOP_KEY] }),
  })
}

export function useStartWorkshopSchedule() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: async (id: string) => {
      const { data } = await apiClient.patch<WorkshopSchedule>(`/workshop/schedules/${id}/start`)
      return data
    },
    // Same rationale as useUpdateWorkshopSchedule: starting a schedule doesn't cascade to the
    // linked MaintenanceRecord server-side, so only the workshop key needs invalidation.
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
    onSuccess: () => invalidateQueryKeys(queryClient, [WORKSHOP_KEY, MAINTENANCE_KEY]),
  })
}
