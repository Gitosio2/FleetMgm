import { useMutation, useQueryClient } from '@tanstack/react-query'
import { apiClient } from '@fleetmgm/api'
import type { CreateJobRequest, Job, UpdateJobRequest } from '@fleetmgm/api'
import { createCrudHooks } from './createCrudHooks'

const JOBS_KEY = 'jobs'

const jobHooks = createCrudHooks<Job, CreateJobRequest, UpdateJobRequest>(JOBS_KEY, '/jobs')

export const useJobs = jobHooks.useList
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
