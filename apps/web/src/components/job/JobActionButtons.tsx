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

  if (job.status === 'PENDING') {
    return (
      <div className="flex gap-1">
        <Button
          variant="ghost"
          size="sm"
          disabled={isPending}
          onClick={() => startJob.mutate({ id: job.id, startUsageValue: null })}
        >
          Iniciar
        </Button>
        <Button
          variant="ghost"
          size="sm"
          disabled={isPending}
          onClick={() => cancelJob.mutate(job.id)}
        >
          Cancelar
        </Button>
      </div>
    )
  }

  if (job.status === 'IN_PROGRESS') {
    return (
      <div className="flex gap-1">
        <Button
          variant="ghost"
          size="sm"
          disabled={isPending}
          onClick={() => completeJob.mutate({ id: job.id, endUsageValue: null })}
        >
          Completar
        </Button>
        <Button
          variant="ghost"
          size="sm"
          disabled={isPending}
          onClick={() => cancelJob.mutate(job.id)}
        >
          Cancelar
        </Button>
      </div>
    )
  }

  return null
}
