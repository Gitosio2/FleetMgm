import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { apiClient } from '@fleetmgm/api'
import type { CreateJobRequest, Job, JobStatus, PageResponse, UpdateJobRequest } from '@fleetmgm/api'
import { createCrudHooks } from './createCrudHooks'

const JOBS_KEY = 'jobs'

const jobHooks = createCrudHooks<Job, CreateJobRequest, UpdateJobRequest>(JOBS_KEY, '/jobs')

export type JobFilters = {
  title?: string
  originLocation?: string
  destinationLocation?: string
  vehicleId?: string
  assignedDriverId?: string
  status?: JobStatus
  actualStartFrom?: string
  actualStartTo?: string
  actualEndFrom?: string
  actualEndTo?: string
}

// Hand-rolled instead of jobHooks.useList — same rationale as useVehicles/useSuppliers/useClients/
// useWorkers: createCrudHooks has no filter support and stays that way. Uses the JOBS_KEY constant
// the factory was built with, so useCreateJob/useUpdateJob's invalidateQueries({ queryKey: [JOBS_KEY] })
// still refreshes this list.
export function useJobs(filters: JobFilters = {}, page = 0, size = 20) {
  const {
    title,
    originLocation,
    destinationLocation,
    vehicleId,
    assignedDriverId,
    status,
    actualStartFrom,
    actualStartTo,
    actualEndFrom,
    actualEndTo,
  } = filters

  return useQuery({
    queryKey: [JOBS_KEY, { ...filters, page, size }],
    queryFn: async () => {
      const { data } = await apiClient.get<PageResponse<Job>>('/jobs', {
        params: {
          title,
          originLocation,
          destinationLocation,
          vehicleId,
          assignedDriverId,
          status,
          actualStartFrom,
          actualStartTo,
          actualEndFrom,
          actualEndTo,
          page,
          size,
        },
      })
      return data
    },
  })
}

export const useCreateJob = jobHooks.useCreate
export const useUpdateJob = jobHooks.useUpdate

export function useStartJob() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: async ({ id, startUsageValue }: { id: string; startUsageValue?: number | null }) => {
      const { data } = await apiClient.patch<Job>(`/jobs/${id}/start`, { startUsageValue })
      return data
    },
    onSuccess: () => queryClient.invalidateQueries({ queryKey: [JOBS_KEY] }),
  })
}

export function useCompleteJob() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: async ({ id, endUsageValue }: { id: string; endUsageValue?: number | null }) => {
      const { data } = await apiClient.patch<Job>(`/jobs/${id}/complete`, { endUsageValue })
      return data
    },
    onSuccess: () => queryClient.invalidateQueries({ queryKey: [JOBS_KEY] }),
  })
}

export function useCancelJob() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: async (id: string) => {
      const { data } = await apiClient.patch<Job>(`/jobs/${id}/cancel`)
      return data
    },
    onSuccess: () => queryClient.invalidateQueries({ queryKey: [JOBS_KEY] }),
  })
}
