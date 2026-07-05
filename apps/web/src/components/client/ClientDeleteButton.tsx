import { Trash2 } from 'lucide-react'
import { useDeleteClient } from '@fleetmgm/hooks'
import { Button } from '@/components/ui/button'
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
  AlertDialogTrigger,
} from '@/components/ui/alert-dialog'

type ClientDeleteButtonProps = {
  clientId: string
  clientName: string
}

export function ClientDeleteButton({ clientId, clientName }: ClientDeleteButtonProps) {
  const deleteClient = useDeleteClient()

  return (
    <AlertDialog>
      <AlertDialogTrigger asChild>
        <Button variant="ghost" size="sm" aria-label="Delete client" disabled={deleteClient.isPending}>
          <Trash2 className="size-4" />
        </Button>
      </AlertDialogTrigger>
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>Delete {clientName}?</AlertDialogTitle>
          <AlertDialogDescription>
            This removes the client from active lists. This action cannot be undone from here.
          </AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <AlertDialogCancel>Cancel</AlertDialogCancel>
          <AlertDialogAction onClick={() => deleteClient.mutate(clientId)}>
            Delete
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  )
}
