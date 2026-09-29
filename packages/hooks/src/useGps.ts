import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { apiClient } from '@fleetmgm/api'
import type { GpsMockStatus, GpsPosition, VehicleCategory } from '@fleetmgm/api'

const GPS_KEY = 'gps'
const POLL_INTERVAL_MS = 10_000

export function useGps(category?: VehicleCategory, vehicleId?: string) {
  return useQuery({
    queryKey: [GPS_KEY, 'latest', { category, vehicleId }],
    queryFn: async () => {
      const { data } = await apiClient.get<GpsPosition[]>('/gps/latest', {
        params: { category, vehicleId },
      })
      return data
    },
    refetchInterval: POLL_INTERVAL_MS,
  })
}

// Deliberately not polled: the status only changes when someone toggles it here, and a background
// poll would keep the backend awake around the clock — the opposite of what this switch is for.
export function useGpsMockStatus() {
  return useQuery({
    queryKey: [GPS_KEY, 'mock'],
    queryFn: async () => {
      const { data } = await apiClient.get<GpsMockStatus>('/gps/mock')
      return data
    },
  })
}

export function useSetGpsMock() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: async (enabled: boolean) => {
      const { data } = await apiClient.patch<GpsMockStatus>('/gps/mock', { enabled })
      return data
    },
    onSuccess: () => queryClient.invalidateQueries({ queryKey: [GPS_KEY] }),
  })
}
