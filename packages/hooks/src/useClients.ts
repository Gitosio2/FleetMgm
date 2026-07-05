import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { apiClient } from '@fleetmgm/api'
import type { Client, CreateClientRequest, PageResponse, UpdateClientRequest } from '@fleetmgm/api'

const CLIENTS_KEY = 'clients'

export function useClients(page = 0, size = 20) {
  return useQuery({
    queryKey: [CLIENTS_KEY, { page, size }],
    queryFn: async () => {
      const { data } = await apiClient.get<PageResponse<Client>>('/clients', {
        params: { page, size },
      })
      return data
    },
  })
}

export function useCreateClient() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: async (request: CreateClientRequest) => {
      const { data } = await apiClient.post<Client>('/clients', request)
      return data
    },
    onSuccess: () => queryClient.invalidateQueries({ queryKey: [CLIENTS_KEY] }),
  })
}

export function useUpdateClient() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: async ({ id, request }: { id: string; request: UpdateClientRequest }) => {
      const { data } = await apiClient.put<Client>(`/clients/${id}`, request)
      return data
    },
    onSuccess: () => queryClient.invalidateQueries({ queryKey: [CLIENTS_KEY] }),
  })
}

export function useDeleteClient() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: async (id: string) => {
      await apiClient.delete(`/clients/${id}`)
    },
    onSuccess: () => queryClient.invalidateQueries({ queryKey: [CLIENTS_KEY] }),
  })
}
