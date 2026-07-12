import { useQuery } from '@tanstack/react-query'
import { apiClient } from '@fleetmgm/api'
import type { GpsPosition } from '@fleetmgm/api'

const GPS_KEY = 'gps'
const POLL_INTERVAL_MS = 10_000

export function useGps() {
  return useQuery({
    queryKey: [GPS_KEY, 'latest'],
    queryFn: async () => {
      const { data } = await apiClient.get<GpsPosition[]>('/gps/latest')
      return data
    },
    refetchInterval: POLL_INTERVAL_MS,
  })
}
