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
        className="text-left font-medium text-on-surface underline-offset-2 hover:underline"
        onClick={() => setOpen(true)}
      >
        {supplierInvoiceNumber}
      </button>
      {/* Mounted only while open: SupplierInvoiceFormModal unconditionally calls
          useAllVehicles()/useAllSuppliers() (fetch-every-page hooks) to populate its selects —
          always rendering it here would fire both on every dashboard load, before any invoice is
          ever clicked. */}
      {open && (
        <SupplierInvoiceFormModal open={open} onOpenChange={setOpen} supplierInvoice={supplierInvoice} readOnly />
      )}
    </>
  )
}
