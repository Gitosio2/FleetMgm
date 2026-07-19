import { useState } from 'react'
import { useSupplierInvoice } from '@fleetmgm/hooks'
import { SupplierInvoiceFormModal } from './SupplierInvoiceFormModal'

type SupplierInvoiceInfoLinkProps = {
  supplierInvoiceId: string
  supplierInvoiceNumber: string
}

export function SupplierInvoiceInfoLink({ supplierInvoiceId, supplierInvoiceNumber }: SupplierInvoiceInfoLinkProps) {
  const [open, setOpen] = useState(false)
  const { data: supplierInvoice } = useSupplierInvoice(open ? supplierInvoiceId : '')

  return (
    <>
      <button
        type="button"
        className="text-left font-medium text-secondary underline-offset-2 hover:underline"
        onClick={() => setOpen(true)}
      >
        {supplierInvoiceNumber}
      </button>
      <SupplierInvoiceFormModal open={open} onOpenChange={setOpen} supplierInvoice={supplierInvoice} readOnly />
    </>
  )
}
