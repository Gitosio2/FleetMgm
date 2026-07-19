import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { apiClient } from '@fleetmgm/api'
import type {
  CreateInvoiceRequest,
  Invoice,
  InvoiceStatus,
  LineItemRequest,
  LineItemResponse,
  PageResponse,
  UpdateInvoiceRequest,
} from '@fleetmgm/api'
import { invalidateQueryKeys } from './invalidateQueryKeys'
import { FINANCIAL_SUMMARY_KEY } from './useDashboard'

export const INVOICE_KEY = 'invoices'

export type InvoiceFilters = {
  invoiceNumber?: string
  clientId?: string
  status?: InvoiceStatus
  issueDateFrom?: string
  issueDateTo?: string
  dueDateFrom?: string
  dueDateTo?: string
  totalMin?: number
  totalMax?: number
}

export function useInvoices(filters: InvoiceFilters = {}, page = 0, size = 20) {
  const { invoiceNumber, clientId, status, issueDateFrom, issueDateTo, dueDateFrom, dueDateTo, totalMin, totalMax } =
    filters

  return useQuery({
    queryKey: [INVOICE_KEY, { ...filters, page, size }],
    queryFn: async () => {
      const { data } = await apiClient.get<PageResponse<Invoice>>('/invoices', {
        params: {
          invoiceNumber,
          clientId,
          status,
          issueDateFrom,
          issueDateTo,
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

export function useInvoice(id: string) {
  return useQuery({
    queryKey: [INVOICE_KEY, id],
    queryFn: async () => {
      const { data } = await apiClient.get<Invoice>(`/invoices/${id}`)
      return data
    },
    enabled: id !== '',
  })
}

export function useCreateInvoice() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: async (request: CreateInvoiceRequest) => {
      const { data } = await apiClient.post<Invoice>('/invoices', request)
      return data
    },
    onSuccess: () => queryClient.invalidateQueries({ queryKey: [INVOICE_KEY] }),
  })
}

export function useUpdateInvoice() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: async ({ id, request }: { id: string; request: UpdateInvoiceRequest }) => {
      const { data } = await apiClient.put<Invoice>(`/invoices/${id}`, request)
      return data
    },
    onSuccess: () => queryClient.invalidateQueries({ queryKey: [INVOICE_KEY] }),
  })
}

export function useDeleteInvoice() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: async (id: string) => {
      await apiClient.delete(`/invoices/${id}`)
    },
    onSuccess: () => queryClient.invalidateQueries({ queryKey: [INVOICE_KEY] }),
  })
}

export function useIssueInvoice() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: async (id: string) => {
      const { data } = await apiClient.patch<Invoice>(`/invoices/${id}/issue`)
      return data
    },
    onSuccess: () => queryClient.invalidateQueries({ queryKey: [INVOICE_KEY] }),
  })
}

export function usePayInvoice() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: async ({ id, paymentDate }: { id: string; paymentDate?: string | null }) => {
      const { data } = await apiClient.patch<Invoice>(`/invoices/${id}/pay`, { paymentDate })
      return data
    },
    // Paying an invoice removes it from the dashboard's upcoming-receivables list (it stops
    // matching the backend's ISSUED-only filter) — invalidate FINANCIAL_SUMMARY_KEY too so the
    // dashboard card refetches, whether the mutation was triggered from the table or the card.
    onSuccess: () => invalidateQueryKeys(queryClient, [INVOICE_KEY, FINANCIAL_SUMMARY_KEY]),
  })
}

export function useAddLineItem() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: async ({ id, request }: { id: string; request: LineItemRequest }) => {
      const { data } = await apiClient.post<Invoice>(`/invoices/${id}/line-items`, request)
      return data
    },
    // The response is the full updated Invoice (including recalculated subtotal/tax/total), not
    // just the created LineItemResponse — invalidating the list key keeps both the table's totals
    // and the modal's line item list consistent with the server-side recalculation.
    onSuccess: () => invalidateQueryKeys(queryClient, [INVOICE_KEY]),
  })
}

export function useUpdateLineItem() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: async ({
      invoiceId,
      lineItemId,
      request,
    }: {
      invoiceId: string
      lineItemId: string
      request: LineItemRequest
    }) => {
      const { data } = await apiClient.patch<LineItemResponse>(
        `/invoices/${invoiceId}/line-items/${lineItemId}`,
        request,
      )
      return data
    },
    onSuccess: () => invalidateQueryKeys(queryClient, [INVOICE_KEY]),
  })
}

// Not a query — this is a one-shot side effect (trigger a browser download), not app state to
// cache or invalidate. useMutation is a convenient way to get isPending/isError tracking for the
// download button without introducing a query key that nothing ever reads from.
export function useDownloadInvoicePdf() {
  return useMutation({
    mutationFn: async ({ id, invoiceNumber }: { id: string; invoiceNumber: string }) => {
      const response = await apiClient.get<Blob>(`/invoices/${id}/pdf`, { responseType: 'blob' })
      const url = window.URL.createObjectURL(response.data)
      const link = document.createElement('a')
      link.href = url
      link.download = `${invoiceNumber}.pdf`
      document.body.appendChild(link)
      link.click()
      link.remove()
      // Revoking synchronously in the same tick as click() is a documented browser race: the
      // download read of the blob may not have started yet, producing an empty/corrupted file
      // (Firefox bug 1282407, Chromium issue 41380177). Deferring lets the download start first.
      setTimeout(() => window.URL.revokeObjectURL(url), 0)
    },
  })
}
