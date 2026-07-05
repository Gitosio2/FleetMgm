import { create } from 'zustand'
import { persist } from 'zustand/middleware'

export type AppRole = 'ADMIN' | 'MANAGER' | 'ADMINISTRATIVE' | 'WORKSHOP_STAFF' | 'DRIVER'

export type AuthSession = {
  email: string
  role: AppRole
  accessToken: string
  refreshToken: string
}

type AuthState = {
  email: string | null
  role: AppRole | null
  accessToken: string | null
  refreshToken: string | null
  isAuthenticated: boolean
  login: (session: AuthSession) => void
  logout: () => void
  setAccessToken: (accessToken: string) => void
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      email: null,
      role: null,
      accessToken: null,
      refreshToken: null,
      isAuthenticated: false,
      login: (session) => set({ ...session, isAuthenticated: true }),
      logout: () =>
        set({
          email: null,
          role: null,
          accessToken: null,
          refreshToken: null,
          isAuthenticated: false,
        }),
      setAccessToken: (accessToken) => set({ accessToken }),
    }),
    { name: 'fleetmgm-auth' },
  ),
)
