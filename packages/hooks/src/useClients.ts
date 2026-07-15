import type { Client, CreateClientRequest, UpdateClientRequest } from '@fleetmgm/api'
import { createCrudHooks } from './createCrudHooks'

const clientHooks = createCrudHooks<Client, CreateClientRequest, UpdateClientRequest>(
  'clients',
  '/clients',
)

export const useClients = clientHooks.useList
export const useClient = clientHooks.useDetail
export const useCreateClient = clientHooks.useCreate
export const useUpdateClient = clientHooks.useUpdate
export const useDeleteClient = clientHooks.useDelete
