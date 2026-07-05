import axios, { type AxiosError, type InternalAxiosRequestConfig } from 'axios'
import { useAuthStore } from '@fleetmgm/store'

export const apiClient = axios.create({
  baseURL: '/api/v1',
})

apiClient.interceptors.request.use((config) => {
  const { accessToken } = useAuthStore.getState()
  if (accessToken) {
    config.headers.set('Authorization', `Bearer ${accessToken}`)
  }
  return config
})

type RetriableRequestConfig = InternalAxiosRequestConfig & { _retry?: boolean }

type RefreshResponse = {
  accessToken: string
  refreshToken: string
  role: string
}

let refreshPromise: Promise<string> | null = null

async function refreshAccessToken(): Promise<string> {
  const { refreshToken } = useAuthStore.getState()
  if (!refreshToken) {
    throw new Error('No refresh token available')
  }

  const response = await axios.post<RefreshResponse>('/api/v1/auth/refresh', { refreshToken })

  useAuthStore.getState().setAccessToken(response.data.accessToken)
  return response.data.accessToken
}

function isAuthEndpoint(url: string | undefined): boolean {
  return url != null && (url.includes('/auth/login') || url.includes('/auth/refresh'))
}

apiClient.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const originalRequest = error.config as RetriableRequestConfig | undefined

    if (
      error.response?.status !== 401 ||
      !originalRequest ||
      originalRequest._retry ||
      isAuthEndpoint(originalRequest.url)
    ) {
      return Promise.reject(error)
    }

    originalRequest._retry = true

    try {
      refreshPromise ??= refreshAccessToken().finally(() => {
        refreshPromise = null
      })
      const accessToken = await refreshPromise

      originalRequest.headers.set('Authorization', `Bearer ${accessToken}`)
      return apiClient(originalRequest)
    } catch (refreshError) {
      useAuthStore.getState().logout()
      return Promise.reject(refreshError)
    }
  },
)
