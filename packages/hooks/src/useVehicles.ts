import type { CreateVehicleRequest, UpdateVehicleRequest, Vehicle } from '@fleetmgm/api'
import { createCrudHooks } from './createCrudHooks'

const vehicleHooks = createCrudHooks<Vehicle, CreateVehicleRequest, UpdateVehicleRequest>(
  'vehicles',
  '/vehicles',
)

export const useVehicles = vehicleHooks.useList
export const useCreateVehicle = vehicleHooks.useCreate
export const useUpdateVehicle = vehicleHooks.useUpdate
export const useDeleteVehicle = vehicleHooks.useDelete
