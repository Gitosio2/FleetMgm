import { Navigate } from 'react-router-dom'
import { useAuthStore } from '@fleetmgm/store'
import { Landing } from './Landing'

export function Home() {
  const isAuthenticated = useAuthStore((state) => state.isAuthenticated)

  if (isAuthenticated) {
    return <Navigate to="/dashboard" replace />
  }

  return <Landing />
}
