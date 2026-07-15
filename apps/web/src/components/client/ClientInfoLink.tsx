import { useState } from 'react'
import { useClient } from '@fleetmgm/hooks'
import { ClientFormModal } from './ClientFormModal'

type ClientInfoLinkProps = {
  clientId: string
  clientName: string
}

export function ClientInfoLink({ clientId, clientName }: ClientInfoLinkProps) {
  const [open, setOpen] = useState(false)
  const { data: client } = useClient(open ? clientId : undefined)

  return (
    <>
      <button
        type="button"
        className="text-left text-secondary underline-offset-2 hover:underline"
        onClick={() => setOpen(true)}
      >
        {clientName}
      </button>
      <ClientFormModal open={open} onOpenChange={setOpen} client={client} readOnly />
    </>
  )
}
