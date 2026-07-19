import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { apiClient } from '@fleetmgm/api'
import type {
  CreateSupplierInvoiceRequest,
  ExpenseCategory,
  PageResponse,
  SupplierInvoice,
  SupplierInvoiceStatus,
  SupplierLineItemRequest,
  SupplierLineItemResponse,
  UpdateSupplierInvoiceRequest,
} from '@fleetmgm/api'
import { invalidateQueryKeys } from './invalidateQueryKeys'
import { FINANCIAL_SUMMARY_KEY } from './useDashboard'

export const SUPPLIER_INVOICE_KEY = 'supplier-invoices'

export type SupplierInvoiceFilters = {
  supplierInvoiceNumber?: string
  vehicleId?: string
  category?: ExpenseCategory
  supplierId?: string
  status?: SupplierInvoiceStatus
  invoiceDateFrom?: string
  invoiceDateTo?: string
  dueDateFrom?: string
  dueDateTo?: string
  totalMin?: number
  totalMax?: number
}

export function useSupplierInvoices(filters: SupplierInvoiceFilters = {}, page = 0, size = 20) {
  const {
    supplierInvoiceNumber,
    vehicleId,
    category,
    supplierId,
    status,
    invoiceDateFrom,
    invoiceDateTo,
    dueDateFrom,
    dueDateTo,
    totalMin,
    totalMax,
  } = filters

  return useQuery({
    queryKey: [SUPPLIER_INVOICE_KEY, { ...filters, page, size }],
    queryFn: async () => {
      const { data } = await apiClient.get<PageResponse<SupplierInvoice>>('/supplier-invoices', {
        params: {
          supplierInvoiceNumber,
          vehicleId,
          category,
          supplierId,
          status,
          invoiceDateFrom,
          invoiceDateTo,
          dueDateFrom,
          dueDateTo,
          totalMin,
          totalMax,
          page,
          size,
        },
      })
      return data
    },
  })
}

export function useCreateSupplierInvoice() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: async (request: CreateSupplierInvoiceRequest) => {
      const { data } = await apiClient.post<SupplierInvoice>('/supplier-invoices', request)
      return data
    },
    // Unlike a client Invoice, a supplier invoice has no DRAFT state — total/invoiceDate are set
    // directly on creation, so it counts toward monthly costs immediately. Invalidate
    // FINANCIAL_SUMMARY_KEY so the dashboard's "Resumen del mes" card doesn't keep a stale cache.
    onSuccess: () => invalidateQueryKeys(queryClient, [SUPPLIER_INVOICE_KEY, FINANCIAL_SUMMARY_KEY]),
  })
}

export function useDeleteSupplierInvoice() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: async (id: string) => {
      await apiClient.delete(`/supplier-invoices/${id}`)
    },
    // Soft-deleting removes the invoice's total from the monthly cost sum.
    onSuccess: () => invalidateQueryKeys(queryClient, [SUPPLIER_INVOICE_KEY, FINANCIAL_SUMMARY_KEY]),
  })
}

export function useUpdateSupplierInvoice() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: async ({ id, request }: { id: string; request: UpdateSupplierInvoiceRequest }) => {
      const { data } = await apiClient.put<SupplierInvoice>(`/supplier-invoices/${id}`, request)
      return data
    },
    // total/invoiceDate can change here, both of which feed the monthly cost sum.
    onSuccess: () => invalidateQueryKeys(queryClient, [SUPPLIER_INVOICE_KEY, FINANCIAL_SUMMARY_KEY]),
  })
}

export function usePaySupplierInvoice() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: async ({ id, paymentDate }: { id: string; paymentDate?: string | null }) => {
      const { data } = await apiClient.patch<SupplierInvoice>(`/supplier-invoices/${id}/pay`, { paymentDate })
      return data
    },
    // Paying removes it from the dashboard's upcoming-payables list — invalidate
    // FINANCIAL_SUMMARY_KEY too so the dashboard card refetches.
    onSuccess: () => invalidateQueryKeys(queryClient, [SUPPLIER_INVOICE_KEY, FINANCIAL_SUMMARY_KEY]),
  })
}

export function useAddSupplierLineItem() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: async ({ id, request }: { id: string; request: SupplierLineItemRequest }) => {
      // The backend endpoint returns only the created SupplierLineItemResponse, not the parent
      // invoice (see SupplierInvoiceController.addLineItem) — the query invalidation below is
      // what refreshes the invoice (and its lineItems array) in the UI.
      const { data } = await apiClient.post<SupplierLineItemResponse>(`/supplier-invoices/${id}/line-items`, request)
      return data
    },
    onSuccess: () => queryClient.invalidateQueries({ queryKey: [SUPPLIER_INVOICE_KEY] }),
  })
}

export function useUpdateSupplierLineItem() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: async ({
      id,
      lineItemId,
      request,
    }: {
      id: string
      lineItemId: string
      request: SupplierLineItemRequest
    }) => {
      const { data } = await apiClient.put<SupplierLineItemResponse>(
        `/supplier-invoices/${id}/line-items/${lineItemId}`,
        request,
      )
      return data
    },
    onSuccess: () => queryClient.invalidateQueries({ queryKey: [SUPPLIER_INVOICE_KEY] }),
  })
}

export function useDeleteSupplierLineItem() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: async ({ id, lineItemId }: { id: string; lineItemId: string }) => {
      await apiClient.delete(`/supplier-invoices/${id}/line-items/${lineItemId}`)
    },
    onSuccess: () => queryClient.invalidateQueries({ queryKey: [SUPPLIER_INVOICE_KEY] }),
  })
}
