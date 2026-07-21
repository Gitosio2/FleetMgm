import {
  Building2,
  CalendarClock,
  ClipboardList,
  Contact2,
  LayoutDashboard,
  MapPin,
  Receipt,
  ShieldCheck,
  Store,
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

export const MANAGEMENT_ROLES: AppRole[] = ['ADMIN', 'MANAGER', 'ADMINISTRATIVE']

export const NAV_ITEMS: NavItem[] = [
  { label: 'Panel', to: '/dashboard', icon: LayoutDashboard, allowedRoles: MANAGEMENT_ROLES },
  {
    label: 'Trabajos',
    to: '/jobs',
    icon: ClipboardList,
    allowedRoles: [...MANAGEMENT_ROLES, 'DRIVER'],
  },
  {
    label: 'Agenda',
    to: '/workshop',
    icon: CalendarClock,
    allowedRoles: [...MANAGEMENT_ROLES, 'WORKSHOP_STAFF'],
  },
  {
    label: 'Órdenes de mantenimiento',
    to: '/maintenance-orders',
    icon: Wrench,
    allowedRoles: [...MANAGEMENT_ROLES, 'WORKSHOP_STAFF'],
  },
  { label: 'Facturación', to: '/billing', icon: Receipt, allowedRoles: MANAGEMENT_ROLES },
  { label: 'Gastos de proveedor', to: '/supplier-invoices', icon: Store, allowedRoles: MANAGEMENT_ROLES },
  { label: 'Proveedores', to: '/suppliers', icon: Contact2, allowedRoles: MANAGEMENT_ROLES },
  { label: 'Vehículos', to: '/vehicles', icon: Truck },
  { label: 'Trabajadores', to: '/workers', icon: Users },
  { label: 'Clientes', to: '/clients', icon: Building2, allowedRoles: MANAGEMENT_ROLES },
  { label: 'Mapa GPS', to: '/gps', icon: MapPin, allowedRoles: MANAGEMENT_ROLES },
  { label: 'Registro de auditoría', to: '/audit', icon: ShieldCheck, allowedRoles: ['ADMIN', 'MANAGER'] },
]
