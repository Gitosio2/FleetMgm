import type { ReactNode } from 'react'
import { Navigate } from 'react-router-dom'
import { useAuthStore } from '@fleetmgm/store'

type ProtectedRouteProps = {
  children: ReactNode
  allowedRoles?: string[]
}

export function ProtectedRoute({ children, allowedRoles }: ProtectedRouteProps) {
  const isAuthenticated = useAuthStore((state) => state.isAuthenticated)
  const role = useAuthStore((state) => state.role)

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />
  }

  if (allowedRoles && (!role || !allowedRoles.includes(role))) {
    return (
      <div role="alert" className="flex h-full flex-col items-center justify-center gap-2 p-12 text-center">
        <h1 className="font-display text-2xl font-semibold">403 — Acceso denegado</h1>
        <p className="text-on-surface-variant">No tienes permiso para ver esta página.</p>
      </div>
    )
  }

  return <>{children}</>
}
