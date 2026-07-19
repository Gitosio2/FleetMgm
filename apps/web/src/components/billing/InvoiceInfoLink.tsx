import { useState } from 'react'
import { useInvoice } from '@fleetmgm/hooks'
import { InvoiceFormModal } from './InvoiceFormModal'

type InvoiceInfoLinkProps = {
  invoiceId: string
  invoiceNumber: string
}

export function InvoiceInfoLink({ invoiceId, invoiceNumber }: InvoiceInfoLinkProps) {
  const [open, setOpen] = useState(false)
  const { data: invoice } = useInvoice(open ? invoiceId : '')

  return (
    <>
      <button
        type="button"
        className="text-left font-medium text-on-surface underline-offset-2 hover:underline"
        onClick={() => setOpen(true)}
      >
        {invoiceNumber}
      </button>
      {/* Mounted only while open: InvoiceFormModal unconditionally calls useAllClients() (a
          fetch-every-page hook) to populate its client <select> — always rendering it here would
          fire that on every dashboard load, before any invoice is ever clicked. */}
      {open && <InvoiceFormModal open={open} onOpenChange={setOpen} invoice={invoice} readOnly />}
    </>
  )
}
