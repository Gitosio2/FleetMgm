import { Moon, Sun } from 'lucide-react'
import { useThemeStore } from '@fleetmgm/store'
import { Switch } from '@/components/ui/switch'
import { cn } from '@/lib/utils'

type ThemeToggleProps = {
  className?: string
}

export function ThemeToggle({ className }: ThemeToggleProps) {
  const theme = useThemeStore((state) => state.theme)
  const toggleTheme = useThemeStore((state) => state.toggleTheme)
  const isDark = theme === 'dark'

  return (
    <div className={cn('flex items-center justify-between gap-2', className)}>
      <span className="flex items-center gap-2 text-sm font-medium text-on-surface-variant">
        {isDark ? <Moon className="size-4" /> : <Sun className="size-4" />}
        {isDark ? 'Modo oscuro' : 'Modo claro'}
      </span>
      <Switch
        checked={isDark}
        onCheckedChange={toggleTheme}
        aria-label="Cambiar entre modo claro y oscuro"
      />
    </div>
  )
}
