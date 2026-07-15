import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { apiClient } from '@fleetmgm/api'
import type { PageResponse } from '@fleetmgm/api'

export function createCrudHooks<T, TCreate, TUpdate = TCreate>(key: string, basePath: string) {
  function useList(page = 0, size = 20) {
    return useQuery({
      queryKey: [key, { page, size }],
      queryFn: async () => {
        const { data } = await apiClient.get<PageResponse<T>>(basePath, {
          params: { page, size },
        })
        return data
      },
    })
  }

  function useDetail(id: string | undefined) {
    return useQuery({
      queryKey: [key, id],
      queryFn: async () => {
        const { data } = await apiClient.get<T>(`${basePath}/${id}`)
        return data
      },
      enabled: id != null,
    })
  }

  function useCreate() {
    const queryClient = useQueryClient()

    return useMutation({
      mutationFn: async (request: TCreate) => {
        const { data } = await apiClient.post<T>(basePath, request)
        return data
      },
      onSuccess: () => queryClient.invalidateQueries({ queryKey: [key] }),
    })
  }

  function useUpdate() {
    const queryClient = useQueryClient()

    return useMutation({
      mutationFn: async ({ id, request }: { id: string; request: TUpdate }) => {
        const { data } = await apiClient.put<T>(`${basePath}/${id}`, request)
        return data
      },
      onSuccess: () => queryClient.invalidateQueries({ queryKey: [key] }),
    })
  }

  function useDelete() {
    const queryClient = useQueryClient()

    return useMutation({
      mutationFn: async (id: string) => {
        await apiClient.delete(`${basePath}/${id}`)
      },
      onSuccess: () => queryClient.invalidateQueries({ queryKey: [key] }),
    })
  }

  return { useList, useDetail, useCreate, useUpdate, useDelete }
}
