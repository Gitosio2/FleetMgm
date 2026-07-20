import * as React from 'react'
import * as DialogPrimitive from '@radix-ui/react-dialog'
import { X } from 'lucide-react'

import { cn } from '@/lib/utils'

const Sheet = DialogPrimitive.Root
const SheetTrigger = DialogPrimitive.Trigger
const SheetClose = DialogPrimitive.Close

function SheetContent({
  className,
  children,
  ...props
}: React.ComponentProps<typeof DialogPrimitive.Content>) {
  return (
    <DialogPrimitive.Portal>
      <DialogPrimitive.Overlay className="fixed inset-0 z-50 bg-surface-container-lowest/70 backdrop-blur-sm" />
      <DialogPrimitive.Content
        className={cn(
          'fixed inset-y-0 left-0 z-50 flex h-full w-64 flex-col border-r border-outline-variant/40 bg-surface-container-lowest text-on-surface shadow-lg',
          className,
        )}
        {...props}
      >
        <DialogPrimitive.Title className="sr-only">Menú de navegación</DialogPrimitive.Title>
        {children}
        <DialogPrimitive.Close
          className="absolute right-3 top-3 text-on-surface-variant hover:text-on-surface"
          title="Cerrar menú"
        >
          <X className="size-4" />
          <span className="sr-only">Cerrar menú</span>
        </DialogPrimitive.Close>
      </DialogPrimitive.Content>
    </DialogPrimitive.Portal>
  )
}

export { Sheet, SheetTrigger, SheetContent, SheetClose }
