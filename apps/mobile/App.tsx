import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { StatusBar } from 'expo-status-bar'
import { configureApiClient } from '@fleetmgm/api'
import { useEffect, useRef, useState } from 'react'
import { ActivityIndicator, Pressable, StyleSheet, Text, View } from 'react-native'
import { useLogout } from '@fleetmgm/hooks/mobile'
import { useAuthStore } from '@fleetmgm/store'
import { secureAuthStorage } from './src/auth/secureStorage'
import { resolveApiBaseUrl } from './src/config/api'
import { LoginScreen } from './src/screens/LoginScreen'

const queryClient = new QueryClient()

configureApiClient(resolveApiBaseUrl(process.env.EXPO_PUBLIC_API_BASE_URL, __DEV__))

export default function App() {
  const isAuthenticated = useAuthStore((state) => state.isAuthenticated)
  const [isHydrated, setIsHydrated] = useState(false)
  const hydrationStarted = useRef(false)

  useEffect(() => {
    if (hydrationStarted.current) {
      return
    }
    hydrationStarted.current = true
    let isMounted = true

    useAuthStore.persist.setOptions({ storage: secureAuthStorage })
    void Promise.resolve(useAuthStore.persist.rehydrate()).then(() => {
      if (isMounted) {
        setIsHydrated(true)
      }
    })

    return () => {
      isMounted = false
    }
  }, [])

  return (
    <QueryClientProvider client={queryClient}>
      <StatusBar style="dark" />
      {!isHydrated ? <HydrationScreen /> : isAuthenticated ? <AuthenticatedScreen /> : <LoginScreen />}
    </QueryClientProvider>
  )
}

function HydrationScreen() {
  return (
    <View style={styles.safeArea}>
      <View style={styles.container}>
        <ActivityIndicator accessibilityLabel="Cargando sesión" color="#2563eb" />
      </View>
    </View>
  )
}

function AuthenticatedScreen() {
  const logout = useLogout()

  return (
    <View style={styles.safeArea}>
      <View style={styles.container}>
        <Text style={styles.title}>Sesión iniciada</Text>
        <Text style={styles.description}>El inicio de sesión móvil se ha completado.</Text>
        <Pressable
          accessibilityLabel="Cerrar sesión"
          accessibilityRole="button"
          onPress={() => logout.mutate()}
          style={styles.logoutButton}
        >
          <Text style={styles.logoutButtonText}>Cerrar sesión</Text>
        </Pressable>
      </View>
    </View>
  )
}

const styles = StyleSheet.create({
  safeArea: {
    backgroundColor: '#f1f5f9',
    flex: 1,
  },
  container: {
    alignItems: 'center',
    flex: 1,
    justifyContent: 'center',
    padding: 24,
  },
  title: {
    color: '#0f172a',
    fontSize: 24,
    fontWeight: '700',
  },
  description: {
    color: '#475569',
    fontSize: 16,
    marginTop: 8,
    textAlign: 'center',
  },
  logoutButton: {
    borderColor: '#2563eb',
    borderRadius: 8,
    borderWidth: 1,
    marginTop: 24,
    paddingHorizontal: 16,
    paddingVertical: 12,
  },
  logoutButtonText: {
    color: '#1d4ed8',
    fontSize: 16,
    fontWeight: '700',
  },
})
