import { act, create, type ReactTestRenderer } from 'react-test-renderer'
import type { ReactNode } from 'react'
import { useAuthStore } from '@fleetmgm/store'
import * as SecureStore from 'expo-secure-store'
import App from './App'
import { secureAuthStorage } from './src/auth/secureStorage'

jest.mock('expo-secure-store', () => ({
  deleteItemAsync: jest.fn(() => Promise.resolve()),
  getItemAsync: jest.fn(() => Promise.resolve(null)),
  setItemAsync: jest.fn(() => Promise.resolve()),
}))

jest.mock('./src/config/api', () => ({
  resolveApiBaseUrl: jest.fn(() => 'http://10.0.2.2:8080/api/v1'),
}))

jest.mock('react-native-safe-area-context', () => {
  const { View } = require('react-native')

  return {
    SafeAreaProvider: ({ children }: { children: ReactNode }) => <View>{children}</View>,
    useSafeAreaInsets: () => ({ bottom: 12, left: 0, right: 0, top: 28 }),
  }
})

;(globalThis as typeof globalThis & { IS_REACT_ACT_ENVIRONMENT: boolean }).IS_REACT_ACT_ENVIRONMENT = true

function resetAuthStore() {
  useAuthStore.persist.setOptions({ storage: secureAuthStorage })
  useAuthStore.setState({
    accessToken: null,
    email: null,
    isAuthenticated: false,
    refreshToken: null,
    role: null,
  })
}

describe('App', () => {
  beforeEach(() => {
    resetAuthStore()
  })

  it('uses the auth store as the source of truth after hydration and returns to login on logout', async () => {
    let screen: ReactTestRenderer | undefined

    await act(async () => {
      screen = create(<App />)
    })

    if (!screen) {
      throw new Error('App did not render')
    }

    expect(screen.root.findByProps({ accessibilityLabel: 'Correo electrónico' })).toBeTruthy()

    act(() => {
      useAuthStore.getState().login({
        accessToken: 'access-token',
        email: 'admin@fleetmgm.demo',
        refreshToken: 'refresh-token',
        role: 'ADMIN',
      })
    })

    expect(screen.root.findByProps({ accessibilityLabel: 'Cerrar sesión' })).toBeTruthy()

    act(() => {
      useAuthStore.getState().logout()
    })

    expect(screen.root.findByProps({ accessibilityLabel: 'Correo electrónico' })).toBeTruthy()
  })

  it('keeps auth UI gated until a seeded native session finishes hydrating', async () => {
    let resolveStorage: (value: string) => void
    jest.mocked(SecureStore.getItemAsync).mockImplementationOnce(
      () => new Promise((resolve) => { resolveStorage = resolve }),
    )

    let screen: ReactTestRenderer | undefined
    act(() => {
      screen = create(<App />)
    })

    if (!screen) {
      throw new Error('App did not render')
    }

    expect(screen.root.findByProps({ accessibilityLabel: 'Cargando sesión' })).toBeTruthy()
    expect(() => screen.root.findByProps({ accessibilityLabel: 'Correo electrónico' })).toThrow()

    await act(async () => {
      resolveStorage!(JSON.stringify({
        state: {
          accessToken: 'access-token',
          email: 'admin@fleetmgm.demo',
          isAuthenticated: true,
          refreshToken: 'refresh-token',
          role: 'ADMIN',
        },
        version: 0,
      }))
    })

    expect(screen.root.findByProps({ accessibilityLabel: 'Cerrar sesión' })).toBeTruthy()
  })

  it('places the authenticated menu trigger below the device status area', async () => {
    let screen: ReactTestRenderer | undefined

    await act(async () => {
      screen = create(<App />)
    })

    if (!screen) {
      throw new Error('App did not render')
    }

    act(() => {
      useAuthStore.getState().login({
        accessToken: 'access-token',
        email: 'admin@fleetmgm.demo',
        refreshToken: 'refresh-token',
        role: 'ADMIN',
      })
    })

    expect(screen.root.findByProps({ testID: 'mobile-menu-trigger-container' }).props.style).toEqual([
      expect.any(Object),
      { top: 44 },
    ])
  })
})
