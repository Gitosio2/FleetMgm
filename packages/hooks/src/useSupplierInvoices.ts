import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { apiClient } from '@fleetmgm/api'
import type {
  CreateSupplierInvoiceRequest,
  ExpenseCategory,
  PageResponse,
  SupplierInvoice,
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
