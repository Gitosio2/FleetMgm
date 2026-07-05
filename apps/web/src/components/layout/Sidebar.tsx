import { NavLink } from 'react-router-dom'
import { Truck } from 'lucide-react'
import { useAuthStore } from '@fleetmgm/store'
import { cn } from '@/lib/utils'
import { NAV_ITEMS } from './nav-items'

export function Sidebar() {
  const role = useAuthStore((state) => state.role)

  const visibleItems = NAV_ITEMS.filter(
    (item) => !item.allowedRoles || (role && item.allowedRoles.includes(role)),
  )

  return (
    <nav className="flex h-screen w-64 shrink-0 flex-col border-r border-outline-variant/40 bg-surface-container-lowest">
      <div className="flex items-center gap-2 px-6 py-6">
        <span className="flex size-9 items-center justify-center rounded-lg bg-primary-container text-primary">
          <Truck className="size-5" />
        </span>
        <div>
          <p className="font-display text-sm font-semibold leading-tight">Fleet Manager Pro</p>
          <p className="text-xs text-on-surface-variant">Logística Empresarial</p>
        </div>
      </div>

      <ul className="flex flex-1 flex-col gap-1 px-3">
        {visibleItems.map(({ label, to, icon: Icon }) => (
          <li key={to}>
            <NavLink
              to={to}
              end={to === '/'}
              className={({ isActive }) =>
                cn(
                  'flex items-center gap-3 rounded-lg px-3 py-2 text-sm font-medium text-on-surface-variant transition-colors hover:bg-surface-container-high',
                  isActive && 'bg-secondary-container/10 text-secondary-container',
                )
              }
            >
              <Icon className="size-4" />
              {label}
            </NavLink>
          </li>
        ))}
      </ul>
    </nav>
  )
}
