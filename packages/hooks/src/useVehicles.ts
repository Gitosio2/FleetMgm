import { useQuery } from '@tanstack/react-query'
import { apiClient } from '@fleetmgm/api'
import type {
  CreateVehicleRequest,
  PageResponse,
  UpdateVehicleRequest,
  Vehicle,
  VehicleCategory,
  VehicleStatus,
} from '@fleetmgm/api'
import { createCrudHooks } from './createCrudHooks'

export const VEHICLE_KEY = 'vehicles'

const vehicleHooks = createCrudHooks<Vehicle, CreateVehicleRequest, UpdateVehicleRequest>(
  VEHICLE_KEY,
  '/vehicles',
)

export type VehicleFilters = {
  category?: VehicleCategory
  status?: VehicleStatus
  licensePlate?: string
  vehicle?: string
}

// Hand-rolled instead of vehicleHooks.useList — same rationale as useSuppliers/useClients/
// useWorkers: createCrudHooks has no filter support and stays that way. Uses the VEHICLE_KEY
// constant the factory was built with, so useCreateVehicle/useUpdateVehicle/useDeleteVehicle's
// invalidateQueries({ queryKey: [VEHICLE_KEY] }) still refreshes this list.
export function useVehicles(filters: VehicleFilters = {}, page = 0, size = 20) {
  const { category, status, licensePlate, vehicle } = filters

  return useQuery({
    queryKey: [VEHICLE_KEY, { ...filters, page, size }],
    queryFn: async () => {
      const { data } = await apiClient.get<PageResponse<Vehicle>>('/vehicles', {
        params: { category, status, licensePlate, vehicle, page, size },
      })
      return data
    },
  })
}

export const useCreateVehicle = vehicleHooks.useCreate
export const useUpdateVehicle = vehicleHooks.useUpdate
export const useDeleteVehicle = vehicleHooks.useDelete
