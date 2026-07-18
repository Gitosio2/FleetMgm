import { useState } from 'react'
import type { MaintenanceRecord } from '@fleetmgm/api'
import { useMaintenanceRecords } from '@fleetmgm/hooks'
import { Button } from '@/components/ui/button'
import { MaintenanceTable } from '@/components/workshop/MaintenanceTable'
import { MaintenanceFormModal } from '@/components/workshop/MaintenanceFormModal'

const PAGE_SIZE = 20

// Read-only registry — creation and the full lifecycle (iniciar/completar/cancelar) live in
// Agenda now. "Editar" is the only action here, for correcting categoría/costo/descripción.
export function MaintenanceOrders() {
  const [page, setPage] = useState(0)
  const [formOpen, setFormOpen] = useState(false)
  const [editingMaintenance, setEditingMaintenance] = useState<MaintenanceRecord | undefined>(undefined)

  const { data: maintenancePage, isLoading, isError } = useMaintenanceRecords(page, PAGE_SIZE)

  function openEditForm(record: MaintenanceRecord) {
    setEditingMaintenance(record)
    setFormOpen(true)
  }

  return (
    <div className="flex flex-col gap-6">
      <div>
        <h1 className="font-display text-2xl font-semibold">Órdenes de mantenimiento</h1>
        <p className="text-on-surface-variant">
          Registro de trabajos realizados por vehículo — consulta de estado, categoría y costo.
        </p>
      </div>

      {isLoading ? (
        <p className="text-on-surface-variant">Cargando mantenimientos…</p>
      ) : isError ? (
        <p role="alert" className="text-sm text-error">
          No se pudieron cargar los datos.
        </p>
      ) : (
        <MaintenanceTable records={maintenancePage?.content ?? []} onEdit={openEditForm} />
      )}
      {maintenancePage && maintenancePage.totalPages > 1 && (
        <div className="flex items-center justify-between">
          <Button variant="outline" size="sm" disabled={page === 0} onClick={() => setPage((current) => current - 1)}>
            Anterior
          </Button>
          <span className="text-sm text-on-surface-variant">
            Página {page + 1} de {maintenancePage.totalPages}
          </span>
          <Button
            variant="outline"
            size="sm"
            disabled={page + 1 >= maintenancePage.totalPages}
            onClick={() => setPage((current) => current + 1)}
          >
            Siguiente
          </Button>
        </div>
      )}

      {editingMaintenance && (
        <MaintenanceFormModal
          open={formOpen}
          onOpenChange={(open) => {
            setFormOpen(open)
            if (!open) {
              setEditingMaintenance(undefined)
            }
          }}
          record={editingMaintenance}
        />
      )}
    </div>
  )
}
