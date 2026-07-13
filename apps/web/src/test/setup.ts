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

// jsdom has no layout engine, so Element.getBoundingClientRect() always returns an all-zero rect.
// Recharts' <ResponsiveContainer> reads it (on its own outer wrapper div) to size the chart and
// refuses to render any children below a positive width/height. The fix is scoped to that one
// wrapper class: recharts also calls getBoundingClientRect internally on nested nodes (legend,
// tooltip, axis ticks) to auto-measure their own footprint — if those reported the same 800x400
// as the whole container, recharts would reserve all available space for e.g. the legend and
// collapse the actual plot area to zero height. Leaving those at jsdom's natural zero rect keeps
// that internal layout math sane.
const zeroRect = { width: 0, height: 0, top: 0, left: 0, bottom: 0, right: 0, x: 0, y: 0, toJSON() {} }
Element.prototype.getBoundingClientRect = function (this: Element) {
  if (this.classList.contains('recharts-responsive-container')) {
    return { width: 800, height: 400, top: 0, left: 0, bottom: 400, right: 800, x: 0, y: 0, toJSON() {} }
  }
  return zeroRect
}

// jsdom has no layout engine, so it doesn't implement window.scrollTo — Leaflet calls it
// when centering the map, which otherwise logs a noisy "Not implemented" jsdom error.
window.scrollTo = () => {}

beforeAll(() => server.listen({ onUnhandledRequest: 'error' }))
afterEach(() => server.resetHandlers())
afterAll(() => server.close())
