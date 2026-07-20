import * as SecureStore from 'expo-secure-store'
import { type AuthState } from '@fleetmgm/store'
import { createJSONStorage, type PersistStorage, type StateStorage } from 'zustand/middleware'

const secureStateStorage: StateStorage = {
  getItem: (name) => SecureStore.getItemAsync(name),
  setItem: (name, value) => SecureStore.setItemAsync(name, value, {
    keychainAccessible: SecureStore.WHEN_UNLOCKED_THIS_DEVICE_ONLY,
  }),
  removeItem: (name) => SecureStore.deleteItemAsync(name),
}

const nativeAuthStorage = createJSONStorage<AuthState>(
  () => secureStateStorage,
)

if (!nativeAuthStorage) {
  throw new Error('Unable to initialize native authentication storage')
}

export const secureAuthStorage: PersistStorage<AuthState> = nativeAuthStorage
