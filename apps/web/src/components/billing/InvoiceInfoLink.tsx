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
        className="text-left font-medium text-secondary underline-offset-2 hover:underline"
        onClick={() => setOpen(true)}
      >
        {invoiceNumber}
      </button>
      <InvoiceFormModal open={open} onOpenChange={setOpen} invoice={invoice} readOnly />
    </>
  )
}
