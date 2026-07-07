import { useEffect, useState, type FormEvent } from 'react'
import type { Assignment, CreateAssignmentRequest, Worker } from '@fleetmgm/api'
import { useCreateAssignment } from '@fleetmgm/hooks'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog'

const selectClassName =
  'flex h-11 w-full rounded-lg border border-outline-variant bg-surface-container-lowest px-3 py-2 text-sm text-on-surface focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-secondary-container disabled:cursor-not-allowed disabled:opacity-50'

type AssignmentModalProps = {
  open: boolean
  onOpenChange: (open: boolean) => void
  vehicleId: string
  vehicleLabel: string
  drivers: Worker[]
  onAssigned: (assignment: Assignment) => void
}

export function AssignmentModal({
  open,
  onOpenChange,
  vehicleId,
  vehicleLabel,
  drivers,
  onAssigned,
}: AssignmentModalProps) {
  const createAssignment = useCreateAssignment()

  const [driverId, setDriverId] = useState('')
  const [startDate, setStartDate] = useState('')
  const [notes, setNotes] = useState('')

  useEffect(() => {
    if (!open) {
      return
    }
    setDriverId(drivers[0]?.id ?? '')
    setStartDate('')
    setNotes('')
  }, [open, drivers])

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()

    const request: CreateAssignmentRequest = {
      driverId,
      vehicleId,
      startDate,
      notes: notes === '' ? null : notes,
    }

    createAssignment.mutate(request, {
      onSuccess: (assignment) => {
        onAssigned(assignment)
        onOpenChange(false)
      },
    })
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-lg">
        <DialogHeader>
          <DialogTitle>Asignar conductor — {vehicleLabel}</DialogTitle>
        </DialogHeader>

        <form className="flex flex-col gap-4" onSubmit={handleSubmit}>
          <div className="flex flex-col gap-1.5">
            <Label htmlFor="assignment-driver">Conductor</Label>
            <select
              id="assignment-driver"
              className={selectClassName}
              value={driverId}
              onChange={(e) => setDriverId(e.target.value)}
              required
            >
              {drivers.map((driver) => (
                <option key={driver.id} value={driver.id}>
                  {driver.fullName}
                </option>
              ))}
            </select>
          </div>

          <div className="flex flex-col gap-1.5">
            <Label htmlFor="assignment-start-date">Fecha de inicio</Label>
            <Input
              id="assignment-start-date"
              type="date"
              value={startDate}
              onChange={(e) => setStartDate(e.target.value)}
              required
            />
          </div>

          <div className="flex flex-col gap-1.5">
            <Label htmlFor="assignment-notes">Notas</Label>
            <Input id="assignment-notes" value={notes} onChange={(e) => setNotes(e.target.value)} />
          </div>

          <DialogFooter>
            <Button type="submit" disabled={createAssignment.isPending || driverId === ''}>
              Asignar
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}
