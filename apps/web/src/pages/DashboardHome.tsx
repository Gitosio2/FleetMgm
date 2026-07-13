import { Navigate } from 'react-router-dom'
import { useAuthStore } from '@fleetmgm/store'
import { MANAGEMENT_ROLES } from '@/components/layout/nav-items'
import { Dashboard } from '@/pages/Dashboard'

// The index route ("/") is reachable by every authenticated role, but the KPI dashboard content
// is only meaningful for management roles. Rather than let ProtectedRoute show a 403 to
// DRIVER/WORKSHOP_STAFF on their post-login landing page, this component silently redirects them
// to the page they actually use day to day.
export function DashboardHome() {
  const role = useAuthStore((state) => state.role)

  if (role && MANAGEMENT_ROLES.includes(role)) {
    return <Dashboard />
  }

  if (role === 'DRIVER') {
    return <Navigate to="/jobs" replace />
  }

  if (role === 'WORKSHOP_STAFF') {
    return <Navigate to="/workshop" replace />
  }

  return <Dashboard />
}
