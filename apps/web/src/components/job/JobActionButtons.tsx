import { useState } from 'react'
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
  const isError = startJob.isError || completeJob.isError || cancelJob.isError

  function handleConfirmUsageValue(value: number | null) {
    // onSettled, not onSuccess — closing only on success left the modal open (and the
    // row's error alert stuck behind Radix's aria-hidden background overlay, effectively
    // invisible/inaccessible) on failure. Closing either way surfaces the row's existing
    // alert, matching how the pre-modal single-button flow always kept the alert visible.
    if (usageModalMode === 'start') {
      startJob.mutate(
        { id: job.id, startUsageValue: value },
        { onSettled: () => setUsageModalMode(null) },
      )
    } else if (usageModalMode === 'complete') {
      completeJob.mutate(
        { id: job.id, endUsageValue: value },
        { onSettled: () => setUsageModalMode(null) },
      )
    }
  }

  const usageModal = (
    <JobUsageValueModal
      open={usageModalMode != null}
      onOpenChange={(open) => setUsageModalMode(open ? usageModalMode : null)}
      job={job}
      mode={usageModalMode ?? 'start'}
      onConfirm={handleConfirmUsageValue}
      isPending={usageModalMode === 'start' ? startJob.isPending : completeJob.isPending}
    />
  )

  if (job.status === 'PENDING') {
    return (
      <div className="flex flex-col items-start gap-1">
        <div className="flex gap-1">
          <Button
            variant="default"
            size="sm"
            disabled={isPending}
            onClick={() => setUsageModalMode('start')}
          >
            Iniciar
          </Button>
          <Button
            variant="outline"
            size="sm"
            disabled={isPending}
            onClick={() => cancelJob.mutate(job.id)}
          >
            Cancelar
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
            variant="default"
            size="sm"
            disabled={isPending}
            onClick={() => setUsageModalMode('complete')}
          >
            Completar
          </Button>
          <Button
            variant="outline"
            size="sm"
            disabled={isPending}
            onClick={() => cancelJob.mutate(job.id)}
          >
            Cancelar
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
