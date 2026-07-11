import { render, screen, within } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import type { MaintenanceRecord, PageResponse, WorkshopSchedule } from '@fleetmgm/api'
import { server } from '@/mocks/server'
import { DaySchedule } from './DaySchedule'

function renderDaySchedule() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  })

  return render(
    <QueryClientProvider client={queryClient}>
      <DaySchedule />
    </QueryClientProvider>,
  )
}

// Computed the same way DaySchedule itself resolves "today" (local date components) so the test
// stays correct regardless of the real date the suite runs on, without needing to fake the clock.
function todayISODate(): string {
  const now = new Date()
  const year = now.getFullYear()
  const month = String(now.getMonth() + 1).padStart(2, '0')
  const day = String(now.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

function emptyPage<T>(): PageResponse<T> {
  return { content: [], page: 0, size: 100, totalElements: 0, totalPages: 0 }
}

function pageOf<T>(content: T[]): PageResponse<T> {
  return { content, page: 0, size: 100, totalElements: content.length, totalPages: 1 }
}

function mockSchedules(content: WorkshopSchedule[]) {
  server.use(http.get('/api/v1/workshop/schedules', () => HttpResponse.json(pageOf(content))))
}

function mockMaintenance(content: MaintenanceRecord[]) {
  server.use(http.get('/api/v1/maintenance', () => HttpResponse.json(pageOf(content))))
}

const baseSchedule: WorkshopSchedule = {
  id: 'schedule-x',
  vehicleId: 'vehicle-1',
  vehicleLicensePlate: '1234ABC',
  vehicleMake: 'Toyota',
  vehicleModel: 'Hilux',
  technicianId: null,
  technicianName: null,
  maintenanceRecordId: null,
  maintenanceCategory: null,
  scheduledDate: todayISODate(),
  scheduledStartTime: null,
  scheduledEndTime: null,
  type: 'Entrada de agenda',
  priority: 'MEDIUM',
  status: 'PENDING',
  notes: null,
  createdAt: '2026-07-01T09:00:00Z',
}

const baseMaintenance: MaintenanceRecord = {
  id: 'maintenance-x',
  vehicleId: 'vehicle-2',
  vehicleLicensePlate: null,
  vehicleMake: 'Caterpillar',
  vehicleModel: '320 Excavator',
  type: 'Orden de mantenimiento',
  description: null,
  usageAtService: null,
  cost: null,
  workshopEntryDate: todayISODate(),
  workshopExitDate: null,
  workshopEntryTime: null,
  workshopExitTime: null,
  technicianId: null,
  technicianName: null,
  status: 'IN_PROGRESS',
  category: 'PREVENTIVE',
  createdAt: '2026-07-01T09:00:00Z',
}

describe('DaySchedule', () => {
  it('shows a loading state while the queries are in flight', () => {
    server.use(
      http.get('/api/v1/workshop/schedules', async () => {
        await new Promise((resolve) => setTimeout(resolve, 50))
        return HttpResponse.json(emptyPage())
      }),
      http.get('/api/v1/maintenance', async () => {
        await new Promise((resolve) => setTimeout(resolve, 50))
        return HttpResponse.json(emptyPage())
      }),
    )
    renderDaySchedule()

    expect(screen.getByText('Cargando horario…')).toBeInTheDocument()
  })

  it('shows an error message when a query fails', async () => {
    server.use(
      http.get('/api/v1/workshop/schedules', () =>
        HttpResponse.json(
          {
            status: 500,
            code: 'INTERNAL_SERVER_ERROR',
            message: 'Unexpected error',
            correlationId: 'test-correlation-id',
          },
          { status: 500 },
        ),
      ),
    )
    mockMaintenance([])
    renderDaySchedule()

    expect(await screen.findByRole('alert')).toHaveTextContent(/no se pudieron cargar los datos/i)
  })

  it('shows the empty state when nothing is scheduled or done today', async () => {
    mockSchedules([])
    mockMaintenance([])
    renderDaySchedule()

    expect(
      await screen.findByText('No hay trabajos programados ni realizados hoy.'),
    ).toBeInTheDocument()
  })

  it('combines schedule and maintenance entries sorted by time, with untimed entries last', async () => {
    mockSchedules([
      { ...baseSchedule, id: 'schedule-1', type: 'Revisión de frenos', scheduledStartTime: '10:00:00' },
      { ...baseSchedule, id: 'schedule-2', type: 'Revisión sin hora', scheduledStartTime: null },
    ])
    mockMaintenance([
      { ...baseMaintenance, id: 'maintenance-1', type: 'Cambio de aceite', workshopEntryTime: '08:00:00' },
    ])

    renderDaySchedule()

    await screen.findByText('Cambio de aceite')
    const rows = screen.getAllByRole('row').slice(1) // drop the header row
    expect(rows).toHaveLength(3)
    expect(within(rows[0]!).getByText('Cambio de aceite')).toBeInTheDocument()
    expect(within(rows[0]!).getByText('08:00')).toBeInTheDocument()
    expect(within(rows[1]!).getByText('Revisión de frenos')).toBeInTheDocument()
    expect(within(rows[1]!).getByText('10:00')).toBeInTheDocument()
    expect(within(rows[2]!).getByText('Revisión sin hora')).toBeInTheDocument()
    expect(within(rows[2]!).getByText('—')).toBeInTheDocument()
  })

  it('filters maintenance records to only those touching today', async () => {
    mockSchedules([])
    mockMaintenance([
      { ...baseMaintenance, id: 'maintenance-today', type: 'Hoy', workshopEntryDate: todayISODate() },
      {
        ...baseMaintenance,
        id: 'maintenance-other-day',
        type: 'Otro día',
        workshopEntryDate: '2020-01-01',
        workshopExitDate: '2020-01-02',
      },
    ])
    renderDaySchedule()

    expect(await screen.findByText('Hoy')).toBeInTheDocument()
    expect(screen.queryByText('Otro día')).not.toBeInTheDocument()
  })

  it('shows the source and status for each row', async () => {
    mockSchedules([{ ...baseSchedule, id: 'schedule-1', status: 'PENDING' }])
    mockMaintenance([{ ...baseMaintenance, id: 'maintenance-1', status: 'IN_PROGRESS' }])
    renderDaySchedule()

    const scheduleRow = (await screen.findByText('Entrada de agenda')).closest('tr')!
    expect(within(scheduleRow).getByText('Agenda')).toBeInTheDocument()
    expect(within(scheduleRow).getByText('Pendiente')).toBeInTheDocument()

    const maintenanceRow = screen.getByText('Orden de mantenimiento').closest('tr')!
    expect(within(maintenanceRow).getByText('Mantenimiento')).toBeInTheDocument()
    expect(within(maintenanceRow).getByText('En curso')).toBeInTheDocument()
  })
})
