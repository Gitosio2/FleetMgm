import type { CreateSupplierRequest, Supplier, UpdateSupplierRequest } from '@fleetmgm/api'
import { createCrudHooks } from './createCrudHooks'

const supplierHooks = createCrudHooks<Supplier, CreateSupplierRequest, UpdateSupplierRequest>(
  'suppliers',
  '/suppliers',
)

export const useSuppliers = supplierHooks.useList
export const useCreateSupplier = supplierHooks.useCreate
export const useUpdateSupplier = supplierHooks.useUpdate
export const useDeleteSupplier = supplierHooks.useDelete
