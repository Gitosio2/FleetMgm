import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { useThemeStore } from '@fleetmgm/store'
import './index.css'
import App from './App.tsx'

const queryClient = new QueryClient()

// Applies the persisted (or default) theme before the first paint — zustand's persist middleware
// hydrates synchronously from localStorage at module load, so this read is safe outside React.
document.documentElement.dataset.theme = useThemeStore.getState().theme

async function enableMocking() {
  if (import.meta.env.VITE_ENABLE_MSW !== 'true') {
    return
  }

  const { worker } = await import('./mocks/browser')
  return worker.start({ onUnhandledRequest: 'bypass' })
}

enableMocking().then(() => {
  createRoot(document.getElementById('root')!).render(
    <StrictMode>
      <QueryClientProvider client={queryClient}>
        <BrowserRouter>
          <App />
        </BrowserRouter>
      </QueryClientProvider>
    </StrictMode>,
  )
})
