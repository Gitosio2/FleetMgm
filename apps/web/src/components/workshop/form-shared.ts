import type { SchedulePriority } from '@fleetmgm/api'

export const selectClassName =
  'flex h-11 w-full rounded-lg border border-outline-variant bg-surface-container-lowest px-3 py-2 text-sm text-on-surface focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-secondary-container disabled:cursor-not-allowed disabled:opacity-50'

export function toNullableString(value: string): string | null {
  return value === '' ? null : value
}

export const PRIORITY_LABEL: Record<SchedulePriority, string> = {
  LOW: 'Baja',
  MEDIUM: 'Media',
  HIGH: 'Alta',
  URGENT: 'Urgente',
}
