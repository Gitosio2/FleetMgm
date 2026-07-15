import { useState } from 'react'
import { useSupplier } from '@fleetmgm/hooks'
import { SupplierFormModal } from './SupplierFormModal'

type SupplierInfoLinkProps = {
  supplierId: string
  supplierName: string
}

export function SupplierInfoLink({ supplierId, supplierName }: SupplierInfoLinkProps) {
  const [open, setOpen] = useState(false)
  const { data: supplier } = useSupplier(open ? supplierId : undefined)

  return (
    <>
      <button
        type="button"
        className="text-left text-secondary underline-offset-2 hover:underline"
        onClick={() => setOpen(true)}
      >
        {supplierName}
      </button>
      <SupplierFormModal open={open} onOpenChange={setOpen} supplier={supplier} readOnly />
    </>
  )
}
