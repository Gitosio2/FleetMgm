import { resolveApiBaseUrl } from './api'

describe('resolveApiBaseUrl', () => {
  it('requires an explicitly configured API URL', () => {
    expect(() => resolveApiBaseUrl(undefined, true)).toThrow('EXPO_PUBLIC_API_BASE_URL')
  })

  it('allows an explicit HTTP development URL', () => {
    expect(resolveApiBaseUrl('http://10.0.2.2:8080/api/v1/', true)).toBe('http://10.0.2.2:8080/api/v1')
  })

  it('requires HTTPS outside development', () => {
    expect(() => resolveApiBaseUrl('http://api.example.test/api/v1', false)).toThrow('HTTPS')
  })

  it('accepts an HTTPS URL outside development', () => {
    expect(resolveApiBaseUrl('https://api.example.test/api/v1/', false)).toBe('https://api.example.test/api/v1')
  })

  it('rejects malformed URLs even when they use an allowed protocol prefix', () => {
    expect(() => resolveApiBaseUrl('https://', false)).toThrow('valid URL')
  })
})
