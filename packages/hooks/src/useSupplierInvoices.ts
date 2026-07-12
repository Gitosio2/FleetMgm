import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { apiClient } from '@fleetmgm/api'
import type {
  CreateSupplierInvoiceRequest,
  ExpenseCategory,
  PageResponse,
  SupplierInvoice,
  SupplierLineItemRequest,
  SupplierLineItemResponse,
  UpdateSupplierInvoiceRequest,
} from '@fleetmgm/api'

export const SUPPLIER_INVOICE_KEY = 'supplier-invoices'

export function useSupplierInvoices(vehicleId?: string, category?: ExpenseCategory, page = 0, size = 20) {
  return useQuery({
    queryKey: [SUPPLIER_INVOICE_KEY, { vehicleId, category, page, size }],
    queryFn: async () => {
      const { data } = await apiClient.get<PageResponse<SupplierInvoice>>('/supplier-invoices', {
        params: { vehicleId, category, page, size },
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
    onSuccess: () => queryClient.invalidateQueries({ queryKey: [SUPPLIER_INVOICE_KEY] }),
  })
}

export function useUpdateSupplierInvoice() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: async ({ id, request }: { id: string; request: UpdateSupplierInvoiceRequest }) => {
      const { data } = await apiClient.put<SupplierInvoice>(`/supplier-invoices/${id}`, request)
      return data
    },
    onSuccess: () => queryClient.invalidateQueries({ queryKey: [SUPPLIER_INVOICE_KEY] }),
  })
}

export function usePaySupplierInvoice() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: async ({ id, paymentDate }: { id: string; paymentDate?: string | null }) => {
      const { data } = await apiClient.patch<SupplierInvoice>(`/supplier-invoices/${id}/pay`, { paymentDate })
      return data
    },
    onSuccess: () => queryClient.invalidateQueries({ queryKey: [SUPPLIER_INVOICE_KEY] }),
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
