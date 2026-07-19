import { useQuery } from '@tanstack/react-query'
import { apiClient } from '@fleetmgm/api'
import type { CreateWorkerRequest, PageResponse, UpdateWorkerRequest, Worker, WorkerRole } from '@fleetmgm/api'
import { createCrudHooks } from './createCrudHooks'

export const WORKER_KEY = 'workers'

const workerHooks = createCrudHooks<Worker, CreateWorkerRequest, UpdateWorkerRequest>(
  WORKER_KEY,
  '/workers',
)

export type WorkerFilters = {
  name?: string
  nationalId?: string
  workerRole?: WorkerRole
}

// Hand-rolled instead of workerHooks.useList — same rationale as useSuppliers/useClients:
// createCrudHooks has no filter support and stays that way. Uses the WORKER_KEY constant the
// factory was built with, so useCreateWorker/useUpdateWorker/useDeleteWorker's
// invalidateQueries({ queryKey: [WORKER_KEY] }) still refreshes this list.
export function useWorkers(filters: WorkerFilters = {}, page = 0, size = 20) {
  const { name, nationalId, workerRole } = filters

  return useQuery({
    queryKey: [WORKER_KEY, { ...filters, page, size }],
    queryFn: async () => {
      const { data } = await apiClient.get<PageResponse<Worker>>('/workers', {
        params: { name, nationalId, workerRole, page, size },
      })
      return data
    },
  })
}

export const useCreateWorker = workerHooks.useCreate
export const useUpdateWorker = workerHooks.useUpdate
export const useDeleteWorker = workerHooks.useDelete

// For populating a <select> that must offer every worker, not just the first page — same
// rationale as useAllVehicles in useVehicles.ts.
export function useAllWorkers(filters: WorkerFilters = {}) {
  const { name, nationalId, workerRole } = filters

  return useQuery({
    queryKey: [WORKER_KEY, 'all', filters],
    queryFn: async () => {
      const all: Worker[] = []
      let page = 0
      let totalPages = 1
      do {
        const { data } = await apiClient.get<PageResponse<Worker>>('/workers', {
          params: { name, nationalId, workerRole, page, size: 200 },
        })
        all.push(...data.content)
        totalPages = data.totalPages
        page += 1
      } while (page < totalPages)
      return all
    },
  })
}
