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
    // Creating a schedule now always creates (or links) a MaintenanceRecord server-side
    // (WorkshopScheduleService.create()) — invalidate both keys so a freshly booked entry shows
    // up in Órdenes de mantenimiento immediately, not just once it's started.
    onSuccess: () => invalidateQueryKeys(queryClient, [WORKSHOP_KEY, MAINTENANCE_KEY]),
  })
}

export function useUpdateWorkshopSchedule() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: async ({ id, request }: { id: string; request: UpdateScheduleRequest }) => {
      const { data } = await apiClient.put<WorkshopSchedule>(`/workshop/schedules/${id}`, request)
      return data
    },
    // A plain update only rewires which record is linked (by id) — it never mutates the linked
    // MaintenanceRecord's own fields, so only the workshop key needs to be invalidated.
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
    // Starting a schedule now always starts (or creates-and-starts) its linked MaintenanceRecord
    // server-side (WorkshopScheduleService.start()) — invalidate both keys.
    onSuccess: () => invalidateQueryKeys(queryClient, [WORKSHOP_KEY, MAINTENANCE_KEY]),
  })
}

export function useCompleteWorkshopSchedule() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: async (id: string) => {
      await apiClient.patch(`/workshop/schedules/${id}/complete`)
    },
    // Completes the linked MaintenanceRecord, which cascades this schedule to COMPLETED via the
    // existing ScheduleCompletionListener — invalidate both keys so both tables reflect it.
    onSuccess: () => invalidateQueryKeys(queryClient, [WORKSHOP_KEY, MAINTENANCE_KEY]),
  })
}

export function useCancelWorkshopSchedule() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: async (id: string) => {
      const { data } = await apiClient.patch<WorkshopSchedule>(`/workshop/schedules/${id}/cancel`)
      return data
    },
    // Cancelling a schedule can cascade to cancel a linked MaintenanceRecord server-side (unless
    // it's already terminal), so invalidate both feature keys — otherwise the "Órdenes de
    // mantenimiento" table keeps showing a stale pre-cancel status.
    onSuccess: () => invalidateQueryKeys(queryClient, [WORKSHOP_KEY, MAINTENANCE_KEY]),
  })
}
