import { useEffect, useState } from 'react'
import type { GpsMockStatus } from '@fleetmgm/api'
import { useGpsMockStatus, useSetGpsMock } from '@fleetmgm/hooks'
import { useAuthStore, type AppRole } from '@fleetmgm/store'
import { Switch } from '@/components/ui/switch'

// Turning the generator on costs money on a metered host, so the switch is narrower than the map
// itself: ADMINISTRATIVE can watch the fleet move, but not decide that it should.
const TOGGLE_ROLES: AppRole[] = ['ADMIN', 'MANAGER']

const COUNTDOWN_REFRESH_MS = 30_000

/**
 * Counts down locally instead of re-asking the server, so an open Mapa GPS page sends no background
 * traffic while the generator is off.
 */
function useMinutesRemaining(enabledUntil: string | null | undefined) {
  const [now, setNow] = useState(() => Date.now())

  useEffect(() => {
    if (!enabledUntil) {
      return
    }
    // Re-read the clock as soon as a new deadline arrives, not just on the next tick: the snapshot
    // taken when the page mounted is already stale by then, which rounded a fresh 30-minute window
    // up to 31.
    setNow(Date.now())
    const interval = setInterval(() => setNow(Date.now()), COUNTDOWN_REFRESH_MS)
    return () => clearInterval(interval)
  }, [enabledUntil])

  if (!enabledUntil) {
    return null
  }
  const remainingMs = new Date(enabledUntil).getTime() - now
  return remainingMs <= 0 ? 0 : Math.ceil(remainingMs / 60_000)
}

function describeState(
  status: GpsMockStatus | undefined,
  canToggle: boolean,
  minutesRemaining: number | null,
): string {
  const enabled = status?.enabled ?? false

  if (!canToggle) {
    return enabled
      ? 'Activa. Solo un ADMIN o MANAGER puede detenerla.'
      : 'Inactiva. Solo un ADMIN o MANAGER puede activarla.'
  }
  if (!enabled) {
    return 'Apagada para no consumir recursos. El mapa muestra las últimas posiciones registradas.'
  }

  const frequency = `Generando posiciones cada ${status?.intervalSeconds ?? 30} s`
  return minutesRemaining == null
    ? `${frequency}.`
    : `${frequency} · se apaga sola en ${minutesRemaining} min.`
}

export function GpsMockToggle() {
  const role = useAuthStore((state) => state.role)
  const { data: status, isPending } = useGpsMockStatus()
  const { mutate: setGpsMock, isPending: isSaving, isError } = useSetGpsMock()
  const minutesRemaining = useMinutesRemaining(status?.enabledUntil)

  const canToggle = role != null && TOGGLE_ROLES.includes(role)

  return (
    <div
      data-testid="gps-mock-toggle"
      className="flex flex-col gap-1 rounded-lg border border-outline-variant bg-surface-container-lowest px-4 py-2"
    >
      <div className="flex items-center gap-3">
        <Switch
          id="gps-mock-toggle"
          aria-label="Simulación GPS"
          checked={status?.enabled ?? false}
          disabled={isPending || isSaving || !canToggle}
          onCheckedChange={(checked) => setGpsMock(checked)}
        />
        <label htmlFor="gps-mock-toggle" className="text-sm font-medium text-on-surface">
          Simulación GPS
        </label>
      </div>
      <p className="text-xs text-on-surface-variant">
        {describeState(status, canToggle, minutesRemaining)}
      </p>
      {isError && (
        <p role="alert" className="text-xs text-error">
          No se pudo cambiar la simulación GPS.
        </p>
      )}
    </div>
  )
}
