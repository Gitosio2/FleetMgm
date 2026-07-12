import { useQuery } from '@tanstack/react-query'
import { apiClient } from '@fleetmgm/api'
import type { GpsPosition, VehicleCategory } from '@fleetmgm/api'

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
