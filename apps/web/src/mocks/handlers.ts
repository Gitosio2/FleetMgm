import { http, HttpResponse } from 'msw'
import { useAuthStore } from '@fleetmgm/store'

export const VALID_CREDENTIALS = {
  email: 'admin@fleetmgm.com',
  password: 'admin123',
}

export const MOCK_ACCESS_TOKEN = 'mock-access-token'
export const MOCK_REFRESH_TOKEN = 'mock-refresh-token'
export const MOCK_ROLE = 'ADMIN'

type LoginRequestBody = {
  email: string
  password: string
}

type RefreshRequestBody = {
  refreshToken: string
}

const invalidCredentialsResponse = () =>
  HttpResponse.json(
    {
      status: 401,
      code: 'INVALID_CREDENTIALS',
      message: 'Invalid credentials',
      correlationId: 'test-correlation-id',
    },
    { status: 401 },
  )

type Client = {
  id: string
  name: string
  taxId: string
  email: string | null
  phone: string | null
  address: string | null
  createdAt: string
}

type ClientRequestBody = {
  name: string
  taxId: string
  email?: string | null
  phone?: string | null
  address?: string | null
}

export const SEED_CLIENTS: Client[] = [
  {
    id: 'client-1',
    name: 'Acme Logistics',
    taxId: 'B12345678',
    email: 'ops@acme.test',
    phone: '+34600000000',
    address: 'Calle Falsa 123',
    createdAt: '2026-01-10T09:00:00Z',
  },
  {
    id: 'client-2',
    name: 'Transportes Ibérica',
    taxId: 'B87654321',
    email: 'contacto@iberica.test',
    phone: '+34600000001',
    address: 'Av. Principal 45',
    createdAt: '2026-02-15T09:00:00Z',
  },
]

let clients: Client[] = [...SEED_CLIENTS]

export function resetClientsMock() {
  clients = [...SEED_CLIENTS]
}

type VehicleCategory = 'LIGHT_VEHICLE' | 'HEAVY_VEHICLE' | 'HEAVY_MACHINERY'
type UsageMeasure = 'KILOMETERS' | 'HOURS'
type VehicleStatus = 'ACTIVE' | 'MAINTENANCE' | 'INACTIVE' | 'DECOMMISSIONED'
type AcquisitionType = 'PURCHASED' | 'LEASING' | 'RENTING'

type Vehicle = {
  id: string
  vehicleCategory: VehicleCategory
  usageMeasure: UsageMeasure
  make: string
  model: string
  year: number
  licensePlate: string | null
  heavySubtype: string | null
  vin: string | null
  color: string | null
  status: VehicleStatus
  currentKm: number | null
  currentHours: number | null
  acquisitionType: AcquisitionType | null
  acquisitionDate: string | null
  purchasePrice: number | null
  amortizationYears: number | null
  monthlyFee: number | null
  contractEndDate: string | null
  createdAt: string
}

type VehicleRequestBody = {
  vehicleCategory: VehicleCategory
  usageMeasure: UsageMeasure
  make: string
  model: string
  year: number
  licensePlate?: string | null
  heavySubtype?: string | null
  vin?: string | null
  color?: string | null
  acquisitionType?: AcquisitionType | null
  acquisitionDate?: string | null
  purchasePrice?: number | null
  amortizationYears?: number | null
  monthlyFee?: number | null
  contractEndDate?: string | null
  currentKm?: number | null
  currentHours?: number | null
}

export const SEED_VEHICLES: Vehicle[] = [
  {
    id: 'vehicle-1',
    vehicleCategory: 'LIGHT_VEHICLE',
    usageMeasure: 'KILOMETERS',
    make: 'Toyota',
    model: 'Hilux',
    year: 2022,
    licensePlate: '1234ABC',
    heavySubtype: null,
    vin: 'VIN00000001',
    color: 'White',
    status: 'ACTIVE',
    currentKm: 15000,
    currentHours: null,
    acquisitionType: 'PURCHASED',
    acquisitionDate: '2022-03-01',
    purchasePrice: 28000,
    amortizationYears: 5,
    monthlyFee: null,
    contractEndDate: null,
    createdAt: '2026-01-10T09:00:00Z',
  },
  {
    id: 'vehicle-2',
    vehicleCategory: 'HEAVY_MACHINERY',
    usageMeasure: 'HOURS',
    make: 'Caterpillar',
    model: '320 Excavator',
    year: 2019,
    licensePlate: null,
    heavySubtype: 'Excavator',
    vin: 'VIN00000002',
    color: 'Yellow',
    status: 'MAINTENANCE',
    currentKm: null,
    currentHours: 3200,
    acquisitionType: 'LEASING',
    acquisitionDate: '2020-06-15',
    purchasePrice: null,
    amortizationYears: null,
    monthlyFee: 1500,
    contractEndDate: '2027-06-15',
    createdAt: '2026-02-15T09:00:00Z',
  },
]

// The mock has no assignment table — the driver's own vehicle is always the first seed vehicle.
const DRIVER_VEHICLE_ID = SEED_VEHICLES[0]!.id

let vehicles: Vehicle[] = [...SEED_VEHICLES]

export function resetVehiclesMock() {
  vehicles = [...SEED_VEHICLES]
}

type WorkerRole = 'DRIVER' | 'TECHNICIAN' | 'BOTH'

type Worker = {
  id: string
  firstName: string
  lastName: string
  fullName: string
  workerRole: WorkerRole
  nationalId: string
  phone: string | null
  licenseType: string | null
  licenseExpiry: string | null
  userId: string | null
  createdAt: string
}

type WorkerRequestBody = {
  firstName: string
  lastName: string
  workerRole: WorkerRole
  nationalId: string
  phone?: string | null
  licenseType?: string | null
  licenseExpiry?: string | null
  userId?: string | null
}

export const SEED_WORKERS: Worker[] = [
  {
    id: 'worker-1',
    firstName: 'Carlos',
    lastName: 'Gómez',
    fullName: 'Carlos Gómez',
    workerRole: 'DRIVER',
    nationalId: '12345678A',
    phone: '+34611111111',
    licenseType: 'C',
    licenseExpiry: '2027-05-01',
    userId: 'user-driver-1',
    createdAt: '2026-01-05T09:00:00Z',
  },
  {
    id: 'worker-2',
    firstName: 'Laura',
    lastName: 'Fernández',
    fullName: 'Laura Fernández',
    workerRole: 'TECHNICIAN',
    nationalId: '87654321B',
    phone: '+34622222222',
    licenseType: null,
    licenseExpiry: null,
    userId: null,
    createdAt: '2026-02-01T09:00:00Z',
  },
  {
    id: 'worker-3',
    firstName: 'Marta',
    lastName: 'Ruiz',
    fullName: 'Marta Ruiz',
    workerRole: 'DRIVER',
    nationalId: '11223344C',
    phone: '+34633333333',
    licenseType: 'C',
    licenseExpiry: '2028-09-01',
    userId: 'user-driver-2',
    createdAt: '2026-03-01T09:00:00Z',
  },
  {
    id: 'worker-4',
    firstName: 'Pablo',
    lastName: 'Sánchez',
    fullName: 'Pablo Sánchez',
    workerRole: 'DRIVER',
    nationalId: '55667788D',
    phone: '+34644444444',
    licenseType: 'C',
    licenseExpiry: '2029-01-01',
    userId: 'user-driver-3',
    createdAt: '2026-03-05T09:00:00Z',
  },
]

// The mock has no assignment table — the driver's own profile is always the first seed worker.
const DRIVER_WORKER_ID = SEED_WORKERS[0]!.id

let workers: Worker[] = [...SEED_WORKERS]

export function resetWorkersMock() {
  workers = [...SEED_WORKERS]
}

type Assignment = {
  id: string
  driverId: string
  driverName: string
  vehicleId: string
  vehicleLicensePlate: string | null
  vehicleMake: string | null
  vehicleModel: string | null
  startDate: string
  endDate: string | null
  assignedByUserId: string
  notes: string | null
  createdAt: string
  active: boolean
}

type AssignmentRequestBody = {
  driverId: string
  vehicleId: string
  startDate: string
  notes?: string | null
}

const ASSIGNED_BY_USER_ID = 'user-admin-1'

export const SEED_ASSIGNMENTS: Assignment[] = [
  {
    id: 'assignment-1',
    driverId: DRIVER_WORKER_ID,
    driverName: SEED_WORKERS[0]!.fullName,
    vehicleId: DRIVER_VEHICLE_ID,
    vehicleLicensePlate: SEED_VEHICLES[0]!.licensePlate,
    vehicleMake: SEED_VEHICLES[0]!.make,
    vehicleModel: SEED_VEHICLES[0]!.model,
    startDate: '2026-01-15',
    endDate: null,
    assignedByUserId: ASSIGNED_BY_USER_ID,
    notes: null,
    createdAt: '2026-01-15T09:00:00Z',
    active: true,
  },
  {
    id: 'assignment-2',
    driverId: SEED_WORKERS[3]!.id,
    driverName: SEED_WORKERS[3]!.fullName,
    vehicleId: SEED_VEHICLES[1]!.id,
    vehicleLicensePlate: SEED_VEHICLES[1]!.licensePlate,
    vehicleMake: SEED_VEHICLES[1]!.make,
    vehicleModel: SEED_VEHICLES[1]!.model,
    startDate: '2026-02-20',
    endDate: null,
    assignedByUserId: ASSIGNED_BY_USER_ID,
    notes: null,
    createdAt: '2026-02-20T09:00:00Z',
    active: true,
  },
]

let assignments: Assignment[] = [...SEED_ASSIGNMENTS]

export function resetAssignmentsMock() {
  assignments = [...SEED_ASSIGNMENTS]
}

type JobStatus = 'PENDING' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED'

type Job = {
  id: string
  title: string
  description: string | null
  vehicleId: string
  vehicleLicensePlate: string | null
  vehicleMake: string | null
  vehicleModel: string | null
  assignedDriverId: string | null
  assignedDriverName: string | null
  clientId: string | null
  clientName: string | null
  status: JobStatus
  originLocation: string
  destinationLocation: string
  notes: string | null
  scheduledStart: string | null
  scheduledEnd: string | null
  actualStart: string | null
  actualEnd: string | null
  startUsageValue: number | null
  endUsageValue: number | null
  createdAt: string
}

type JobRequestBody = {
  vehicleId: string
  assignedDriverId?: string | null
  clientId?: string | null
  title: string
  description?: string | null
  originLocation: string
  destinationLocation: string
  notes?: string | null
  scheduledStart?: string | null
  scheduledEnd?: string | null
}

const JOB_ACTIVE_STATUSES: JobStatus[] = ['PENDING', 'IN_PROGRESS']

export const SEED_JOBS: Job[] = [
  {
    id: 'job-1',
    title: 'Entrega urgente',
    description: null,
    vehicleId: SEED_VEHICLES[0]!.id,
    vehicleLicensePlate: SEED_VEHICLES[0]!.licensePlate,
    vehicleMake: SEED_VEHICLES[0]!.make,
    vehicleModel: SEED_VEHICLES[0]!.model,
    assignedDriverId: DRIVER_WORKER_ID,
    assignedDriverName: SEED_WORKERS[0]!.fullName,
    clientId: SEED_CLIENTS[0]!.id,
    clientName: SEED_CLIENTS[0]!.name,
    status: 'PENDING',
    originLocation: 'Almacén Central',
    destinationLocation: 'Cliente Acme',
    notes: null,
    scheduledStart: '2026-07-10T08:00:00Z',
    scheduledEnd: null,
    actualStart: null,
    actualEnd: null,
    startUsageValue: null,
    endUsageValue: null,
    createdAt: '2026-07-01T09:00:00Z',
  },
  {
    id: 'job-2',
    title: 'Reparto semanal',
    description: 'Ruta habitual de reparto en zona norte',
    vehicleId: SEED_VEHICLES[0]!.id,
    vehicleLicensePlate: SEED_VEHICLES[0]!.licensePlate,
    vehicleMake: SEED_VEHICLES[0]!.make,
    vehicleModel: SEED_VEHICLES[0]!.model,
    assignedDriverId: DRIVER_WORKER_ID,
    assignedDriverName: SEED_WORKERS[0]!.fullName,
    clientId: null,
    clientName: null,
    status: 'IN_PROGRESS',
    originLocation: 'Almacén Central',
    destinationLocation: 'Zona Norte',
    notes: null,
    scheduledStart: '2026-07-05T08:00:00Z',
    scheduledEnd: null,
    actualStart: '2026-07-05T08:10:00Z',
    actualEnd: null,
    startUsageValue: 14500,
    endUsageValue: null,
    createdAt: '2026-07-04T09:00:00Z',
  },
  {
    id: 'job-3',
    title: 'Entrega finalizada',
    description: null,
    vehicleId: SEED_VEHICLES[0]!.id,
    vehicleLicensePlate: SEED_VEHICLES[0]!.licensePlate,
    vehicleMake: SEED_VEHICLES[0]!.make,
    vehicleModel: SEED_VEHICLES[0]!.model,
    assignedDriverId: DRIVER_WORKER_ID,
    assignedDriverName: SEED_WORKERS[0]!.fullName,
    clientId: SEED_CLIENTS[1]!.id,
    clientName: SEED_CLIENTS[1]!.name,
    status: 'COMPLETED',
    originLocation: 'Almacén Central',
    destinationLocation: 'Cliente Ibérica',
    notes: null,
    scheduledStart: '2026-06-20T08:00:00Z',
    scheduledEnd: null,
    actualStart: '2026-06-20T08:05:00Z',
    actualEnd: '2026-06-20T12:00:00Z',
    startUsageValue: 14000,
    endUsageValue: 14300,
    createdAt: '2026-06-19T09:00:00Z',
  },
  {
    id: 'job-4',
    title: 'Traslado de excavadora',
    description: null,
    vehicleId: SEED_VEHICLES[1]!.id,
    vehicleLicensePlate: SEED_VEHICLES[1]!.licensePlate,
    vehicleMake: SEED_VEHICLES[1]!.make,
    vehicleModel: SEED_VEHICLES[1]!.model,
    assignedDriverId: null,
    assignedDriverName: null,
    clientId: null,
    clientName: null,
    status: 'PENDING',
    originLocation: 'Taller',
    destinationLocation: 'Obra Norte',
    notes: null,
    scheduledStart: null,
    scheduledEnd: null,
    actualStart: null,
    actualEnd: null,
    startUsageValue: null,
    endUsageValue: null,
    createdAt: '2026-07-02T09:00:00Z',
  },
]

let jobs: Job[] = [...SEED_JOBS]

export function resetJobsMock() {
  jobs = [...SEED_JOBS]
}

type MaintenanceStatus = 'SCHEDULED' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED'
type MaintenanceCategory = 'PREVENTIVE' | 'CORRECTIVE'

type MaintenanceRecordMock = {
  id: string
  vehicleId: string
  vehicleLicensePlate: string | null
  vehicleMake: string | null
  vehicleModel: string | null
  type: string
  description: string | null
  usageAtService: number | null
  cost: number | null
  workshopEntryDate: string | null
  workshopExitDate: string | null
  technicianId: string | null
  technicianName: string | null
  invoiceId: string | null
  status: MaintenanceStatus
  category: MaintenanceCategory
  createdAt: string
}

type MaintenanceRequestBody = {
  vehicleId: string
  type: string
  description?: string | null
  technicianId?: string | null
  category?: MaintenanceCategory | null
  scheduledDate: string
}

type MaintenanceUpdateRequestBody = {
  vehicleId: string
  type: string
  description?: string | null
  technicianId?: string | null
  category: MaintenanceCategory
}

const TECHNICIAN_WORKER_ID = SEED_WORKERS[1]!.id

export const SEED_MAINTENANCE: MaintenanceRecordMock[] = [
  {
    id: 'maintenance-1',
    vehicleId: SEED_VEHICLES[0]!.id,
    vehicleLicensePlate: SEED_VEHICLES[0]!.licensePlate,
    vehicleMake: SEED_VEHICLES[0]!.make,
    vehicleModel: SEED_VEHICLES[0]!.model,
    type: 'Cambio de aceite y filtro',
    description: null,
    usageAtService: null,
    cost: null,
    workshopEntryDate: null,
    workshopExitDate: null,
    technicianId: TECHNICIAN_WORKER_ID,
    technicianName: SEED_WORKERS[1]!.fullName,
    invoiceId: null,
    status: 'SCHEDULED',
    category: 'PREVENTIVE',
    createdAt: '2026-07-01T09:00:00Z',
  },
  {
    id: 'maintenance-2',
    vehicleId: SEED_VEHICLES[0]!.id,
    vehicleLicensePlate: SEED_VEHICLES[0]!.licensePlate,
    vehicleMake: SEED_VEHICLES[0]!.make,
    vehicleModel: SEED_VEHICLES[0]!.model,
    type: 'Cambio de pastillas de freno',
    description: 'Ruido al frenar reportado por el conductor',
    usageAtService: 14800,
    cost: null,
    workshopEntryDate: '2026-07-03',
    workshopExitDate: null,
    technicianId: TECHNICIAN_WORKER_ID,
    technicianName: SEED_WORKERS[1]!.fullName,
    invoiceId: null,
    status: 'IN_PROGRESS',
    category: 'CORRECTIVE',
    createdAt: '2026-07-02T09:00:00Z',
  },
  {
    id: 'maintenance-3',
    vehicleId: SEED_VEHICLES[1]!.id,
    vehicleLicensePlate: SEED_VEHICLES[1]!.licensePlate,
    vehicleMake: SEED_VEHICLES[1]!.make,
    vehicleModel: SEED_VEHICLES[1]!.model,
    type: 'Cambio de filtro',
    description: null,
    usageAtService: 3100,
    cost: 85.5,
    workshopEntryDate: '2026-06-10',
    workshopExitDate: '2026-06-11',
    technicianId: null,
    technicianName: null,
    invoiceId: null,
    status: 'COMPLETED',
    category: 'PREVENTIVE',
    createdAt: '2026-06-09T09:00:00Z',
  },
]

let maintenanceRecords: MaintenanceRecordMock[] = [...SEED_MAINTENANCE]

export function resetMaintenanceMock() {
  maintenanceRecords = [...SEED_MAINTENANCE]
}

type SchedulePriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT'
type WorkshopStatus = 'PENDING' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED'
type ScheduleRangeValue = 'today' | 'week' | 'month'

// The mock filters by an explicit `rangeTags` membership list instead of replicating the
// backend's real date-window math (ISO week / calendar month) — that boundary logic is
// already covered by WorkshopScheduleRepositoryTest on the backend; here we only need the
// range selector to request a different range and receive a different filtered list.
type WorkshopScheduleMock = {
  id: string
  vehicleId: string
  vehicleLicensePlate: string | null
  vehicleMake: string | null
  vehicleModel: string | null
  technicianId: string | null
  technicianName: string | null
  maintenanceRecordId: string | null
  maintenanceCategory: MaintenanceCategory | null
  scheduledDate: string
  type: string
  priority: SchedulePriority
  status: WorkshopStatus
  notes: string | null
  createdAt: string
  rangeTags: ScheduleRangeValue[]
}

type ScheduleRequestBody = {
  vehicleId: string
  technicianId?: string | null
  maintenanceRecordId?: string | null
  scheduledDate: string
  type: string
  priority?: SchedulePriority | null
  notes?: string | null
}

type ScheduleUpdateRequestBody = {
  vehicleId: string
  technicianId?: string | null
  maintenanceRecordId?: string | null
  scheduledDate: string
  type: string
  priority: SchedulePriority
  notes?: string | null
}

export const SEED_SCHEDULES: WorkshopScheduleMock[] = [
  {
    id: 'schedule-1',
    vehicleId: SEED_VEHICLES[0]!.id,
    vehicleLicensePlate: SEED_VEHICLES[0]!.licensePlate,
    vehicleMake: SEED_VEHICLES[0]!.make,
    vehicleModel: SEED_VEHICLES[0]!.model,
    technicianId: TECHNICIAN_WORKER_ID,
    technicianName: SEED_WORKERS[1]!.fullName,
    maintenanceRecordId: SEED_MAINTENANCE[0]!.id,
    maintenanceCategory: SEED_MAINTENANCE[0]!.category,
    scheduledDate: '2026-07-09',
    type: 'Cambio de aceite',
    priority: 'MEDIUM',
    status: 'PENDING',
    notes: null,
    createdAt: '2026-07-01T09:00:00Z',
    rangeTags: ['today', 'week', 'month'],
  },
  {
    id: 'schedule-2',
    vehicleId: SEED_VEHICLES[0]!.id,
    vehicleLicensePlate: SEED_VEHICLES[0]!.licensePlate,
    vehicleMake: SEED_VEHICLES[0]!.make,
    vehicleModel: SEED_VEHICLES[0]!.model,
    technicianId: TECHNICIAN_WORKER_ID,
    technicianName: SEED_WORKERS[1]!.fullName,
    maintenanceRecordId: SEED_MAINTENANCE[1]!.id,
    maintenanceCategory: SEED_MAINTENANCE[1]!.category,
    scheduledDate: '2026-07-11',
    type: 'Revisión de frenos',
    priority: 'HIGH',
    status: 'IN_PROGRESS',
    notes: null,
    createdAt: '2026-07-02T09:00:00Z',
    rangeTags: ['week', 'month'],
  },
  {
    id: 'schedule-3',
    vehicleId: SEED_VEHICLES[1]!.id,
    vehicleLicensePlate: SEED_VEHICLES[1]!.licensePlate,
    vehicleMake: SEED_VEHICLES[1]!.make,
    vehicleModel: SEED_VEHICLES[1]!.model,
    technicianId: null,
    technicianName: null,
    maintenanceRecordId: null,
    maintenanceCategory: null,
    scheduledDate: '2026-07-25',
    type: 'Revisión general',
    priority: 'LOW',
    status: 'PENDING',
    notes: null,
    createdAt: '2026-07-03T09:00:00Z',
    rangeTags: ['month'],
  },
]

let workshopSchedules: WorkshopScheduleMock[] = [...SEED_SCHEDULES]

export function resetWorkshopSchedulesMock() {
  workshopSchedules = [...SEED_SCHEDULES]
}

function buildWorkshopScheduleMock(params: {
  vehicle: Vehicle
  technician?: Worker | null
  maintenanceRecordId?: string | null
  maintenanceCategory?: MaintenanceCategory | null
  scheduledDate: string
  type: string
  priority: SchedulePriority
  notes?: string | null
}): WorkshopScheduleMock {
  return {
    id: `schedule-${workshopSchedules.length + 1}`,
    vehicleId: params.vehicle.id,
    vehicleLicensePlate: params.vehicle.licensePlate,
    vehicleMake: params.vehicle.make,
    vehicleModel: params.vehicle.model,
    technicianId: params.technician?.id ?? null,
    technicianName: params.technician?.fullName ?? null,
    maintenanceRecordId: params.maintenanceRecordId ?? null,
    maintenanceCategory: params.maintenanceCategory ?? null,
    scheduledDate: params.scheduledDate,
    type: params.type,
    priority: params.priority,
    status: 'PENDING',
    notes: params.notes ?? null,
    createdAt: new Date().toISOString(),
    // Always tagged for every range, regardless of scheduledDate — deliberate mock
    // simplification, see the comment on WorkshopScheduleMock above.
    rangeTags: ['today', 'week', 'month'],
  }
}

export const handlers = [
  http.get('/api/v1/clients', ({ request }) => {
    const url = new URL(request.url)
    const page = Number(url.searchParams.get('page') ?? 0)
    const size = Number(url.searchParams.get('size') ?? 20)
    const start = page * size
    const content = clients.slice(start, start + size)

    return HttpResponse.json({
      content,
      page,
      size,
      totalElements: clients.length,
      totalPages: Math.max(1, Math.ceil(clients.length / size)),
    })
  }),

  http.post('/api/v1/clients', async ({ request }) => {
    const body = (await request.json()) as ClientRequestBody

    const newClient: Client = {
      id: `client-${clients.length + 1}`,
      name: body.name,
      taxId: body.taxId,
      email: body.email ?? null,
      phone: body.phone ?? null,
      address: body.address ?? null,
      createdAt: new Date().toISOString(),
    }
    clients = [...clients, newClient]

    return HttpResponse.json(newClient, { status: 201 })
  }),

  http.put('/api/v1/clients/:id', async ({ request, params }) => {
    const body = (await request.json()) as ClientRequestBody
    const index = clients.findIndex((client) => client.id === params.id)
    const existing = clients[index]

    if (!existing) {
      return HttpResponse.json(
        {
          status: 404,
          code: 'CLIENT_NOT_FOUND',
          message: `Client ${params.id} not found`,
          correlationId: 'test-correlation-id',
        },
        { status: 404 },
      )
    }

    const updated: Client = {
      ...existing,
      name: body.name,
      taxId: body.taxId,
      email: body.email ?? null,
      phone: body.phone ?? null,
      address: body.address ?? null,
    }
    clients = clients.map((client, i) => (i === index ? updated : client))

    return HttpResponse.json(updated)
  }),

  http.delete('/api/v1/clients/:id', ({ params }) => {
    clients = clients.filter((client) => client.id !== params.id)
    return new HttpResponse(null, { status: 204 })
  }),

  http.get('/api/v1/vehicles', ({ request }) => {
    const url = new URL(request.url)
    const page = Number(url.searchParams.get('page') ?? 0)
    const size = Number(url.searchParams.get('size') ?? 20)

    if (useAuthStore.getState().role === 'DRIVER') {
      const own = vehicles.find((vehicle) => vehicle.id === DRIVER_VEHICLE_ID)
      const content = own ? [own] : []
      return HttpResponse.json({
        content,
        page: 0,
        size: 1,
        totalElements: content.length,
        totalPages: content.length > 0 ? 1 : 0,
      })
    }

    const start = page * size
    const content = vehicles.slice(start, start + size)

    return HttpResponse.json({
      content,
      page,
      size,
      totalElements: vehicles.length,
      totalPages: Math.max(1, Math.ceil(vehicles.length / size)),
    })
  }),

  http.post('/api/v1/vehicles', async ({ request }) => {
    const body = (await request.json()) as VehicleRequestBody

    const newVehicle: Vehicle = {
      id: `vehicle-${vehicles.length + 1}`,
      vehicleCategory: body.vehicleCategory,
      usageMeasure: body.usageMeasure,
      make: body.make,
      model: body.model,
      year: body.year,
      licensePlate: body.licensePlate ?? null,
      heavySubtype: body.heavySubtype ?? null,
      vin: body.vin ?? null,
      color: body.color ?? null,
      status: 'ACTIVE',
      currentKm: body.currentKm ?? null,
      currentHours: body.currentHours ?? null,
      acquisitionType: body.acquisitionType ?? null,
      acquisitionDate: body.acquisitionDate ?? null,
      purchasePrice: body.purchasePrice ?? null,
      amortizationYears: body.amortizationYears ?? null,
      monthlyFee: body.monthlyFee ?? null,
      contractEndDate: body.contractEndDate ?? null,
      createdAt: new Date().toISOString(),
    }
    vehicles = [...vehicles, newVehicle]

    return HttpResponse.json(newVehicle, { status: 201 })
  }),

  http.put('/api/v1/vehicles/:id', async ({ request, params }) => {
    const body = (await request.json()) as VehicleRequestBody
    const index = vehicles.findIndex((vehicle) => vehicle.id === params.id)
    const existing = vehicles[index]

    if (!existing) {
      return HttpResponse.json(
        {
          status: 404,
          code: 'VEHICLE_NOT_FOUND',
          message: `Vehicle ${params.id} not found`,
          correlationId: 'test-correlation-id',
        },
        { status: 404 },
      )
    }

    const updated: Vehicle = {
      ...existing,
      vehicleCategory: body.vehicleCategory,
      usageMeasure: body.usageMeasure,
      make: body.make,
      model: body.model,
      year: body.year,
      licensePlate: body.licensePlate ?? null,
      heavySubtype: body.heavySubtype ?? null,
      vin: body.vin ?? null,
      color: body.color ?? null,
      acquisitionType: body.acquisitionType ?? null,
      acquisitionDate: body.acquisitionDate ?? null,
      purchasePrice: body.purchasePrice ?? null,
      amortizationYears: body.amortizationYears ?? null,
      monthlyFee: body.monthlyFee ?? null,
      contractEndDate: body.contractEndDate ?? null,
    }
    vehicles = vehicles.map((vehicle, i) => (i === index ? updated : vehicle))

    return HttpResponse.json(updated)
  }),

  http.delete('/api/v1/vehicles/:id', ({ params }) => {
    vehicles = vehicles.filter((vehicle) => vehicle.id !== params.id)
    return new HttpResponse(null, { status: 204 })
  }),

  http.get('/api/v1/workers', ({ request }) => {
    const url = new URL(request.url)
    const page = Number(url.searchParams.get('page') ?? 0)
    const size = Number(url.searchParams.get('size') ?? 20)

    if (useAuthStore.getState().role === 'DRIVER') {
      const own = workers.find((worker) => worker.id === DRIVER_WORKER_ID)
      const content = own ? [own] : []
      return HttpResponse.json({
        content,
        page: 0,
        size: 1,
        totalElements: content.length,
        totalPages: content.length > 0 ? 1 : 0,
      })
    }

    const start = page * size
    const content = workers.slice(start, start + size)

    return HttpResponse.json({
      content,
      page,
      size,
      totalElements: workers.length,
      totalPages: Math.max(1, Math.ceil(workers.length / size)),
    })
  }),

  http.post('/api/v1/workers', async ({ request }) => {
    const body = (await request.json()) as WorkerRequestBody

    const newWorker: Worker = {
      id: `worker-${workers.length + 1}`,
      firstName: body.firstName,
      lastName: body.lastName,
      fullName: `${body.firstName} ${body.lastName}`,
      workerRole: body.workerRole,
      nationalId: body.nationalId,
      phone: body.phone ?? null,
      licenseType: body.licenseType ?? null,
      licenseExpiry: body.licenseExpiry ?? null,
      userId: body.userId ?? null,
      createdAt: new Date().toISOString(),
    }
    workers = [...workers, newWorker]

    return HttpResponse.json(newWorker, { status: 201 })
  }),

  http.put('/api/v1/workers/:id', async ({ request, params }) => {
    const body = (await request.json()) as WorkerRequestBody
    const index = workers.findIndex((worker) => worker.id === params.id)
    const existing = workers[index]

    if (!existing) {
      return HttpResponse.json(
        {
          status: 404,
          code: 'WORKER_NOT_FOUND',
          message: `Worker ${params.id} not found`,
          correlationId: 'test-correlation-id',
        },
        { status: 404 },
      )
    }

    const updated: Worker = {
      ...existing,
      firstName: body.firstName,
      lastName: body.lastName,
      fullName: `${body.firstName} ${body.lastName}`,
      workerRole: body.workerRole,
      phone: body.phone ?? null,
      licenseType: body.licenseType ?? null,
      licenseExpiry: body.licenseExpiry ?? null,
    }
    workers = workers.map((worker, i) => (i === index ? updated : worker))

    return HttpResponse.json(updated)
  }),

  http.delete('/api/v1/workers/:id', ({ params }) => {
    workers = workers.filter((worker) => worker.id !== params.id)
    return new HttpResponse(null, { status: 204 })
  }),

  http.post('/api/v1/assignments', async ({ request }) => {
    const body = (await request.json()) as AssignmentRequestBody

    const driver = workers.find((worker) => worker.id === body.driverId)
    if (!driver) {
      return HttpResponse.json(
        {
          status: 404,
          code: 'WORKER_NOT_FOUND',
          message: `Worker ${body.driverId} not found`,
          correlationId: 'test-correlation-id',
        },
        { status: 404 },
      )
    }

    const vehicle = vehicles.find((v) => v.id === body.vehicleId)
    if (!vehicle) {
      return HttpResponse.json(
        {
          status: 404,
          code: 'VEHICLE_NOT_FOUND',
          message: `Vehicle ${body.vehicleId} not found`,
          correlationId: 'test-correlation-id',
        },
        { status: 404 },
      )
    }

    if (assignments.some((assignment) => assignment.driverId === body.driverId && assignment.active)) {
      return HttpResponse.json(
        {
          status: 409,
          code: 'ASSIGNMENT_DRIVER_ALREADY_ACTIVE',
          message: `Driver ${body.driverId} already has an active assignment`,
          correlationId: 'test-correlation-id',
        },
        { status: 409 },
      )
    }

    const newAssignment: Assignment = {
      id: `assignment-${assignments.length + 1}`,
      driverId: driver.id,
      driverName: driver.fullName,
      vehicleId: vehicle.id,
      vehicleLicensePlate: vehicle.licensePlate,
      vehicleMake: vehicle.make,
      vehicleModel: vehicle.model,
      startDate: body.startDate,
      endDate: null,
      assignedByUserId: ASSIGNED_BY_USER_ID,
      notes: body.notes ?? null,
      createdAt: new Date().toISOString(),
      active: true,
    }
    assignments = [...assignments, newAssignment]

    return HttpResponse.json(newAssignment, { status: 201 })
  }),

  http.patch('/api/v1/assignments/:id/end', ({ params }) => {
    const index = assignments.findIndex((assignment) => assignment.id === params.id)
    const existing = assignments[index]

    if (!existing) {
      return HttpResponse.json(
        {
          status: 404,
          code: 'ASSIGNMENT_NOT_FOUND',
          message: `Assignment ${params.id} not found`,
          correlationId: 'test-correlation-id',
        },
        { status: 404 },
      )
    }

    const updated: Assignment = {
      ...existing,
      endDate: new Date().toISOString().slice(0, 10),
      active: false,
    }
    assignments = assignments.map((assignment, i) => (i === index ? updated : assignment))

    return HttpResponse.json(updated)
  }),

  http.get('/api/v1/workers/:workerId/assignments', ({ request, params }) => {
    const url = new URL(request.url)
    const page = Number(url.searchParams.get('page') ?? 0)
    const size = Number(url.searchParams.get('size') ?? 20)

    const content = assignments.filter((assignment) => assignment.driverId === params.workerId)
    const start = page * size
    const pageContent = content.slice(start, start + size)

    return HttpResponse.json({
      content: pageContent,
      page,
      size,
      totalElements: content.length,
      totalPages: Math.max(1, Math.ceil(content.length / size)),
    })
  }),

  http.get('/api/v1/vehicles/:vehicleId/assignment', ({ params }) => {
    const active = assignments.find(
      (assignment) => assignment.vehicleId === params.vehicleId && assignment.active,
    )

    if (!active) {
      return new HttpResponse(null, { status: 204 })
    }

    return HttpResponse.json(active)
  }),

  http.get('/api/v1/assignments/active', ({ request }) => {
    const url = new URL(request.url)
    const driverIds = (url.searchParams.get('driverIds') ?? '').split(',').filter(Boolean)

    const content = assignments.filter(
      (assignment) => assignment.active && driverIds.includes(assignment.driverId),
    )

    return HttpResponse.json(content)
  }),

  http.get('/api/v1/jobs', ({ request }) => {
    const url = new URL(request.url)
    const page = Number(url.searchParams.get('page') ?? 0)
    const size = Number(url.searchParams.get('size') ?? 20)

    const source =
      useAuthStore.getState().role === 'DRIVER'
        ? jobs.filter(
            (job) => job.assignedDriverId === DRIVER_WORKER_ID && JOB_ACTIVE_STATUSES.includes(job.status),
          )
        : jobs

    const start = page * size
    const content = source.slice(start, start + size)

    return HttpResponse.json({
      content,
      page,
      size,
      totalElements: source.length,
      totalPages: Math.max(1, Math.ceil(source.length / size)),
    })
  }),

  http.post('/api/v1/jobs', async ({ request }) => {
    const body = (await request.json()) as JobRequestBody

    const vehicle = vehicles.find((v) => v.id === body.vehicleId)
    if (!vehicle) {
      return HttpResponse.json(
        {
          status: 404,
          code: 'VEHICLE_NOT_FOUND',
          message: `Vehicle ${body.vehicleId} not found`,
          correlationId: 'test-correlation-id',
        },
        { status: 404 },
      )
    }

    let driver: Worker | undefined
    if (body.assignedDriverId) {
      driver = workers.find((w) => w.id === body.assignedDriverId)
      if (!driver) {
        return HttpResponse.json(
          {
            status: 404,
            code: 'WORKER_NOT_FOUND',
            message: `Worker ${body.assignedDriverId} not found`,
            correlationId: 'test-correlation-id',
          },
          { status: 404 },
        )
      }
    }

    let client: Client | undefined
    if (body.clientId) {
      client = clients.find((c) => c.id === body.clientId)
      if (!client) {
        return HttpResponse.json(
          {
            status: 404,
            code: 'CLIENT_NOT_FOUND',
            message: `Client ${body.clientId} not found`,
            correlationId: 'test-correlation-id',
          },
          { status: 404 },
        )
      }
    }

    const newJob: Job = {
      id: `job-${jobs.length + 1}`,
      title: body.title,
      description: body.description ?? null,
      vehicleId: vehicle.id,
      vehicleLicensePlate: vehicle.licensePlate,
      vehicleMake: vehicle.make,
      vehicleModel: vehicle.model,
      assignedDriverId: driver?.id ?? null,
      assignedDriverName: driver?.fullName ?? null,
      clientId: client?.id ?? null,
      clientName: client?.name ?? null,
      status: 'PENDING',
      originLocation: body.originLocation,
      destinationLocation: body.destinationLocation,
      notes: body.notes ?? null,
      scheduledStart: body.scheduledStart ?? null,
      scheduledEnd: body.scheduledEnd ?? null,
      actualStart: null,
      actualEnd: null,
      startUsageValue: null,
      endUsageValue: null,
      createdAt: new Date().toISOString(),
    }
    jobs = [...jobs, newJob]

    return HttpResponse.json(newJob, { status: 201 })
  }),

  http.patch('/api/v1/jobs/:id/start', async ({ request, params }) => {
    const index = jobs.findIndex((job) => job.id === params.id)
    const existing = jobs[index]

    if (!existing) {
      return HttpResponse.json(
        {
          status: 404,
          code: 'JOB_NOT_FOUND',
          message: `Job ${params.id} not found`,
          correlationId: 'test-correlation-id',
        },
        { status: 404 },
      )
    }

    if (existing.status !== 'PENDING') {
      return HttpResponse.json(
        {
          status: 409,
          code: 'JOB_INVALID_STATE_TRANSITION',
          message: `Job ${params.id} cannot be started from state ${existing.status}`,
          correlationId: 'test-correlation-id',
        },
        { status: 409 },
      )
    }

    const body = (await request.json().catch(() => null)) as { startUsageValue?: number | null } | null

    const updated: Job = {
      ...existing,
      status: 'IN_PROGRESS',
      actualStart: new Date().toISOString(),
      startUsageValue: body?.startUsageValue ?? existing.startUsageValue,
    }
    jobs = jobs.map((job, i) => (i === index ? updated : job))

    return HttpResponse.json(updated)
  }),

  http.patch('/api/v1/jobs/:id/complete', async ({ request, params }) => {
    const index = jobs.findIndex((job) => job.id === params.id)
    const existing = jobs[index]

    if (!existing) {
      return HttpResponse.json(
        {
          status: 404,
          code: 'JOB_NOT_FOUND',
          message: `Job ${params.id} not found`,
          correlationId: 'test-correlation-id',
        },
        { status: 404 },
      )
    }

    if (existing.status !== 'IN_PROGRESS') {
      return HttpResponse.json(
        {
          status: 409,
          code: 'JOB_INVALID_STATE_TRANSITION',
          message: `Job ${params.id} cannot be completed from state ${existing.status}`,
          correlationId: 'test-correlation-id',
        },
        { status: 409 },
      )
    }

    const body = (await request.json().catch(() => null)) as { endUsageValue?: number | null } | null
    const endUsageValue = body?.endUsageValue ?? existing.endUsageValue

    const vehicle = vehicles.find((v) => v.id === existing.vehicleId)
    const currentUsage = vehicle
      ? vehicle.usageMeasure === 'KILOMETERS'
        ? vehicle.currentKm
        : vehicle.currentHours
      : null
    const floor = Math.max(currentUsage ?? -Infinity, existing.startUsageValue ?? -Infinity)
    if (endUsageValue != null && floor !== -Infinity && endUsageValue < floor) {
      return HttpResponse.json(
        {
          status: 409,
          code: 'JOB_USAGE_VALUE_BELOW_CURRENT',
          message: `endUsageValue ${endUsageValue} for job ${params.id} is lower than the current recorded usage (${floor})`,
          correlationId: 'test-correlation-id',
        },
        { status: 409 },
      )
    }

    const updated: Job = {
      ...existing,
      status: 'COMPLETED',
      actualEnd: new Date().toISOString(),
      endUsageValue,
    }
    jobs = jobs.map((job, i) => (i === index ? updated : job))

    if (vehicle && endUsageValue != null) {
      vehicles = vehicles.map((v) =>
        v.id === vehicle.id
          ? {
              ...v,
              currentKm: v.usageMeasure === 'KILOMETERS' ? endUsageValue : v.currentKm,
              currentHours: v.usageMeasure === 'HOURS' ? endUsageValue : v.currentHours,
            }
          : v,
      )
    }

    return HttpResponse.json(updated)
  }),

  http.patch('/api/v1/jobs/:id/cancel', ({ params }) => {
    const index = jobs.findIndex((job) => job.id === params.id)
    const existing = jobs[index]

    if (!existing) {
      return HttpResponse.json(
        {
          status: 404,
          code: 'JOB_NOT_FOUND',
          message: `Job ${params.id} not found`,
          correlationId: 'test-correlation-id',
        },
        { status: 404 },
      )
    }

    if (existing.status !== 'PENDING' && existing.status !== 'IN_PROGRESS') {
      return HttpResponse.json(
        {
          status: 409,
          code: 'JOB_INVALID_STATE_TRANSITION',
          message: `Job ${params.id} cannot be cancelled from state ${existing.status}`,
          correlationId: 'test-correlation-id',
        },
        { status: 409 },
      )
    }

    const updated: Job = { ...existing, status: 'CANCELLED' }
    jobs = jobs.map((job, i) => (i === index ? updated : job))

    return HttpResponse.json(updated)
  }),

  http.get('/api/v1/maintenance', ({ request }) => {
    const url = new URL(request.url)
    const page = Number(url.searchParams.get('page') ?? 0)
    const size = Number(url.searchParams.get('size') ?? 20)
    const start = page * size
    const content = maintenanceRecords.slice(start, start + size)

    return HttpResponse.json({
      content,
      page,
      size,
      totalElements: maintenanceRecords.length,
      totalPages: Math.max(1, Math.ceil(maintenanceRecords.length / size)),
    })
  }),

  http.post('/api/v1/maintenance', async ({ request }) => {
    const body = (await request.json()) as MaintenanceRequestBody

    const vehicle = vehicles.find((v) => v.id === body.vehicleId)
    if (!vehicle) {
      return HttpResponse.json(
        {
          status: 404,
          code: 'VEHICLE_NOT_FOUND',
          message: `Vehicle ${body.vehicleId} not found`,
          correlationId: 'test-correlation-id',
        },
        { status: 404 },
      )
    }

    let technician: Worker | undefined
    if (body.technicianId) {
      technician = workers.find((w) => w.id === body.technicianId)
      if (!technician) {
        return HttpResponse.json(
          {
            status: 404,
            code: 'WORKER_NOT_FOUND',
            message: `Worker ${body.technicianId} not found`,
            correlationId: 'test-correlation-id',
          },
          { status: 404 },
        )
      }
    }

    const newRecord: MaintenanceRecordMock = {
      id: `maintenance-${maintenanceRecords.length + 1}`,
      vehicleId: vehicle.id,
      vehicleLicensePlate: vehicle.licensePlate,
      vehicleMake: vehicle.make,
      vehicleModel: vehicle.model,
      type: body.type,
      description: body.description ?? null,
      usageAtService: null,
      cost: null,
      workshopEntryDate: null,
      workshopExitDate: null,
      technicianId: technician?.id ?? null,
      technicianName: technician?.fullName ?? null,
      invoiceId: null,
      status: 'SCHEDULED',
      category: body.category ?? 'PREVENTIVE',
      createdAt: new Date().toISOString(),
    }
    maintenanceRecords = [...maintenanceRecords, newRecord]

    // Mirrors the backend's MaintenanceService.create() -> WorkshopScheduleService.create() call
    // (deployment-gated by WORKSHOP_AUTO_CREATE_SCHEDULE, default true): a scheduled maintenance
    // order always creates its own linked agenda entry. The mock has no concept of the toggle —
    // it always behaves as if the flag is on.
    const newSchedule = buildWorkshopScheduleMock({
      vehicle,
      technician,
      maintenanceRecordId: newRecord.id,
      maintenanceCategory: newRecord.category,
      scheduledDate: body.scheduledDate,
      type: newRecord.type,
      priority: 'MEDIUM',
    })
    workshopSchedules = [...workshopSchedules, newSchedule]

    return HttpResponse.json(newRecord, { status: 201 })
  }),

  http.put('/api/v1/maintenance/:id', async ({ request, params }) => {
    const body = (await request.json()) as MaintenanceUpdateRequestBody
    const index = maintenanceRecords.findIndex((record) => record.id === params.id)
    const existing = maintenanceRecords[index]

    if (!existing) {
      return HttpResponse.json(
        {
          status: 404,
          code: 'MAINTENANCE_NOT_FOUND',
          message: `Maintenance ${params.id} not found`,
          correlationId: 'test-correlation-id',
        },
        { status: 404 },
      )
    }

    const vehicle = vehicles.find((v) => v.id === body.vehicleId)
    if (!vehicle) {
      return HttpResponse.json(
        {
          status: 404,
          code: 'VEHICLE_NOT_FOUND',
          message: `Vehicle ${body.vehicleId} not found`,
          correlationId: 'test-correlation-id',
        },
        { status: 404 },
      )
    }

    let technician: Worker | undefined
    if (body.technicianId) {
      technician = workers.find((w) => w.id === body.technicianId)
      if (!technician) {
        return HttpResponse.json(
          {
            status: 404,
            code: 'WORKER_NOT_FOUND',
            message: `Worker ${body.technicianId} not found`,
            correlationId: 'test-correlation-id',
          },
          { status: 404 },
        )
      }
    }

    // The backend's update() has no status guard (pre-existing gap, out of scope here) — the
    // mock mirrors that by allowing the merge regardless of the record's current status.
    const updated: MaintenanceRecordMock = {
      ...existing,
      vehicleId: vehicle.id,
      vehicleLicensePlate: vehicle.licensePlate,
      vehicleMake: vehicle.make,
      vehicleModel: vehicle.model,
      type: body.type,
      description: body.description ?? null,
      technicianId: technician?.id ?? null,
      technicianName: technician?.fullName ?? null,
      category: body.category,
    }
    maintenanceRecords = maintenanceRecords.map((record, i) => (i === index ? updated : record))

    return HttpResponse.json(updated)
  }),

  http.patch('/api/v1/maintenance/:id/start', async ({ request, params }) => {
    const index = maintenanceRecords.findIndex((record) => record.id === params.id)
    const existing = maintenanceRecords[index]

    if (!existing) {
      return HttpResponse.json(
        {
          status: 404,
          code: 'MAINTENANCE_NOT_FOUND',
          message: `Maintenance ${params.id} not found`,
          correlationId: 'test-correlation-id',
        },
        { status: 404 },
      )
    }

    if (existing.status !== 'SCHEDULED') {
      return HttpResponse.json(
        {
          status: 409,
          code: 'MAINTENANCE_INVALID_STATE_TRANSITION',
          message: `Maintenance ${params.id} cannot be started from state ${existing.status}`,
          correlationId: 'test-correlation-id',
        },
        { status: 409 },
      )
    }

    const body = (await request.json().catch(() => null)) as { usageAtService?: number | null } | null

    const updated: MaintenanceRecordMock = {
      ...existing,
      status: 'IN_PROGRESS',
      workshopEntryDate: new Date().toISOString().slice(0, 10),
      usageAtService: body?.usageAtService ?? existing.usageAtService,
    }
    maintenanceRecords = maintenanceRecords.map((record, i) => (i === index ? updated : record))

    return HttpResponse.json(updated)
  }),

  http.patch('/api/v1/maintenance/:id/complete', async ({ request, params }) => {
    const index = maintenanceRecords.findIndex((record) => record.id === params.id)
    const existing = maintenanceRecords[index]

    if (!existing) {
      return HttpResponse.json(
        {
          status: 404,
          code: 'MAINTENANCE_NOT_FOUND',
          message: `Maintenance ${params.id} not found`,
          correlationId: 'test-correlation-id',
        },
        { status: 404 },
      )
    }

    if (existing.status !== 'IN_PROGRESS') {
      return HttpResponse.json(
        {
          status: 409,
          code: 'MAINTENANCE_INVALID_STATE_TRANSITION',
          message: `Maintenance ${params.id} cannot be completed from state ${existing.status}`,
          correlationId: 'test-correlation-id',
        },
        { status: 409 },
      )
    }

    const body = (await request.json().catch(() => null)) as { cost?: number | null } | null

    const updated: MaintenanceRecordMock = {
      ...existing,
      status: 'COMPLETED',
      workshopExitDate: new Date().toISOString().slice(0, 10),
      cost: body?.cost ?? existing.cost,
    }
    maintenanceRecords = maintenanceRecords.map((record, i) => (i === index ? updated : record))

    // Mirrors the backend's MaintenanceCompletedEvent -> ScheduleCompletionListener: a
    // WorkshopSchedule linked to this maintenance record transitions to COMPLETED too.
    // There is no manual "/complete" endpoint for schedules (Hito 25/26 decision) — this
    // is the only way a schedule ever reaches COMPLETED.
    workshopSchedules = workshopSchedules.map((schedule) =>
      schedule.maintenanceRecordId === updated.id ? { ...schedule, status: 'COMPLETED' } : schedule,
    )

    return HttpResponse.json(updated)
  }),

  http.patch('/api/v1/maintenance/:id/cancel', ({ params }) => {
    const index = maintenanceRecords.findIndex((record) => record.id === params.id)
    const existing = maintenanceRecords[index]

    if (!existing) {
      return HttpResponse.json(
        {
          status: 404,
          code: 'MAINTENANCE_NOT_FOUND',
          message: `Maintenance ${params.id} not found`,
          correlationId: 'test-correlation-id',
        },
        { status: 404 },
      )
    }

    if (existing.status !== 'SCHEDULED' && existing.status !== 'IN_PROGRESS') {
      return HttpResponse.json(
        {
          status: 409,
          code: 'MAINTENANCE_INVALID_STATE_TRANSITION',
          message: `Maintenance ${params.id} cannot be cancelled from state ${existing.status}`,
          correlationId: 'test-correlation-id',
        },
        { status: 409 },
      )
    }

    const updated: MaintenanceRecordMock = { ...existing, status: 'CANCELLED' }
    maintenanceRecords = maintenanceRecords.map((record, i) => (i === index ? updated : record))

    // Mirrors the schedule-cancel handler: a WorkshopSchedule linked to this maintenance
    // record is cancelled too, so the agenda stays consistent with the order.
    workshopSchedules = workshopSchedules.map((schedule) =>
      schedule.maintenanceRecordId === updated.id ? { ...schedule, status: 'CANCELLED' } : schedule,
    )

    return HttpResponse.json(updated)
  }),

  http.get('/api/v1/workshop/schedules', ({ request }) => {
    const url = new URL(request.url)
    const rangeParam = url.searchParams.get('range')

    if (!rangeParam) {
      return HttpResponse.json(
        {
          status: 400,
          code: 'INVALID_RANGE',
          message: 'range is required — must be one of: today, week, month',
          correlationId: 'test-correlation-id',
        },
        { status: 400 },
      )
    }

    const range = rangeParam.toLowerCase() as ScheduleRangeValue
    if (range !== 'today' && range !== 'week' && range !== 'month') {
      return HttpResponse.json(
        {
          status: 400,
          code: 'INVALID_RANGE',
          message: `range '${rangeParam}' is invalid — must be one of: today, week, month`,
          correlationId: 'test-correlation-id',
        },
        { status: 400 },
      )
    }

    const page = Number(url.searchParams.get('page') ?? 0)
    const size = Number(url.searchParams.get('size') ?? 20)

    const source = workshopSchedules.filter((schedule) => schedule.rangeTags.includes(range))
    const start = page * size
    const content = source.slice(start, start + size).map(({ rangeTags: _rangeTags, ...schedule }) => schedule)

    return HttpResponse.json({
      content,
      page,
      size,
      totalElements: source.length,
      totalPages: Math.max(1, Math.ceil(source.length / size)),
    })
  }),

  http.post('/api/v1/workshop/schedules', async ({ request }) => {
    const body = (await request.json()) as ScheduleRequestBody

    const vehicle = vehicles.find((v) => v.id === body.vehicleId)
    if (!vehicle) {
      return HttpResponse.json(
        {
          status: 404,
          code: 'VEHICLE_NOT_FOUND',
          message: `Vehicle ${body.vehicleId} not found`,
          correlationId: 'test-correlation-id',
        },
        { status: 404 },
      )
    }

    let technician: Worker | undefined
    if (body.technicianId) {
      technician = workers.find((w) => w.id === body.technicianId)
      if (!technician) {
        return HttpResponse.json(
          {
            status: 404,
            code: 'WORKER_NOT_FOUND',
            message: `Worker ${body.technicianId} not found`,
            correlationId: 'test-correlation-id',
          },
          { status: 404 },
        )
      }
    }

    const linkedMaintenance = body.maintenanceRecordId
      ? maintenanceRecords.find((record) => record.id === body.maintenanceRecordId)
      : undefined

    const newSchedule = buildWorkshopScheduleMock({
      vehicle,
      technician,
      maintenanceRecordId: linkedMaintenance?.id,
      maintenanceCategory: linkedMaintenance?.category,
      scheduledDate: body.scheduledDate,
      type: body.type,
      priority: body.priority ?? 'MEDIUM',
      notes: body.notes,
    })
    workshopSchedules = [...workshopSchedules, newSchedule]

    const { rangeTags: _rangeTags, ...response } = newSchedule
    return HttpResponse.json(response, { status: 201 })
  }),

  http.put('/api/v1/workshop/schedules/:id', async ({ request, params }) => {
    const body = (await request.json()) as ScheduleUpdateRequestBody
    const index = workshopSchedules.findIndex((schedule) => schedule.id === params.id)
    const existing = workshopSchedules[index]

    if (!existing) {
      return HttpResponse.json(
        {
          status: 404,
          code: 'SCHEDULE_NOT_FOUND',
          message: `Schedule ${params.id} not found`,
          correlationId: 'test-correlation-id',
        },
        { status: 404 },
      )
    }

    const vehicle = vehicles.find((v) => v.id === body.vehicleId)
    if (!vehicle) {
      return HttpResponse.json(
        {
          status: 404,
          code: 'VEHICLE_NOT_FOUND',
          message: `Vehicle ${body.vehicleId} not found`,
          correlationId: 'test-correlation-id',
        },
        { status: 404 },
      )
    }

    let technician: Worker | undefined
    if (body.technicianId) {
      technician = workers.find((w) => w.id === body.technicianId)
      if (!technician) {
        return HttpResponse.json(
          {
            status: 404,
            code: 'WORKER_NOT_FOUND',
            message: `Worker ${body.technicianId} not found`,
            correlationId: 'test-correlation-id',
          },
          { status: 404 },
        )
      }
    }

    const linkedMaintenance = body.maintenanceRecordId
      ? maintenanceRecords.find((record) => record.id === body.maintenanceRecordId)
      : undefined

    // Same rationale as the maintenance PUT handler: the backend's update() has no status
    // guard (pre-existing gap, out of scope here), so the mock allows the merge regardless of
    // the schedule's current status.
    const updated: WorkshopScheduleMock = {
      ...existing,
      vehicleId: vehicle.id,
      vehicleLicensePlate: vehicle.licensePlate,
      vehicleMake: vehicle.make,
      vehicleModel: vehicle.model,
      technicianId: technician?.id ?? null,
      technicianName: technician?.fullName ?? null,
      maintenanceRecordId: linkedMaintenance?.id ?? null,
      maintenanceCategory: linkedMaintenance?.category ?? null,
      scheduledDate: body.scheduledDate,
      type: body.type,
      priority: body.priority,
      notes: body.notes ?? null,
    }
    workshopSchedules = workshopSchedules.map((schedule, i) => (i === index ? updated : schedule))

    const { rangeTags: _rangeTags, ...response } = updated
    return HttpResponse.json(response)
  }),

  http.patch('/api/v1/workshop/schedules/:id/start', ({ params }) => {
    const index = workshopSchedules.findIndex((schedule) => schedule.id === params.id)
    const existing = workshopSchedules[index]

    if (!existing) {
      return HttpResponse.json(
        {
          status: 404,
          code: 'SCHEDULE_NOT_FOUND',
          message: `Schedule ${params.id} not found`,
          correlationId: 'test-correlation-id',
        },
        { status: 404 },
      )
    }

    if (existing.status !== 'PENDING') {
      return HttpResponse.json(
        {
          status: 409,
          code: 'SCHEDULE_INVALID_STATE_TRANSITION',
          message: `Schedule ${params.id} cannot be started from state ${existing.status}`,
          correlationId: 'test-correlation-id',
        },
        { status: 409 },
      )
    }

    const updated: WorkshopScheduleMock = { ...existing, status: 'IN_PROGRESS' }
    workshopSchedules = workshopSchedules.map((schedule, i) => (i === index ? updated : schedule))

    const { rangeTags: _rangeTags, ...response } = updated
    return HttpResponse.json(response)
  }),

  http.patch('/api/v1/workshop/schedules/:id/cancel', ({ params }) => {
    const index = workshopSchedules.findIndex((schedule) => schedule.id === params.id)
    const existing = workshopSchedules[index]

    if (!existing) {
      return HttpResponse.json(
        {
          status: 404,
          code: 'SCHEDULE_NOT_FOUND',
          message: `Schedule ${params.id} not found`,
          correlationId: 'test-correlation-id',
        },
        { status: 404 },
      )
    }

    if (existing.status !== 'PENDING' && existing.status !== 'IN_PROGRESS') {
      return HttpResponse.json(
        {
          status: 409,
          code: 'SCHEDULE_INVALID_STATE_TRANSITION',
          message: `Schedule ${params.id} cannot be cancelled from state ${existing.status}`,
          correlationId: 'test-correlation-id',
        },
        { status: 409 },
      )
    }

    const updated: WorkshopScheduleMock = { ...existing, status: 'CANCELLED' }
    workshopSchedules = workshopSchedules.map((schedule, i) => (i === index ? updated : schedule))

    // Mirrors the maintenance-cancel handler in the opposite direction: cancelling a schedule
    // cascades to its linked MaintenanceRecord too (backend: MaintenanceCancelledEvent's
    // ScheduleCancellationListener runs the same guard symmetrically), unless that record is
    // already in a terminal state (COMPLETED/CANCELLED).
    maintenanceRecords = maintenanceRecords.map((record) =>
      record.id === updated.maintenanceRecordId && record.status !== 'COMPLETED' && record.status !== 'CANCELLED'
        ? { ...record, status: 'CANCELLED' }
        : record,
    )

    const { rangeTags: _rangeTags, ...response } = updated
    return HttpResponse.json(response)
  }),

  http.post('/api/v1/auth/login', async ({ request }) => {
    const body = (await request.json()) as LoginRequestBody

    if (body.email !== VALID_CREDENTIALS.email || body.password !== VALID_CREDENTIALS.password) {
      return invalidCredentialsResponse()
    }

    return HttpResponse.json({
      accessToken: MOCK_ACCESS_TOKEN,
      refreshToken: MOCK_REFRESH_TOKEN,
      role: MOCK_ROLE,
    })
  }),

  http.post('/api/v1/auth/refresh', async ({ request }) => {
    const body = (await request.json()) as RefreshRequestBody

    if (body.refreshToken !== MOCK_REFRESH_TOKEN) {
      return invalidCredentialsResponse()
    }

    return HttpResponse.json({
      accessToken: `${MOCK_ACCESS_TOKEN}-refreshed`,
      refreshToken: MOCK_REFRESH_TOKEN,
      role: MOCK_ROLE,
    })
  }),
]
