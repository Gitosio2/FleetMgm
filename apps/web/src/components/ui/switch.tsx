import * as React from 'react'
import * as SwitchPrimitive from '@radix-ui/react-switch'

import { cn } from '@/lib/utils'

function Switch({
  className,
  ...props
}: React.ComponentProps<typeof SwitchPrimitive.Root>) {
  return (
    <SwitchPrimitive.Root
      data-slot="switch"
      className={cn(
        'peer inline-flex h-6 w-11 shrink-0 items-center rounded-full border border-outline-variant bg-surface-container-high transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-secondary-container disabled:cursor-not-allowed disabled:opacity-50 data-[state=checked]:border-secondary-container data-[state=checked]:bg-secondary-container',
        className,
      )}
      {...props}
    >
      <SwitchPrimitive.Thumb
        data-slot="switch-thumb"
        className="pointer-events-none block size-5 translate-x-0.5 rounded-full bg-surface-container-lowest shadow-md transition-transform data-[state=checked]:translate-x-5"
      />
    </SwitchPrimitive.Root>
  )
}

export { Switch }
