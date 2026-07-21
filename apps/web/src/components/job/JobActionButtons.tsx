import { useState } from 'react'
import { Check, Play, X } from 'lucide-react'
import type { Job } from '@fleetmgm/api'
import { useCancelJob, useCompleteJob, useStartJob } from '@fleetmgm/hooks'
import { Button } from '@/components/ui/button'
import { JobUsageValueModal } from './JobUsageValueModal'

type JobActionButtonsProps = {
  job: Job
}

export function JobActionButtons({ job }: JobActionButtonsProps) {
  const startJob = useStartJob()
  const completeJob = useCompleteJob()
  const cancelJob = useCancelJob()
  const [usageModalMode, setUsageModalMode] = useState<'start' | 'complete' | null>(null)

  const isPending = startJob.isPending || completeJob.isPending || cancelJob.isPending
  // Start/complete errors render inside JobUsageValueModal itself now — this alert is
  // cancel-only, since "Cancelar" doesn't go through a modal.
  const isError = cancelJob.isError

  function handleConfirmUsageValue(value: number | null) {
    if (usageModalMode === 'start') {
      startJob.mutate(
        { id: job.id, startUsageValue: value },
        { onSuccess: () => setUsageModalMode(null) },
      )
    } else if (usageModalMode === 'complete') {
      completeJob.mutate(
        { id: job.id, endUsageValue: value },
        { onSuccess: () => setUsageModalMode(null) },
      )
    }
  }

  const usageMutationPending = usageModalMode === 'start' ? startJob.isPending : completeJob.isPending

  const usageModal = (
    <JobUsageValueModal
      open={usageModalMode != null}
      onOpenChange={(open) => {
        // Ignore close attempts (X, Escape, outside click) while the request is still in
        // flight — otherwise a failure that arrives after the user closed the dialog has
        // nowhere left to render (the row-level alert below is cancel-only), leaving the job
        // unchanged with no visible feedback at all.
        if (!open && usageMutationPending) {
          return
        }
        setUsageModalMode(open ? usageModalMode : null)
      }}
      job={job}
      mode={usageModalMode ?? 'start'}
      onConfirm={handleConfirmUsageValue}
      isPending={usageMutationPending}
      error={usageModalMode === 'start' ? startJob.error : completeJob.error}
    />
  )

  if (job.status === 'PENDING') {
    return (
      <div className="flex flex-col items-start gap-1">
        <div className="flex gap-1">
          <Button
            variant="success"
            size="sm"
            aria-label="Iniciar trabajo"
            title="Iniciar trabajo"
            disabled={isPending}
            onClick={() => setUsageModalMode('start')}
          >
            <Play className="size-4" />
          </Button>
          <Button
            variant="destructive"
            size="sm"
            aria-label="Cancelar trabajo"
            title="Cancelar trabajo"
            disabled={isPending}
            onClick={() => cancelJob.mutate(job.id)}
          >
            <X className="size-4" />
          </Button>
        </div>
        {isError && (
          <p role="alert" className="text-sm text-error">
            No se pudo completar la acción.
          </p>
        )}
        {usageModal}
      </div>
    )
  }

  if (job.status === 'IN_PROGRESS') {
    return (
      <div className="flex flex-col items-start gap-1">
        <div className="flex gap-1">
          <Button
            variant="secondary"
            size="sm"
            aria-label="Completar trabajo"
            title="Completar trabajo"
            disabled={isPending}
            onClick={() => setUsageModalMode('complete')}
          >
            <Check className="size-4" />
          </Button>
          <Button
            variant="destructive"
            size="sm"
            aria-label="Cancelar trabajo"
            title="Cancelar trabajo"
            disabled={isPending}
            onClick={() => cancelJob.mutate(job.id)}
          >
            <X className="size-4" />
          </Button>
        </div>
        {isError && (
          <p role="alert" className="text-sm text-error">
            No se pudo completar la acción.
          </p>
        )}
        {usageModal}
      </div>
    )
  }

  return null
}
