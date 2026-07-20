import { useEffect, useState, type FormEvent } from 'react'
import type { Job, UsageMeasure } from '@fleetmgm/api'
import { useVehicle } from '@fleetmgm/hooks'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { JOB_ERROR_MESSAGES, resolveJobErrorMessage } from './job-shared'

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
  error?: unknown
}

export function JobUsageValueModal({
  open,
  onOpenChange,
  job,
  mode,
  onConfirm,
  isPending,
  error,
}: JobUsageValueModalProps) {
  const { data: vehicle } = useVehicle(job.vehicleId)
  const [value, setValue] = useState('')
  const [validationError, setValidationError] = useState<string | null>(null)

  useEffect(() => {
    if (!open) {
      return
    }
    setValue('')
    setValidationError(null)
  }, [open])

  const fieldLabel = vehicle ? `${USAGE_MEASURE_LABEL[vehicle.usageMeasure]} actuales` : 'Valor de uso actual'
  const currentValue = vehicle
    ? vehicle.usageMeasure === 'KILOMETERS'
      ? vehicle.currentKm
      : vehicle.currentHours
    : null

  // Only `complete` has a backend-enforced floor (JobService.assertUsageValueNotRegressing) —
  // `start` never validates startUsageValue against anything, so no client-side check applies there.
  const floor =
    mode === 'complete' ? Math.max(currentValue ?? -Infinity, job.startUsageValue ?? -Infinity) : -Infinity

  const { title, confirmLabel } = MODE_COPY[mode]
  const errorMessage = validationError ?? (error ? resolveJobErrorMessage(error) : null)

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()

    if (value === '') {
      onConfirm(null)
      return
    }

    const numericValue = Number(value)
    if (floor !== -Infinity && numericValue < floor) {
      setValidationError(JOB_ERROR_MESSAGES.JOB_USAGE_VALUE_BELOW_CURRENT!)
      return
    }

    onConfirm(numericValue)
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
              onChange={(e) => {
                setValue(e.target.value)
                setValidationError(null)
              }}
              placeholder={currentValue != null ? `Actual: ${currentValue}` : undefined}
            />
          </div>

          {errorMessage && (
            <p role="alert" className="text-sm text-error">
              {errorMessage}
            </p>
          )}

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
