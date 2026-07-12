import { useEffect, useState, type FormEvent } from 'react'
import type { Supplier } from '@fleetmgm/api'
import { useCreateSupplier, useUpdateSupplier } from '@fleetmgm/hooks'
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

type SupplierFormModalProps = {
  open: boolean
  onOpenChange: (open: boolean) => void
  supplier?: Supplier
}

export function SupplierFormModal({ open, onOpenChange, supplier }: SupplierFormModalProps) {
  const isEditing = supplier != null
  const createSupplier = useCreateSupplier()
  const updateSupplier = useUpdateSupplier()

  const [name, setName] = useState('')
  const [taxId, setTaxId] = useState('')
  const [email, setEmail] = useState('')
  const [phone, setPhone] = useState('')
  const [address, setAddress] = useState('')

  useEffect(() => {
    if (!open) {
      return
    }
    setName(supplier?.name ?? '')
    setTaxId(supplier?.taxId ?? '')
    setEmail(supplier?.email ?? '')
    setPhone(supplier?.phone ?? '')
    setAddress(supplier?.address ?? '')
  }, [open, supplier])

  const isPending = createSupplier.isPending || updateSupplier.isPending

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const request = { name, taxId: taxId === '' ? null : taxId, email, phone, address }

    if (supplier) {
      updateSupplier.mutate({ id: supplier.id, request }, { onSuccess: () => onOpenChange(false) })
    } else {
      createSupplier.mutate(request, { onSuccess: () => onOpenChange(false) })
    }
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>{isEditing ? 'Editar proveedor' : 'Nuevo proveedor'}</DialogTitle>
        </DialogHeader>

        <form className="flex flex-col gap-4" onSubmit={handleSubmit}>
          <div className="flex flex-col gap-1.5">
            <Label htmlFor="supplier-name">Nombre</Label>
            <Input id="supplier-name" value={name} onChange={(e) => setName(e.target.value)} required />
          </div>

          <div className="flex flex-col gap-1.5">
            <Label htmlFor="supplier-tax-id">NIF</Label>
            <Input id="supplier-tax-id" value={taxId} onChange={(e) => setTaxId(e.target.value)} />
          </div>

          <div className="flex flex-col gap-1.5">
            <Label htmlFor="supplier-email">Correo electrónico</Label>
            <Input
              id="supplier-email"
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
            />
          </div>

          <div className="flex flex-col gap-1.5">
            <Label htmlFor="supplier-phone">Teléfono</Label>
            <Input id="supplier-phone" value={phone} onChange={(e) => setPhone(e.target.value)} />
          </div>

          <div className="flex flex-col gap-1.5">
            <Label htmlFor="supplier-address">Dirección</Label>
            <Input id="supplier-address" value={address} onChange={(e) => setAddress(e.target.value)} />
          </div>

          <DialogFooter>
            <Button type="submit" disabled={isPending}>
              {isEditing ? 'Guardar cambios' : 'Crear proveedor'}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}
