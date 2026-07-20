import { beforeEach, describe, expect, it } from 'vitest'
import { useAuthStore } from './authStore'

const SESSION = {
  email: 'admin@fleetmgm.com',
  role: 'ADMIN' as const,
  accessToken: 'access-token-1',
  refreshToken: 'refresh-token-1',
}

describe('authStore', () => {
  beforeEach(() => {
    useAuthStore.getState().logout()
  })

  it('login persists the session', () => {
    useAuthStore.getState().login(SESSION)

    const state = useAuthStore.getState()
    expect(state.isAuthenticated).toBe(true)
    expect(state.email).toBe(SESSION.email)
    expect(state.role).toBe(SESSION.role)
    expect(state.accessToken).toBe(SESSION.accessToken)
    expect(state.refreshToken).toBe(SESSION.refreshToken)
  })

  it('logout clears the session', () => {
    useAuthStore.getState().login(SESSION)

    useAuthStore.getState().logout()

    const state = useAuthStore.getState()
    expect(state.isAuthenticated).toBe(false)
    expect(state.email).toBeNull()
    expect(state.role).toBeNull()
    expect(state.accessToken).toBeNull()
    expect(state.refreshToken).toBeNull()
  })

  it('setAccessToken refreshes the access token without dropping the rest of the session', () => {
    useAuthStore.getState().login(SESSION)

    useAuthStore.getState().setAccessToken('access-token-2')

    const state = useAuthStore.getState()
    expect(state.accessToken).toBe('access-token-2')
    expect(state.isAuthenticated).toBe(true)
    expect(state.email).toBe(SESSION.email)
    expect(state.refreshToken).toBe(SESSION.refreshToken)
  })

  it('always exposes a usable persist storage adapter', () => {
    expect(useAuthStore.persist.getOptions().storage).toBeDefined()
    expect(() => useAuthStore.persist.rehydrate()).not.toThrow()
  })
})
