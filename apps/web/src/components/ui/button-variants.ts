import { cva } from 'class-variance-authority'

export const buttonVariants = cva(
  "inline-flex items-center justify-center gap-2 whitespace-nowrap rounded-lg text-sm font-medium transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-secondary-container disabled:pointer-events-none disabled:opacity-50 [&_svg]:pointer-events-none [&_svg]:size-4 [&_svg]:shrink-0",
  {
    variants: {
      variant: {
        default: 'bg-primary text-on-primary hover:opacity-90',
        outline:
          'border border-outline-variant bg-transparent text-on-surface hover:bg-surface-container-high',
        ghost: 'bg-transparent text-on-surface hover:bg-surface-container-high',
        destructive: 'bg-error text-on-error hover:opacity-90',
      },
      size: {
        default: 'h-11 px-4 py-2',
        sm: 'h-9 px-3',
        lg: 'h-12 px-6 text-base',
      },
    },
    defaultVariants: {
      variant: 'default',
      size: 'default',
    },
  },
)
