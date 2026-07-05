import { Outlet } from 'react-router-dom'
import { LogOut } from 'lucide-react'
import { useLogout } from '@fleetmgm/hooks'
import { useAuthStore } from '@fleetmgm/store'
import { Button } from '@/components/ui/button'
import { Sidebar } from './Sidebar'

export function Layout() {
  const email = useAuthStore((state) => state.email)
  const role = useAuthStore((state) => state.role)
  const logout = useLogout()

  return (
    <div className="flex min-h-screen bg-surface-dim text-on-surface">
      <Sidebar />

      <div className="flex min-w-0 flex-1 flex-col">
        <header className="flex items-center justify-between border-b border-outline-variant/40 px-8 py-4">
          <div>
            <p className="text-sm font-medium">{email}</p>
            <p className="text-xs text-on-surface-variant">{role}</p>
          </div>
          <Button variant="ghost" size="sm" onClick={() => logout.mutate()}>
            <LogOut />
            Cerrar sesión
          </Button>
        </header>

        <main className="flex-1 overflow-y-auto p-8">
          <Outlet />
        </main>
      </div>
    </div>
  )
}
