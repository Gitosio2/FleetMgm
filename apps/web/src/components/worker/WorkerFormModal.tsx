import { useEffect, useState, type FormEvent } from 'react'
import type { CreateWorkerRequest, Worker, WorkerRole } from '@fleetmgm/api'
import { useCreateWorker, useUpdateWorker } from '@fleetmgm/hooks'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { WORKER_ROLES, WORKER_ROLE_LABEL } from './worker-shared'

const selectClassName =
  'flex h-11 w-full rounded-lg border border-outline-variant bg-surface-container-lowest px-3 py-2 text-sm text-on-surface focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-secondary-container disabled:cursor-not-allowed disabled:opacity-50'

type WorkerFormModalProps = {
  open: boolean
  onOpenChange: (open: boolean) => void
  worker?: Worker
}

function toNullableString(value: string): string | null {
  return value === '' ? null : value
}

export function WorkerFormModal({ open, onOpenChange, worker }: WorkerFormModalProps) {
  const isEditing = worker != null
  const createWorker = useCreateWorker()
  const updateWorker = useUpdateWorker()

  const [firstName, setFirstName] = useState('')
  const [lastName, setLastName] = useState('')
  const [workerRole, setWorkerRole] = useState<WorkerRole>('DRIVER')
  const [nationalId, setNationalId] = useState('')
  const [phone, setPhone] = useState('')
  const [licenseType, setLicenseType] = useState('')
  const [licenseExpiry, setLicenseExpiry] = useState('')

  useEffect(() => {
    if (!open) {
      return
    }
    setFirstName(worker?.firstName ?? '')
    setLastName(worker?.lastName ?? '')
    setWorkerRole(worker?.workerRole ?? 'DRIVER')
    setNationalId(worker?.nationalId ?? '')
    setPhone(worker?.phone ?? '')
    setLicenseType(worker?.licenseType ?? '')
    setLicenseExpiry(worker?.licenseExpiry ?? '')
  }, [open, worker])

  const isPending = createWorker.isPending || updateWorker.isPending

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()

    if (worker) {
      updateWorker.mutate(
        {
          id: worker.id,
          request: {
            firstName,
            lastName,
            workerRole,
            phone: toNullableString(phone),
            licenseType: toNullableString(licenseType),
            licenseExpiry: toNullableString(licenseExpiry),
          },
        },
        { onSuccess: () => onOpenChange(false) },
      )
      return
    }

    const request: CreateWorkerRequest = {
      firstName,
      lastName,
      workerRole,
      nationalId,
      phone: toNullableString(phone),
      licenseType: toNullableString(licenseType),
      licenseExpiry: toNullableString(licenseExpiry),
    }
    createWorker.mutate(request, { onSuccess: () => onOpenChange(false) })
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-h-[85vh] max-w-2xl overflow-y-auto">
        <DialogHeader>
          <DialogTitle>{isEditing ? 'Editar trabajador' : 'Nuevo trabajador'}</DialogTitle>
        </DialogHeader>

        <form className="flex flex-col gap-4" onSubmit={handleSubmit}>
          <div className="grid grid-cols-2 gap-4">
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="worker-first-name">Nombre</Label>
              <Input
                id="worker-first-name"
                value={firstName}
                onChange={(e) => setFirstName(e.target.value)}
                required
              />
            </div>
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="worker-last-name">Apellidos</Label>
              <Input
                id="worker-last-name"
                value={lastName}
                onChange={(e) => setLastName(e.target.value)}
                required
              />
            </div>
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="worker-role">Rol</Label>
              <select
                id="worker-role"
                className={selectClassName}
                value={workerRole}
                onChange={(e) => setWorkerRole(e.target.value as WorkerRole)}
              >
                {WORKER_ROLES.map((role) => (
                  <option key={role} value={role}>
                    {WORKER_ROLE_LABEL[role]}
                  </option>
                ))}
              </select>
            </div>
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="worker-national-id">Documento de identidad</Label>
              <Input
                id="worker-national-id"
                value={nationalId}
                onChange={(e) => setNationalId(e.target.value)}
                required
                disabled={isEditing}
              />
            </div>
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="worker-phone">Teléfono</Label>
              <Input id="worker-phone" value={phone} onChange={(e) => setPhone(e.target.value)} />
            </div>
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="worker-license-type">Tipo de licencia</Label>
              <Input
                id="worker-license-type"
                value={licenseType}
                onChange={(e) => setLicenseType(e.target.value)}
              />
            </div>
          </div>

          <div className="flex flex-col gap-1.5">
            <Label htmlFor="worker-license-expiry">Vencimiento de licencia</Label>
            <Input
              id="worker-license-expiry"
              type="date"
              value={licenseExpiry}
              onChange={(e) => setLicenseExpiry(e.target.value)}
            />
          </div>

          <DialogFooter>
            <Button type="submit" disabled={isPending}>
              {isEditing ? 'Guardar cambios' : 'Crear trabajador'}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}
