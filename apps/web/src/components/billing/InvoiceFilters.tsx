import type { Client, InvoiceStatus } from '@fleetmgm/api'
import { FilterBar, type FilterField } from './FilterBar'
import { STATUS_LABEL } from './invoice-shared'

type InvoiceFiltersProps = {
  invoiceNumber: string
  onInvoiceNumberChange: (value: string) => void
  clientId: string
  onClientIdChange: (value: string) => void
  status: InvoiceStatus | ''
  onStatusChange: (value: InvoiceStatus | '') => void
  issueDateFrom: string
  onIssueDateFromChange: (value: string) => void
  issueDateTo: string
  onIssueDateToChange: (value: string) => void
  dueDateFrom: string
  onDueDateFromChange: (value: string) => void
  dueDateTo: string
  onDueDateToChange: (value: string) => void
  paymentDateFrom: string
  onPaymentDateFromChange: (value: string) => void
  paymentDateTo: string
  onPaymentDateToChange: (value: string) => void
  totalMin: string
  onTotalMinChange: (value: string) => void
  totalMax: string
  onTotalMaxChange: (value: string) => void
  clients: Client[]
  onCreate: () => void
}

export function InvoiceFilters({
  invoiceNumber,
  onInvoiceNumberChange,
  clientId,
  onClientIdChange,
  status,
  onStatusChange,
  issueDateFrom,
  onIssueDateFromChange,
  issueDateTo,
  onIssueDateToChange,
  dueDateFrom,
  onDueDateFromChange,
  dueDateTo,
  onDueDateToChange,
  paymentDateFrom,
  onPaymentDateFromChange,
  paymentDateTo,
  onPaymentDateToChange,
  totalMin,
  onTotalMinChange,
  totalMax,
  onTotalMaxChange,
  clients,
  onCreate,
}: InvoiceFiltersProps) {
  // OVERDUE is never persisted on Invoice.status (see InvoiceRepository.findAllJoinFetch's comment)
  // — it's computed live from dueDate, so offering it here would be a filter option that can never
  // match a row.
  const statusOptions = (Object.keys(STATUS_LABEL) as InvoiceStatus[])
    .filter((value) => value !== 'OVERDUE')
    .map((value) => ({ value, label: STATUS_LABEL[value] }))

  const fields: FilterField[] = [
    {
      type: 'text',
      key: 'invoiceNumber',
      label: 'Nº de factura',
      ariaLabel: 'Buscar por número de factura',
      value: invoiceNumber,
      onChange: onInvoiceNumberChange,
      placeholder: 'INV-2026-00001',
    },
    {
      type: 'select',
      key: 'clientId',
      label: 'Cliente',
      ariaLabel: 'Filtrar por cliente',
      value: clientId,
      onChange: onClientIdChange,
      placeholder: 'Todos los clientes',
      options: clients.map((client) => ({ value: client.id, label: client.name })),
    },
    {
      type: 'select',
      key: 'status',
      label: 'Estado',
      ariaLabel: 'Filtrar por estado',
      value: status,
      onChange: (value) => onStatusChange(value as InvoiceStatus | ''),
      placeholder: 'Todos los estados',
      options: statusOptions,
    },
    {
      type: 'date',
      key: 'issueDateFrom',
      label: 'Emisión desde',
      ariaLabel: 'Emisión desde',
      value: issueDateFrom,
      onChange: onIssueDateFromChange,
    },
    {
      type: 'date',
      key: 'issueDateTo',
      label: 'Emisión hasta',
      ariaLabel: 'Emisión hasta',
      value: issueDateTo,
      onChange: onIssueDateToChange,
    },
    {
      type: 'date',
      key: 'dueDateFrom',
      label: 'Vencimiento desde',
      ariaLabel: 'Vencimiento desde',
      value: dueDateFrom,
      onChange: onDueDateFromChange,
    },
    {
      type: 'date',
      key: 'dueDateTo',
      label: 'Vencimiento hasta',
      ariaLabel: 'Vencimiento hasta',
      value: dueDateTo,
      onChange: onDueDateToChange,
    },
    {
      type: 'date',
      key: 'paymentDateFrom',
      label: 'Pago desde',
      ariaLabel: 'Pago desde',
      value: paymentDateFrom,
      onChange: onPaymentDateFromChange,
    },
    {
      type: 'date',
      key: 'paymentDateTo',
      label: 'Pago hasta',
      ariaLabel: 'Pago hasta',
      value: paymentDateTo,
      onChange: onPaymentDateToChange,
    },
    {
      type: 'number',
      key: 'totalMin',
      label: 'Total mínimo',
      ariaLabel: 'Total mínimo',
      value: totalMin,
      onChange: onTotalMinChange,
    },
    {
      type: 'number',
      key: 'totalMax',
      label: 'Total máximo',
      ariaLabel: 'Total máximo',
      value: totalMax,
      onChange: onTotalMaxChange,
    },
  ]

  return (
    <FilterBar
      title="Facturación a clientes"
      description="Gestiona las facturas de clientes y su ciclo de vida."
      createLabel="Nueva factura"
      onCreate={onCreate}
      fields={fields}
    />
  )
}
