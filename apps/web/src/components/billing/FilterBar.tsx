import { useState } from 'react'
import { ChevronDown, Plus } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader } from '@/components/ui/card'
import { Collapsible, CollapsibleContent, CollapsibleTrigger } from '@/components/ui/collapsible'

const selectClassName =
  'flex h-9 rounded-lg border border-outline-variant bg-surface-container-lowest px-3 text-sm text-on-surface focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-secondary-container'

export type FilterField =
  | {
      type: 'select'
      key: string
      label: string
      ariaLabel: string
      value: string
      onChange: (value: string) => void
      placeholder: string
      options: { value: string; label: string }[]
    }
  | {
      type: 'date'
      key: string
      label: string
      ariaLabel: string
      value: string
      onChange: (value: string) => void
    }
  | {
      type: 'number'
      key: string
      label: string
      ariaLabel: string
      value: string
      onChange: (value: string) => void
      step?: string
      min?: string
    }
  | {
      type: 'text'
      key: string
      label: string
      ariaLabel: string
      value: string
      onChange: (value: string) => void
      placeholder?: string
    }

type FilterBarProps = {
  title: string
  description: string
  createLabel: string
  onCreate: () => void
  fields: FilterField[]
}

export function FilterBar({ title, description, createLabel, onCreate, fields }: FilterBarProps) {
  const [filtersOpen, setFiltersOpen] = useState(true)

  function toggleFilters() {
    setFiltersOpen((open) => !open)
  }

  return (
    <Card>
      <Collapsible open={filtersOpen} onOpenChange={setFiltersOpen}>
        <CardHeader className="flex-row items-center justify-between gap-4">
          <div
            role="button"
            tabIndex={0}
            className="cursor-pointer"
            onClick={toggleFilters}
            onKeyDown={(e) => {
              if (e.key === 'Enter' || e.key === ' ') {
                e.preventDefault()
                toggleFilters()
              }
            }}
          >
            <h1 className="font-display text-2xl font-semibold">{title}</h1>
            <p className="text-on-surface-variant">{description}</p>
          </div>
          <div className="flex items-center gap-2">
            <Button onClick={onCreate}>
              <Plus className="size-4" />
              {createLabel}
            </Button>
            <CollapsibleTrigger asChild>
              <Button
                variant="outline"
                size="sm"
                className="group"
                aria-label="Mostrar u ocultar filtros"
                title="Mostrar u ocultar filtros"
              >
                <ChevronDown className="size-4 transition-transform duration-200 group-data-[state=open]:rotate-180" />
              </Button>
            </CollapsibleTrigger>
          </div>
        </CardHeader>
        <CollapsibleContent>
          <CardContent>
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
              {fields.map((field) => (
                <div key={field.key} className="flex flex-col gap-1 text-sm text-on-surface-variant">
                  <span>{field.label}</span>
                  {field.type === 'select' ? (
                    <select
                      aria-label={field.ariaLabel}
                      className={selectClassName}
                      value={field.value}
                      onChange={(e) => field.onChange(e.target.value)}
                    >
                      <option value="">{field.placeholder}</option>
                      {field.options.map((option) => (
                        <option key={option.value} value={option.value}>
                          {option.label}
                        </option>
                      ))}
                    </select>
                  ) : field.type === 'date' ? (
                    <input
                      aria-label={field.ariaLabel}
                      type="date"
                      className={selectClassName}
                      value={field.value}
                      onChange={(e) => field.onChange(e.target.value)}
                    />
                  ) : field.type === 'number' ? (
                    <input
                      aria-label={field.ariaLabel}
                      type="number"
                      step={field.step ?? '0.01'}
                      min={field.min ?? '0'}
                      className={selectClassName}
                      value={field.value}
                      onChange={(e) => field.onChange(e.target.value)}
                    />
                  ) : (
                    <input
                      aria-label={field.ariaLabel}
                      type="text"
                      placeholder={field.placeholder}
                      className={selectClassName}
                      value={field.value}
                      onChange={(e) => field.onChange(e.target.value)}
                    />
                  )}
                </div>
              ))}
            </div>
          </CardContent>
        </CollapsibleContent>
      </Collapsible>
    </Card>
  )
}
