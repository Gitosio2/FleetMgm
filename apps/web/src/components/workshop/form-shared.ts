import type { MaintenanceCategory, SchedulePriority } from '@fleetmgm/api'

export const selectClassName =
  'flex h-11 w-full rounded-lg border border-outline-variant bg-surface-container-lowest px-3 py-2 text-sm text-on-surface focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-secondary-container disabled:cursor-not-allowed disabled:opacity-50'

export function toNullableString(value: string): string | null {
  return value === '' ? null : value
}

// HTML <input type="time"> emits/accepts "HH:mm" (no seconds); the backend's LocalTime fields
// expect "HH:mm:ss". Appends the missing seconds, or returns null for an empty input.
export function toNullableTime(value: string): string | null {
  return value === '' ? null : `${value}:00`
}

// Normalizes a "HH:mm:ss" value from the API down to "HH:mm" for prefilling an
// <input type="time">. Real browsers sanitize an "HH:mm:ss" value string down to "HH:mm"
// automatically for display, but jsdom (used in tests) does not — normalizing explicitly here
// keeps the component's state format consistent (always "HH:mm") across environments, instead of
// depending on browser-specific input sanitization.
export function toTimeInputValue(value: string | null): string {
  return value ? value.slice(0, 5) : ''
}

export const PRIORITY_LABEL: Record<SchedulePriority, string> = {
  LOW: 'Baja',
  MEDIUM: 'Media',
  HIGH: 'Alta',
  URGENT: 'Urgente',
}

export const CATEGORY_LABEL: Record<MaintenanceCategory, string> = {
  PREVENTIVE: 'Preventivo',
  CORRECTIVE: 'Correctivo',
}
