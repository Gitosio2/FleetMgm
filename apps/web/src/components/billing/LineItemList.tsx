import { useState, type FormEvent } from 'react'
import { Pencil, Plus } from 'lucide-react'
import type { Invoice, LineItemRequest, LineItemResponse } from '@fleetmgm/api'
import { useAddLineItem, useUpdateLineItem } from '@fleetmgm/hooks'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { formatCurrency } from '@/lib/currency'

type LineItemListProps = {
  invoice: Invoice
  readOnly?: boolean
}

export function LineItemList({ invoice, readOnly = false }: LineItemListProps) {
  const [editingId, setEditingId] = useState<string | null>(null)
  const [isAdding, setIsAdding] = useState(false)

  const canAddLineItem = !readOnly && invoice.status === 'DRAFT'

  return (
    <div className="flex flex-col gap-3">
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead>Descripción</TableHead>
            <TableHead>Cantidad</TableHead>
            <TableHead>Precio unitario</TableHead>
            <TableHead>Subtotal</TableHead>
            <TableHead>Acciones</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {invoice.lineItems.length === 0 && !isAdding && (
            <TableRow>
              <TableCell colSpan={5} className="text-center text-on-surface-variant">
                Sin líneas de factura todavía.
              </TableCell>
            </TableRow>
          )}

          {invoice.lineItems.map((lineItem) =>
            editingId === lineItem.id ? (
              <LineItemFormRow
                key={lineItem.id}
                invoiceId={invoice.id}
                lineItem={lineItem}
                onCancel={() => setEditingId(null)}
                onSaved={() => setEditingId(null)}
              />
            ) : (
              <TableRow key={lineItem.id}>
                <TableCell>{lineItem.description}</TableCell>
                <TableCell>{lineItem.quantity}</TableCell>
                <TableCell>{formatCurrency(lineItem.unitPrice)}</TableCell>
                <TableCell>{formatCurrency(lineItem.subtotal)}</TableCell>
                <TableCell>
                  {canAddLineItem && (
                    <Button
                      type="button"
                      variant="ghost"
                      size="sm"
                      aria-label="Editar línea"
                      title="Editar línea"
                      onClick={() => setEditingId(lineItem.id)}
                    >
                      <Pencil className="size-4" />
                    </Button>
                  )}
                </TableCell>
              </TableRow>
            ),
          )}

          {canAddLineItem &&
            (isAdding ? (
              <LineItemFormRow
                invoiceId={invoice.id}
                onCancel={() => setIsAdding(false)}
                onSaved={() => setIsAdding(false)}
              />
            ) : (
              <TableRow>
                <TableCell colSpan={5}>
                  <Button type="button" variant="ghost" size="sm" onClick={() => setIsAdding(true)}>
                    <Plus className="size-4" />
                    Añadir línea
                  </Button>
                </TableCell>
              </TableRow>
            ))}
        </TableBody>
      </Table>
    </div>
  )
}

// Shared by both "add" and "edit" so the two flows look and behave identically — the only
// difference is which mutation fires on submit (create vs update) and the button/error copy.
type LineItemFormRowProps = {
  invoiceId: string
  lineItem?: LineItemResponse
  onCancel: () => void
  onSaved: () => void
}

function LineItemFormRow({ invoiceId, lineItem, onCancel, onSaved }: LineItemFormRowProps) {
  const isEditing = lineItem != null
  const addLineItem = useAddLineItem()
  const updateLineItem = useUpdateLineItem()
  const mutation = isEditing ? updateLineItem : addLineItem

  const [description, setDescription] = useState(lineItem?.description ?? '')
  const [quantity, setQuantity] = useState(String(lineItem?.quantity ?? '1'))
  const [unitPrice, setUnitPrice] = useState(String(lineItem?.unitPrice ?? ''))

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()

    const request: LineItemRequest = {
      description,
      quantity: Number(quantity),
      unitPrice: Number(unitPrice),
    }

    if (isEditing) {
      updateLineItem.mutate({ invoiceId, lineItemId: lineItem.id, request }, { onSuccess: onSaved })
    } else {
      addLineItem.mutate({ id: invoiceId, request }, { onSuccess: onSaved })
    }
  }

  const idPrefix = isEditing ? `edit-line-item-${lineItem.id}` : 'new-line-item'

  return (
    <TableRow>
      <TableCell colSpan={5}>
        <form className="flex flex-col gap-3" onSubmit={handleSubmit}>
          <div className="grid grid-cols-3 gap-3">
            <div className="flex flex-col gap-1.5">
              <Label htmlFor={`${idPrefix}-description`}>Descripción</Label>
              <Input
                id={`${idPrefix}-description`}
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                required
              />
            </div>
            <div className="flex flex-col gap-1.5">
              <Label htmlFor={`${idPrefix}-quantity`}>Cantidad</Label>
              <Input
                id={`${idPrefix}-quantity`}
                type="number"
                min="0"
                step="any"
                value={quantity}
                onChange={(e) => setQuantity(e.target.value)}
                required
              />
            </div>
            <div className="flex flex-col gap-1.5">
              <Label htmlFor={`${idPrefix}-unit-price`}>Precio unitario</Label>
              <Input
                id={`${idPrefix}-unit-price`}
                type="number"
                min="0"
                step="any"
                value={unitPrice}
                onChange={(e) => setUnitPrice(e.target.value)}
                required
              />
            </div>
          </div>

          {mutation.isError && (
            <p role="alert" className="text-sm text-error">
              {isEditing ? 'No se pudo actualizar la línea.' : 'No se pudo agregar la línea.'}
            </p>
          )}

          <div className="flex gap-2">
            <Button type="submit" size="sm" variant="outline" disabled={mutation.isPending}>
              {isEditing ? 'Guardar' : 'Agregar línea'}
            </Button>
            <Button type="button" size="sm" variant="ghost" onClick={onCancel}>
              Cancelar
            </Button>
          </div>
        </form>
      </TableCell>
    </TableRow>
  )
}
