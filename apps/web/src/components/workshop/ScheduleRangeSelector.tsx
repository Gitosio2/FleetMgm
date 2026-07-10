import type { ScheduleRange } from '@fleetmgm/api'
import { Button } from '@/components/ui/button'

const RANGE_OPTIONS: { value: ScheduleRange; label: string }[] = [
  { value: 'today', label: 'Hoy' },
  { value: 'week', label: 'Semana' },
  { value: 'month', label: 'Mes' },
]

type ScheduleRangeSelectorProps = {
  value: ScheduleRange
  onChange: (range: ScheduleRange) => void
}

export function ScheduleRangeSelector({ value, onChange }: ScheduleRangeSelectorProps) {
  return (
    <div className="flex gap-1" role="group" aria-label="Rango de la agenda">
      {RANGE_OPTIONS.map((option) => (
        <Button
          key={option.value}
          type="button"
          variant={value === option.value ? 'default' : 'outline'}
          size="sm"
          onClick={() => onChange(option.value)}
        >
          {option.label}
        </Button>
      ))}
    </div>
  )
}
