import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { apiClient } from '@fleetmgm/api'
import type { Assignment, CreateAssignmentRequest, PageResponse } from '@fleetmgm/api'

const ASSIGNMENTS_KEY = 'assignments'

export function useWorkerAssignments(workerId: string, page = 0, size = 20) {
  return useQuery({
    queryKey: [ASSIGNMENTS_KEY, 'worker', workerId, { page, size }],
    queryFn: async () => {
      const { data } = await apiClient.get<PageResponse<Assignment>>(
        `/workers/${workerId}/assignments`,
        { params: { page, size } },
      )
      return data
    },
    enabled: Boolean(workerId),
  })
}

export function useVehicleAssignment(vehicleId: string) {
  return useQuery({
    queryKey: [ASSIGNMENTS_KEY, 'vehicle', vehicleId],
    queryFn: async () => {
      const { data } = await apiClient.get<Assignment | ''>(`/vehicles/${vehicleId}/assignment`, {
        validateStatus: (status) => status === 200 || status === 204,
      })
      return data === '' || data == null ? null : data
    },
    enabled: Boolean(vehicleId),
  })
}

export function useActiveAssignmentsByDrivers(driverIds: string[]) {
  return useQuery({
    queryKey: [ASSIGNMENTS_KEY, 'active-by-drivers', driverIds],
    queryFn: async () => {
      const { data } = await apiClient.get<Assignment[]>('/assignments/active', {
        params: { driverIds: driverIds.join(',') },
      })
      return data
    },
    enabled: driverIds.length > 0,
  })
}

export function useCreateAssignment() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: async (request: CreateAssignmentRequest) => {
      const { data } = await apiClient.post<Assignment>('/assignments', request)
      return data
    },
    onSuccess: (assignment) => {
      queryClient.invalidateQueries({ queryKey: [ASSIGNMENTS_KEY, 'worker', assignment.driverId] })
      queryClient.invalidateQueries({ queryKey: [ASSIGNMENTS_KEY, 'vehicle', assignment.vehicleId] })
    },
  })
}

export function useEndAssignment() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: async (id: string) => {
      const { data } = await apiClient.patch<Assignment>(`/assignments/${id}/end`)
      return data
    },
    onSuccess: (assignment) => {
      queryClient.invalidateQueries({ queryKey: [ASSIGNMENTS_KEY, 'worker', assignment.driverId] })
      queryClient.invalidateQueries({ queryKey: [ASSIGNMENTS_KEY, 'vehicle', assignment.vehicleId] })
    },
  })
}
