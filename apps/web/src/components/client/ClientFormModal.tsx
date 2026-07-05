import { useEffect, useState, type FormEvent } from 'react'
import type { Client } from '@fleetmgm/api'
import { useCreateClient, useUpdateClient } from '@fleetmgm/hooks'
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

type ClientFormModalProps = {
  open: boolean
  onOpenChange: (open: boolean) => void
  client?: Client
}

export function ClientFormModal({ open, onOpenChange, client }: ClientFormModalProps) {
  const isEditing = client != null
  const createClient = useCreateClient()
  const updateClient = useUpdateClient()

  const [name, setName] = useState('')
  const [taxId, setTaxId] = useState('')
  const [email, setEmail] = useState('')
  const [phone, setPhone] = useState('')
  const [address, setAddress] = useState('')

  useEffect(() => {
    if (!open) {
      return
    }
    setName(client?.name ?? '')
    setTaxId(client?.taxId ?? '')
    setEmail(client?.email ?? '')
    setPhone(client?.phone ?? '')
    setAddress(client?.address ?? '')
  }, [open, client])

  const isPending = createClient.isPending || updateClient.isPending

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const request = { name, taxId, email, phone, address }

    if (client) {
      updateClient.mutate({ id: client.id, request }, { onSuccess: () => onOpenChange(false) })
    } else {
      createClient.mutate(request, { onSuccess: () => onOpenChange(false) })
    }
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>{isEditing ? 'Editar cliente' : 'Nuevo cliente'}</DialogTitle>
        </DialogHeader>

        <form className="flex flex-col gap-4" onSubmit={handleSubmit}>
          <div className="flex flex-col gap-1.5">
            <Label htmlFor="client-name">Nombre</Label>
            <Input id="client-name" value={name} onChange={(e) => setName(e.target.value)} required />
          </div>

          <div className="flex flex-col gap-1.5">
            <Label htmlFor="client-tax-id">ID fiscal</Label>
            <Input id="client-tax-id" value={taxId} onChange={(e) => setTaxId(e.target.value)} required />
          </div>

          <div className="flex flex-col gap-1.5">
            <Label htmlFor="client-email">Correo electrónico</Label>
            <Input
              id="client-email"
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
            />
          </div>

          <div className="flex flex-col gap-1.5">
            <Label htmlFor="client-phone">Teléfono</Label>
            <Input id="client-phone" value={phone} onChange={(e) => setPhone(e.target.value)} />
          </div>

          <div className="flex flex-col gap-1.5">
            <Label htmlFor="client-address">Dirección</Label>
            <Input id="client-address" value={address} onChange={(e) => setAddress(e.target.value)} />
          </div>

          <DialogFooter>
            <Button type="submit" disabled={isPending}>
              {isEditing ? 'Guardar cambios' : 'Crear cliente'}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}
