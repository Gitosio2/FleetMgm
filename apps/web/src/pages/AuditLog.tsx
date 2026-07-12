import { useState } from 'react'
import type { AuditAction } from '@fleetmgm/api'
import { useAuditLog } from '@fleetmgm/hooks'
import { Button } from '@/components/ui/button'
import { AuditLogTable } from '@/components/audit/AuditLogTable'
import { AuditLogFilters } from '@/components/audit/AuditLogFilters'

const PAGE_SIZE = 20

export function AuditLog() {
  const [page, setPage] = useState(0)
  const [entityType, setEntityType] = useState('')
  const [action, setAction] = useState<AuditAction | ''>('')
  const [from, setFrom] = useState('')
  const [to, setTo] = useState('')

  const { data, isLoading, isError } = useAuditLog(
    {
      entityType: entityType === '' ? undefined : entityType,
      action: action === '' ? undefined : action,
      from: from === '' ? undefined : `${from}T00:00:00Z`,
      to: to === '' ? undefined : `${to}T23:59:59Z`,
    },
    page,
    PAGE_SIZE,
  )

  function resetPageAnd<T>(setter: (value: T) => void) {
    return (value: T) => {
      setter(value)
      setPage(0)
    }
  }

  return (
    <div className="flex flex-col gap-6">
      <div>
        <h1 className="font-display text-2xl font-semibold">Registro de auditoría</h1>
        <p className="text-on-surface-variant">Consulta el historial de acciones realizadas en el sistema.</p>
      </div>

      <AuditLogFilters
        entityType={entityType}
        onEntityTypeChange={resetPageAnd(setEntityType)}
        action={action}
        onActionChange={resetPageAnd(setAction)}
        from={from}
        onFromChange={resetPageAnd(setFrom)}
        to={to}
        onToChange={resetPageAnd(setTo)}
      />

      {isLoading ? (
        <p className="text-on-surface-variant">Cargando registro de auditoría…</p>
      ) : isError ? (
        <p role="alert" className="text-sm text-error">
          No se pudieron cargar los datos.
        </p>
      ) : (
        <AuditLogTable entries={data?.content ?? []} />
      )}

      {data && data.totalPages > 1 && (
        <div className="flex items-center justify-between">
          <Button
            variant="outline"
            size="sm"
            disabled={page === 0}
            onClick={() => setPage((current) => current - 1)}
          >
            Anterior
          </Button>
          <span className="text-sm text-on-surface-variant">
            Página {page + 1} de {data.totalPages}
          </span>
          <Button
            variant="outline"
            size="sm"
            disabled={page + 1 >= data.totalPages}
            onClick={() => setPage((current) => current + 1)}
          >
            Siguiente
          </Button>
        </div>
      )}
    </div>
  )
}
