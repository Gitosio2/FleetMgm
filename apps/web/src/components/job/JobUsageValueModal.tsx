import { useEffect, useState, type FormEvent } from 'react'
import type { Job, UsageMeasure } from '@fleetmgm/api'
import { useVehicle } from '@fleetmgm/hooks'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog'

const USAGE_MEASURE_LABEL: Record<UsageMeasure, string> = {
  KILOMETERS: 'Kilómetros',
  HOURS: 'Horas',
}

const MODE_COPY = {
  start: { title: 'Iniciar trabajo', confirmLabel: 'Iniciar' },
  complete: { title: 'Completar trabajo', confirmLabel: 'Completar' },
} as const

type JobUsageValueModalProps = {
  open: boolean
  onOpenChange: (open: boolean) => void
  job: Job
  mode: 'start' | 'complete'
  onConfirm: (value: number | null) => void
  isPending: boolean
}

export function JobUsageValueModal({
  open,
  onOpenChange,
  job,
  mode,
  onConfirm,
  isPending,
}: JobUsageValueModalProps) {
  const { data: vehicle } = useVehicle(job.vehicleId)
  const [value, setValue] = useState('')

  useEffect(() => {
    if (!open) {
      return
    }
    setValue('')
  }, [open])

  const fieldLabel = vehicle ? `${USAGE_MEASURE_LABEL[vehicle.usageMeasure]} actuales` : 'Valor de uso actual'
  const currentValue = vehicle
    ? vehicle.usageMeasure === 'KILOMETERS'
      ? vehicle.currentKm
      : vehicle.currentHours
    : null

  const { title, confirmLabel } = MODE_COPY[mode]

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    onConfirm(value === '' ? null : Number(value))
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-md">
        <DialogHeader>
          <DialogTitle>{title}</DialogTitle>
        </DialogHeader>

        <form className="flex flex-col gap-4" onSubmit={handleSubmit}>
          <div className="flex flex-col gap-1.5">
            <Label htmlFor="job-usage-value">{fieldLabel}</Label>
            <Input
              id="job-usage-value"
              type="number"
              value={value}
              onChange={(e) => setValue(e.target.value)}
              placeholder={currentValue != null ? `Actual: ${currentValue}` : undefined}
            />
          </div>

          <DialogFooter>
            <Button
              type="button"
              variant="outline"
              disabled={isPending}
              onClick={() => onOpenChange(false)}
            >
              Cancelar
            </Button>
            <Button type="submit" disabled={isPending}>
              {confirmLabel}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}
