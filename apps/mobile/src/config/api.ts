export function resolveApiBaseUrl(value: string | undefined, isDevelopment: boolean): string {
  const configuredUrl = value?.trim()

  if (!configuredUrl) {
    throw new Error('EXPO_PUBLIC_API_BASE_URL must be configured')
  }

  let url: URL
  try {
    url = new URL(configuredUrl)
  } catch {
    throw new Error('EXPO_PUBLIC_API_BASE_URL must be a valid URL')
  }

  if (!url.hostname) {
    throw new Error('EXPO_PUBLIC_API_BASE_URL must include a host')
  }

  if (url.protocol === 'https:') {
    return url.toString().replace(/\/$/, '')
  }

  if (url.protocol === 'http:' && isDevelopment) {
    return url.toString().replace(/\/$/, '')
  }

  if (url.protocol === 'http:') {
    throw new Error('HTTPS is required for EXPO_PUBLIC_API_BASE_URL outside development')
  }

  throw new Error('EXPO_PUBLIC_API_BASE_URL must use HTTP or HTTPS')
}
