import { useQuery } from '@tanstack/react-query'
import { apiClient } from '@fleetmgm/api'
import type { AuditAction, AuditLog, PageResponse } from '@fleetmgm/api'

export const AUDIT_LOG_KEY = 'audit-log'

export type AuditLogFilters = {
  entityType?: string
  action?: AuditAction
  from?: string
  to?: string
}

export function useAuditLog(filters: AuditLogFilters = {}, page = 0, size = 20) {
  const { entityType, action, from, to } = filters

  return useQuery({
    queryKey: [AUDIT_LOG_KEY, { entityType, action, from, to, page, size }],
    queryFn: async () => {
      const { data } = await apiClient.get<PageResponse<AuditLog>>('/audit', {
        params: { entityType, action, from, to, page, size },
      })
      return data
    },
  })
}
