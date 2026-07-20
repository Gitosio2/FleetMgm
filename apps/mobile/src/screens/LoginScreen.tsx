import { useState } from 'react'
import {
  ActivityIndicator,
  Pressable,
  StyleSheet,
  Text,
  TextInput,
  View,
} from 'react-native'
import { isAxiosError } from 'axios'
import { useLogin } from '@fleetmgm/hooks'

export function LoginScreen() {
  const login = useLogin()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const canSubmit = email.trim().length > 0 && password.length > 0 && !login.isPending

  function handleSubmit() {
    if (!canSubmit) {
      return
    }

    login.mutate(
      { email: email.trim(), password },
    )
  }

  const showInvalidCredentials = login.isError && isAxiosError(login.error) && login.error.response?.status === 401
  const showConnectionError = login.isError && !showInvalidCredentials

  return (
    <View style={styles.safeArea}>
      <View style={styles.container}>
        <View style={styles.card}>
          <Text style={styles.brand}>FleetMgm</Text>
          <Text style={styles.title}>Inicio de sesión seguro</Text>
          <Text style={styles.description}>Ingresa tus credenciales institucionales.</Text>

          <View style={styles.field}>
            <Text style={styles.label}>Correo electrónico</Text>
            <TextInput
              accessibilityLabel="Correo electrónico"
              autoCapitalize="none"
              autoComplete="email"
              keyboardType="email-address"
              onChangeText={setEmail}
              placeholder="operador@empresa.com"
              placeholderTextColor="#64748b"
              style={styles.input}
              textContentType="emailAddress"
              value={email}
            />
          </View>

          <View style={styles.field}>
            <Text style={styles.label}>Contraseña</Text>
            <TextInput
              accessibilityLabel="Contraseña"
              autoComplete="current-password"
              onChangeText={setPassword}
              placeholder="Contraseña"
              placeholderTextColor="#64748b"
              secureTextEntry
              style={styles.input}
              textContentType="password"
              value={password}
            />
          </View>

          {showInvalidCredentials && (
            <Text accessibilityRole="alert" style={styles.error}>
              Credenciales inválidas
            </Text>
          )}

          {showConnectionError && (
            <Text accessibilityRole="alert" style={styles.error}>
              No se pudo conectar al servidor. Inténtalo de nuevo.
            </Text>
          )}

          <Pressable
            accessibilityLabel="Iniciar sesión"
            accessibilityRole="button"
            accessibilityState={{ disabled: !canSubmit }}
            disabled={!canSubmit}
            onPress={handleSubmit}
            style={({ pressed }) => [
              styles.submitButton,
              !canSubmit && styles.submitButtonDisabled,
              pressed && canSubmit && styles.submitButtonPressed,
            ]}
          >
            {login.isPending ? (
              <ActivityIndicator accessibilityLabel="Iniciando sesión" color="#ffffff" />
            ) : (
              <Text style={styles.submitButtonText}>Iniciar sesión</Text>
            )}
          </Pressable>
        </View>
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
  card: {
    backgroundColor: '#ffffff',
    borderColor: '#e2e8f0',
    borderRadius: 16,
    borderWidth: 1,
    maxWidth: 440,
    padding: 24,
    shadowColor: '#0f172a',
    shadowOffset: { width: 0, height: 8 },
    shadowOpacity: 0.1,
    shadowRadius: 16,
    width: '100%',
  },
  brand: {
    color: '#2563eb',
    fontSize: 18,
    fontWeight: '700',
    marginBottom: 20,
  },
  title: {
    color: '#0f172a',
    fontSize: 24,
    fontWeight: '700',
  },
  description: {
    color: '#475569',
    fontSize: 15,
    lineHeight: 22,
    marginBottom: 24,
    marginTop: 8,
  },
  field: {
    gap: 8,
    marginBottom: 16,
  },
  label: {
    color: '#1e293b',
    fontSize: 14,
    fontWeight: '600',
  },
  input: {
    borderColor: '#cbd5e1',
    borderRadius: 8,
    borderWidth: 1,
    color: '#0f172a',
    fontSize: 16,
    minHeight: 48,
    paddingHorizontal: 14,
  },
  error: {
    color: '#b91c1c',
    fontSize: 14,
    marginBottom: 16,
  },
  submitButton: {
    alignItems: 'center',
    backgroundColor: '#2563eb',
    borderRadius: 8,
    justifyContent: 'center',
    minHeight: 48,
    paddingHorizontal: 16,
  },
  submitButtonDisabled: {
    backgroundColor: '#94a3b8',
  },
  submitButtonPressed: {
    backgroundColor: '#1d4ed8',
  },
  submitButtonText: {
    color: '#ffffff',
    fontSize: 16,
    fontWeight: '700',
  },
})
