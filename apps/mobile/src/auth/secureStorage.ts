import * as SecureStore from 'expo-secure-store'
import { type AuthState } from '@fleetmgm/store'
import { createJSONStorage, type StateStorage } from 'zustand/middleware'

const secureStateStorage: StateStorage = {
  getItem: (name) => SecureStore.getItemAsync(name),
  setItem: (name, value) => SecureStore.setItemAsync(name, value),
  removeItem: (name) => SecureStore.deleteItemAsync(name),
}

export const secureAuthStorage = createJSONStorage<AuthState>(
  () => secureStateStorage,
)
