import { useState } from 'react'
import { Outlet } from 'react-router-dom'
import { LogOut, Menu } from 'lucide-react'
import { useLogout } from '@fleetmgm/hooks'
import { useAuthStore } from '@fleetmgm/store'
import { Button } from '@/components/ui/button'
import { Sheet, SheetContent } from '@/components/ui/sheet'
import { Sidebar } from './Sidebar'

export function Layout() {
  const email = useAuthStore((state) => state.email)
  const role = useAuthStore((state) => state.role)
  const logout = useLogout()
  const [mobileNavOpen, setMobileNavOpen] = useState(false)

  return (
    <div className="flex h-screen bg-surface-dim text-on-surface">
      <Sidebar className="hidden lg:flex" />

      <Sheet open={mobileNavOpen} onOpenChange={setMobileNavOpen}>
        <SheetContent>
          <Sidebar onNavigate={() => setMobileNavOpen(false)} />
        </SheetContent>
      </Sheet>

      <div className="flex min-w-0 flex-1 flex-col overflow-hidden">
        <header className="shrink-0 flex items-center justify-between gap-3 border-b border-outline-variant/40 bg-surface-container-lowest px-4 py-3 sm:px-6 lg:px-8 lg:py-4">
          <div className="flex min-w-0 flex-1 items-center gap-3">
            <Button
              variant="ghost"
              size="sm"
              className="lg:hidden"
              onClick={() => setMobileNavOpen(true)}
            >
              <Menu />
              <span className="sr-only">Abrir menú</span>
            </Button>
            <div className="min-w-0">
              <p className="truncate text-sm font-medium">{email}</p>
              <p className="truncate text-xs text-on-surface-variant">{role}</p>
            </div>
          </div>
          <Button variant="ghost" size="sm" className="shrink-0" onClick={() => logout.mutate()}>
            <LogOut />
            Cerrar sesión
          </Button>
        </header>

        <main className="flex-1 overflow-y-auto p-4 sm:p-6 lg:p-8">
          <Outlet />
        </main>
      </div>
    </div>
  )
}
