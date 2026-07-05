import {
  Building2,
  ClipboardList,
  LayoutDashboard,
  MapPin,
  Receipt,
  ShieldCheck,
  TrendingUp,
  Truck,
  Users,
  Wrench,
  type LucideIcon,
} from 'lucide-react'
import type { AppRole } from '@fleetmgm/store'

export type NavItem = {
  label: string
  to: string
  icon: LucideIcon
  allowedRoles?: AppRole[]
}

const MANAGEMENT_ROLES: AppRole[] = ['ADMIN', 'MANAGER', 'ADMINISTRATIVE']

export const NAV_ITEMS: NavItem[] = [
  { label: 'Dashboard', to: '/', icon: LayoutDashboard },
  { label: 'Vehicles', to: '/vehicles', icon: Truck },
  { label: 'Workers', to: '/workers', icon: Users },
  { label: 'Clients', to: '/clients', icon: Building2, allowedRoles: MANAGEMENT_ROLES },
  {
    label: 'Jobs',
    to: '/jobs',
    icon: ClipboardList,
    allowedRoles: [...MANAGEMENT_ROLES, 'DRIVER'],
  },
  {
    label: 'Workshop',
    to: '/workshop',
    icon: Wrench,
    allowedRoles: [...MANAGEMENT_ROLES, 'WORKSHOP_STAFF'],
  },
  { label: 'Billing', to: '/billing', icon: Receipt, allowedRoles: MANAGEMENT_ROLES },
  { label: 'GPS Map', to: '/gps', icon: MapPin, allowedRoles: MANAGEMENT_ROLES },
  { label: 'Reports', to: '/reports', icon: TrendingUp, allowedRoles: MANAGEMENT_ROLES },
  { label: 'Audit Log', to: '/audit', icon: ShieldCheck, allowedRoles: ['ADMIN', 'MANAGER'] },
]
