import { useQuery } from '@tanstack/react-query'
import { apiClient } from '@fleetmgm/api'
import type { AuditAction, AuditLog, AuditLogPerformer, PageResponse } from '@fleetmgm/api'

export const AUDIT_LOG_KEY = 'audit-log'
export const AUDIT_LOG_PERFORMERS_KEY = 'audit-log-performers'

export type AuditLogFilters = {
  entityType?: string
  action?: AuditAction
  from?: string
  to?: string
  performedByEmail?: string
}

export function useAuditLog(filters: AuditLogFilters = {}, page = 0, size = 20) {
  const { entityType, action, from, to, performedByEmail } = filters

  return useQuery({
    queryKey: [AUDIT_LOG_KEY, { entityType, action, from, to, performedByEmail, page, size }],
    queryFn: async () => {
      const { data } = await apiClient.get<PageResponse<AuditLog>>('/audit', {
        params: { entityType, action, from, to, performedByEmail, page, size },
      })
      return data
    },
  })
}

// Distinct performers already present in the audit log, for the user filter dropdown — scoped to
// this feature, not a general user-listing endpoint (see AuditLogFilters).
export function useAuditLogPerformers() {
  return useQuery({
    queryKey: [AUDIT_LOG_PERFORMERS_KEY],
    queryFn: async () => {
      const { data } = await apiClient.get<AuditLogPerformer[]>('/audit/performers')
      return data
    },
  })
}
