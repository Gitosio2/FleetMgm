import { FleetMap } from '@/components/map/FleetMap'

export function Map() {
  return (
    <div className="flex h-full flex-col gap-6">
      <div>
        <h1 className="font-display text-2xl font-semibold">Mapa GPS</h1>
        <p className="text-on-surface-variant">Ubicación en tiempo real de la flota.</p>
      </div>
      <FleetMap />
    </div>
  )
}
