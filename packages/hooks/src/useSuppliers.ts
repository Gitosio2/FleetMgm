import { useQuery } from '@tanstack/react-query'
import { apiClient } from '@fleetmgm/api'
import type { CreateSupplierRequest, PageResponse, Supplier, UpdateSupplierRequest } from '@fleetmgm/api'
import { createCrudHooks } from './createCrudHooks'

export const SUPPLIER_KEY = 'suppliers'

const supplierHooks = createCrudHooks<Supplier, CreateSupplierRequest, UpdateSupplierRequest>(
  SUPPLIER_KEY,
  '/suppliers',
)

export type SupplierFilters = {
  name?: string
  taxId?: string
}

// Hand-rolled instead of supplierHooks.useList (createCrudHooks has no filter support, and stays
// that way — every other CRUD entity in this app doesn't need filtering, so extending the shared
// factory just for this one consumer isn't worth the risk). Uses the SUPPLIER_KEY constant the
// factory was built with, so useCreateSupplier/useUpdateSupplier/useDeleteSupplier's
// invalidateQueries({ queryKey: [SUPPLIER_KEY] }) still refreshes this list — same pattern as
// useInvoices/useSupplierInvoices in the billing feature.
export function useSuppliers(filters: SupplierFilters = {}, page = 0, size = 20) {
  const { name, taxId } = filters

  return useQuery({
    queryKey: [SUPPLIER_KEY, { ...filters, page, size }],
    queryFn: async () => {
      const { data } = await apiClient.get<PageResponse<Supplier>>('/suppliers', {
        params: { name, taxId, page, size },
      })
      return data
    },
  })
}

export const useSupplier = supplierHooks.useDetail
export const useCreateSupplier = supplierHooks.useCreate
export const useUpdateSupplier = supplierHooks.useUpdate
export const useDeleteSupplier = supplierHooks.useDelete

// For populating a <select> that must offer every supplier, not just the first page — same
// rationale as useAllVehicles in useVehicles.ts.
export function useAllSuppliers(filters: SupplierFilters = {}) {
  const { name, taxId } = filters

  return useQuery({
    queryKey: [SUPPLIER_KEY, 'all', filters],
    queryFn: async () => {
      const all: Supplier[] = []
      let page = 0
      let totalPages = 1
      do {
        const { data } = await apiClient.get<PageResponse<Supplier>>('/suppliers', {
          params: { name, taxId, page, size: 200 },
        })
        all.push(...data.content)
        totalPages = data.totalPages
        page += 1
      } while (page < totalPages)
      return all
    },
  })
}
