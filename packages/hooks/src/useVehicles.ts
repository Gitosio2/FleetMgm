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

export const useVehicle = vehicleHooks.useDetail
export const useCreateVehicle = vehicleHooks.useCreate
export const useUpdateVehicle = vehicleHooks.useUpdate
export const useDeleteVehicle = vehicleHooks.useDelete

// For populating a <select> that must offer every vehicle, not just the first page — a plain
// useVehicles({}, 0, 100) silently hid vehicles beyond the 100th on fleets past that size, making
// the vehicle filter/assignment selects unable to target them at all (unlike free-text filters,
// which still reach every row server-side; an exact-match select can only offer what's actually
// rendered as an <option>). Loops pages at a generous batch size until exhausted; fine for this
// project's fleet sizes — a fleet large enough to make this loop slow would also make a single
// giant <select> unusable, at which point the real fix is a server-side searchable combobox.
export function useAllVehicles(filters: VehicleFilters = {}) {
  const { category, status, licensePlate, vehicle } = filters

  return useQuery({
    queryKey: [VEHICLE_KEY, 'all', filters],
    queryFn: async () => {
      const all: Vehicle[] = []
      let page = 0
      let totalPages = 1
      do {
        const { data } = await apiClient.get<PageResponse<Vehicle>>('/vehicles', {
          params: { category, status, licensePlate, vehicle, page, size: 200 },
        })
        all.push(...data.content)
        totalPages = data.totalPages
        page += 1
      } while (page < totalPages)
      return all
    },
  })
}
