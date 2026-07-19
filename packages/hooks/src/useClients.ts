import { useQuery } from '@tanstack/react-query'
import { apiClient } from '@fleetmgm/api'
import type { Client, CreateClientRequest, PageResponse, UpdateClientRequest } from '@fleetmgm/api'
import { createCrudHooks } from './createCrudHooks'

export const CLIENT_KEY = 'clients'

const clientHooks = createCrudHooks<Client, CreateClientRequest, UpdateClientRequest>(
  CLIENT_KEY,
  '/clients',
)

export type ClientFilters = {
  name?: string
  taxId?: string
}

// Hand-rolled instead of clientHooks.useList (createCrudHooks has no filter support, and stays
// that way — every other CRUD entity in this app doesn't need filtering, so extending the shared
// factory just for this one consumer isn't worth the risk). Uses the CLIENT_KEY constant the
// factory was built with, so useCreateClient/useUpdateClient/useDeleteClient's
// invalidateQueries({ queryKey: [CLIENT_KEY] }) still refreshes this list — same pattern as
// useSuppliers.
export function useClients(filters: ClientFilters = {}, page = 0, size = 20) {
  const { name, taxId } = filters

  return useQuery({
    queryKey: [CLIENT_KEY, { ...filters, page, size }],
    queryFn: async () => {
      const { data } = await apiClient.get<PageResponse<Client>>('/clients', {
        params: { name, taxId, page, size },
      })
      return data
    },
  })
}

export const useClient = clientHooks.useDetail
export const useCreateClient = clientHooks.useCreate
export const useUpdateClient = clientHooks.useUpdate
export const useDeleteClient = clientHooks.useDelete

// For populating a <select> that must offer every client, not just the first page — same
// rationale as useAllVehicles in useVehicles.ts.
export function useAllClients(filters: ClientFilters = {}) {
  const { name, taxId } = filters

  return useQuery({
    queryKey: [CLIENT_KEY, 'all', filters],
    queryFn: async () => {
      const all: Client[] = []
      let page = 0
      let totalPages = 1
      do {
        const { data } = await apiClient.get<PageResponse<Client>>('/clients', {
          params: { name, taxId, page, size: 200 },
        })
        all.push(...data.content)
        totalPages = data.totalPages
        page += 1
      } while (page < totalPages)
      return all
    },
  })
}
