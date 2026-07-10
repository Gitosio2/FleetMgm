import type { Job } from '@fleetmgm/api'
import { useCancelJob, useCompleteJob, useStartJob } from '@fleetmgm/hooks'
import { Button } from '@/components/ui/button'

type JobActionButtonsProps = {
  job: Job
}

export function JobActionButtons({ job }: JobActionButtonsProps) {
  const startJob = useStartJob()
  const completeJob = useCompleteJob()
  const cancelJob = useCancelJob()

  const isPending = startJob.isPending || completeJob.isPending || cancelJob.isPending
  const isError = startJob.isError || completeJob.isError || cancelJob.isError

  if (job.status === 'PENDING') {
    return (
      <div className="flex flex-col items-start gap-1">
        <div className="flex gap-1">
          <Button
            variant="default"
            size="sm"
            disabled={isPending}
            onClick={() => startJob.mutate({ id: job.id, startUsageValue: null })}
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
            onClick={() => completeJob.mutate({ id: job.id, endUsageValue: null })}
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
      </div>
    )
  }

  return null
}
