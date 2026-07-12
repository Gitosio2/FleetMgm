import type { AuditAction } from '@fleetmgm/api'
import { AUDIT_ACTION_LABEL, AUDIT_ENTITY_TYPE_LABEL } from './audit-log-shared'

const selectClassName =
  'flex h-9 rounded-lg border border-outline-variant bg-surface-container-lowest px-3 text-sm text-on-surface focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-secondary-container'

const inputClassName = selectClassName

type AuditLogFiltersProps = {
  entityType: string
  onEntityTypeChange: (value: string) => void
  action: AuditAction | ''
  onActionChange: (value: AuditAction | '') => void
  from: string
  onFromChange: (value: string) => void
  to: string
  onToChange: (value: string) => void
  performedByEmail: string
  onPerformedByEmailChange: (value: string) => void
}

export function AuditLogFilters({
  entityType,
  onEntityTypeChange,
  action,
  onActionChange,
  from,
  onFromChange,
  to,
  onToChange,
  performedByEmail,
  onPerformedByEmailChange,
}: AuditLogFiltersProps) {
  return (
    <div className="flex flex-wrap items-center gap-3">
      <input
        aria-label="Filtrar por usuario"
        type="text"
        placeholder="Buscar por usuario…"
        className={inputClassName}
        value={performedByEmail}
        onChange={(e) => onPerformedByEmailChange(e.target.value)}
      />
      <select
        aria-label="Filtrar por tipo de entidad"
        className={selectClassName}
        value={entityType}
        onChange={(e) => onEntityTypeChange(e.target.value)}
      >
        <option value="">Todas las entidades</option>
        {Object.entries(AUDIT_ENTITY_TYPE_LABEL).map(([value, label]) => (
          <option key={value} value={value}>
            {label}
          </option>
        ))}
      </select>
      <select
        aria-label="Filtrar por acción"
        className={selectClassName}
        value={action}
        onChange={(e) => onActionChange(e.target.value as AuditAction | '')}
      >
        <option value="">Todas las acciones</option>
        {(Object.keys(AUDIT_ACTION_LABEL) as AuditAction[]).map((value) => (
          <option key={value} value={value}>
            {AUDIT_ACTION_LABEL[value]}
          </option>
        ))}
      </select>
      <label className="flex items-center gap-2 text-sm text-on-surface-variant">
        Desde
        <input
          aria-label="Desde"
          type="date"
          className={inputClassName}
          value={from}
          onChange={(e) => onFromChange(e.target.value)}
        />
      </label>
      <label className="flex items-center gap-2 text-sm text-on-surface-variant">
        Hasta
        <input
          aria-label="Hasta"
          type="date"
          className={inputClassName}
          value={to}
          onChange={(e) => onToChange(e.target.value)}
        />
      </label>
    </div>
  )
}
