import '@testing-library/jest-dom/vitest'
// @ts-expect-error — Node builtin; this frontend project has no @types/node, and adding it just
// for this one test-setup import isn't worth the broader ambient-type surface it would open up.
import { Blob as NodeBlob } from 'node:buffer'
import { afterAll, afterEach, beforeAll } from 'vitest'
import { server } from '@/mocks/server'

// jsdom replaces the global Blob with an incomplete polyfill that has no .stream() method.
// MSW's node interceptors construct undici Response objects internally (e.g. for
// responseType: 'blob' XHR requests) and undici's extractBody() calls .stream() on the Blob
// it's given — with jsdom's Blob, that throws "TypeError: object.stream is not a function".
// Restoring Node's own Blob (the one undici itself is built around) avoids the mismatch.
globalThis.Blob = NodeBlob as unknown as typeof Blob

class ResizeObserverStub {
  observe() {}
  unobserve() {}
  disconnect() {}
}

globalThis.ResizeObserver ??= ResizeObserverStub

// jsdom has no layout engine, so it doesn't implement window.scrollTo — Leaflet calls it
// when centering the map, which otherwise logs a noisy "Not implemented" jsdom error.
window.scrollTo = () => {}

beforeAll(() => server.listen({ onUnhandledRequest: 'error' }))
afterEach(() => server.resetHandlers())
afterAll(() => server.close())
