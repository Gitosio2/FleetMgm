import { useMutation } from '@tanstack/react-query'
import { apiClient } from '@fleetmgm/api'
import { useAuthStore, type AppRole } from '@fleetmgm/store'

type LoginRequest = {
  email: string
  password: string
}

type AuthResponse = {
  accessToken: string
  refreshToken: string
  role: AppRole
}

export function useLogin() {
  const login = useAuthStore((state) => state.login)

  return useMutation({
    mutationFn: async (request: LoginRequest) => {
      const { data } = await apiClient.post<AuthResponse>('/auth/login', request)
      return { ...data, email: request.email }
    },
    onSuccess: (session) => login(session),
  })
}

export function useLogout() {
  const refreshToken = useAuthStore((state) => state.refreshToken)
  const logout = useAuthStore((state) => state.logout)

  return useMutation({
    mutationFn: async () => {
      if (refreshToken) {
        await apiClient.post('/auth/logout', { refreshToken })
      }
    },
    onSettled: () => logout(),
  })
}
