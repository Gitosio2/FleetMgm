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
    startDate: '2026-01-15',
    endDate: null,
    assignedByUserId: ASSIGNED_BY_USER_ID,
    notes: null,
    createdAt: '2026-01-15T09:00:00Z',
    active: true,
  },
]

let assignments: Assignment[] = [...SEED_ASSIGNMENTS]

export function resetAssignmentsMock() {
  assignments = [...SEED_ASSIGNMENTS]
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
