import * as SecureStore from 'expo-secure-store'
import { secureAuthStorage } from './secureStorage'

jest.mock('expo-secure-store', () => ({
  WHEN_UNLOCKED_THIS_DEVICE_ONLY: 'WHEN_UNLOCKED_THIS_DEVICE_ONLY',
  deleteItemAsync: jest.fn(() => Promise.resolve()),
  getItemAsync: jest.fn(() => Promise.resolve(null)),
  setItemAsync: jest.fn(() => Promise.resolve()),
}))

describe('secureAuthStorage', () => {
  it('stores persisted auth data with device-only keychain access', async () => {
    await secureAuthStorage.setItem('fleetmgm-auth', {
      state: {
        accessToken: 'access-token',
        email: 'admin@fleetmgm.demo',
        isAuthenticated: true,
        login: jest.fn(),
        logout: jest.fn(),
        refreshToken: 'refresh-token',
        role: 'ADMIN',
        setAccessToken: jest.fn(),
      },
      version: 0,
    })

    expect(SecureStore.setItemAsync).toHaveBeenCalledWith(
      'fleetmgm-auth',
      expect.any(String),
      { keychainAccessible: SecureStore.WHEN_UNLOCKED_THIS_DEVICE_ONLY },
    )
  })
})
