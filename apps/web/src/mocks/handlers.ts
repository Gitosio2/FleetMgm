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

type Supplier = {
  id: string
  name: string
  taxId: string | null
  email: string | null
  phone: string | null
  address: string | null
  createdAt: string
}

type SupplierRequestBody = {
  name: string
  taxId?: string | null
  email?: string | null
  phone?: string | null
  address?: string | null
}

export const SEED_SUPPLIERS: Supplier[] = [
  {
    id: 'supplier-1',
    name: 'Taller Mecánico Norte',
    taxId: 'B11122233',
    email: 'contacto@tallernorte.test',
    phone: '+34600100200',
    address: 'Polígono Norte 10',
    createdAt: '2026-01-10T09:00:00Z',
  },
  {
    id: 'supplier-2',
    name: 'Estación de Servicio Sur',
    taxId: null,
    email: null,
    phone: null,
    address: null,
    createdAt: '2026-02-15T09:00:00Z',
  },
  {
    id: 'supplier-3',
    name: 'Aseguradora Segurcar',
    taxId: 'B44455566',
    email: 'polizas@segurcar.test',
    phone: '+34600300400',
    address: 'Av. del Seguro 5',
    createdAt: '2026-03-01T09:00:00Z',
  },
  // Unused by any SEED_SUPPLIER_INVOICES row — kept available for tests that create a new
  // supplier invoice, so the new row's supplier name doesn't collide with an existing one.
  {
    id: 'supplier-4',
    name: 'Ferretería Central',
    taxId: null,
    email: null,
    phone: null,
    address: null,
    createdAt: '2026-03-05T09:00:00Z',
  },
  {
    id: 'supplier-5',
    name: 'Taller Rápido',
    taxId: null,
    email: null,
    phone: null,
    address: null,
    createdAt: '2026-03-06T09:00:00Z',
  },
  {
    id: 'supplier-6',
    name: 'Estación de Servicio Central',
    taxId: null,
    email: null,
    phone: null,
    address: null,
    createdAt: '2026-03-07T09:00:00Z',
  },
]

let suppliers: Supplier[] = [...SEED_SUPPLIERS]

export function resetSuppliersMock() {
  suppliers = [...SEED_SUPPLIERS]
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

type GpsSourceMock = 'MOCK' | 'DEVICE'

type GpsPositionMock = {
  id: string
  vehicleId: string
  licensePlate: string | null
  vehicleMake: string | null
  vehicleModel: string | null
  vehicleCategory: VehicleCategory
  latitude: number
  longitude: number
  heading: number | null
  speed: number | null
  recordedAt: string
  source: GpsSourceMock
}

export const SEED_GPS_POSITIONS: GpsPositionMock[] = [
  {
    id: 'gps-1',
    vehicleId: SEED_VEHICLES[0]!.id,
    licensePlate: SEED_VEHICLES[0]!.licensePlate,
    vehicleMake: SEED_VEHICLES[0]!.make,
    vehicleModel: SEED_VEHICLES[0]!.model,
    vehicleCategory: SEED_VEHICLES[0]!.vehicleCategory,
    latitude: 40.4168,
    longitude: -3.7038,
    heading: 90,
    speed: 45.7,
    recordedAt: '2026-07-12T10:00:00Z',
    source: 'MOCK',
  },
  {
    id: 'gps-2',
    vehicleId: SEED_VEHICLES[1]!.id,
    licensePlate: SEED_VEHICLES[1]!.licensePlate,
    vehicleMake: SEED_VEHICLES[1]!.make,
    vehicleModel: SEED_VEHICLES[1]!.model,
    vehicleCategory: SEED_VEHICLES[1]!.vehicleCategory,
    latitude: 40.42,
    longitude: -3.71,
    heading: 180,
    speed: 0,
    recordedAt: '2026-07-12T10:00:00Z',
    source: 'MOCK',
  },
]

let gpsPositions: GpsPositionMock[] = [...SEED_GPS_POSITIONS]

export function resetGpsMock() {
  gpsPositions = [...SEED_GPS_POSITIONS]
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
  actualStart?: string | null
  actualEnd?: string | null
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
  {
    id: 'job-5',
    title: 'Traslado programado',
    description: null,
    vehicleId: SEED_VEHICLES[0]!.id,
    vehicleLicensePlate: SEED_VEHICLES[0]!.licensePlate,
    vehicleMake: SEED_VEHICLES[0]!.make,
    vehicleModel: SEED_VEHICLES[0]!.model,
    assignedDriverId: null,
    assignedDriverName: null,
    clientId: null,
    clientName: null,
    status: 'PENDING',
    originLocation: 'Almacén Central',
    destinationLocation: 'Depósito Este',
    notes: null,
    scheduledStart: null,
    scheduledEnd: null,
    actualStart: '2026-07-08T09:00:00Z',
    actualEnd: null,
    startUsageValue: null,
    endUsageValue: null,
    createdAt: '2026-07-03T09:00:00Z',
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
  workshopEntryTime: string | null
  workshopExitTime: string | null
  technicianId: string | null
  technicianName: string | null
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
    workshopEntryTime: null,
    workshopExitTime: null,
    technicianId: TECHNICIAN_WORKER_ID,
    technicianName: SEED_WORKERS[1]!.fullName,
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
    workshopEntryTime: '08:30:00',
    workshopExitTime: null,
    technicianId: TECHNICIAN_WORKER_ID,
    technicianName: SEED_WORKERS[1]!.fullName,
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
    workshopEntryTime: '09:00:00',
    workshopExitTime: '17:00:00',
    technicianId: null,
    technicianName: null,
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
  scheduledStartTime: string | null
  scheduledEndTime: string | null
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
  scheduledStartTime?: string | null
  scheduledEndTime?: string | null
  type: string
  priority?: SchedulePriority | null
  notes?: string | null
}

type ScheduleUpdateRequestBody = {
  vehicleId: string
  technicianId?: string | null
  maintenanceRecordId?: string | null
  scheduledDate: string
  scheduledStartTime?: string | null
  scheduledEndTime?: string | null
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
    scheduledStartTime: '08:00:00',
    scheduledEndTime: '09:00:00',
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
    scheduledStartTime: null,
    scheduledEndTime: null,
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
    scheduledStartTime: null,
    scheduledEndTime: null,
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
  scheduledStartTime?: string | null
  scheduledEndTime?: string | null
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
    scheduledStartTime: params.scheduledStartTime ?? null,
    scheduledEndTime: params.scheduledEndTime ?? null,
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

type InvoiceStatus = 'DRAFT' | 'ISSUED' | 'PAID' | 'OVERDUE'

type LineItemMock = {
  id: string
  description: string
  quantity: number
  unitPrice: number
  subtotal: number
  linkedJobId: string | null
}

type LineItemRequestBody = {
  description: string
  quantity: number
  unitPrice: number
  linkedJobId?: string | null
}

type InvoiceMock = {
  id: string
  invoiceNumber: string
  clientId: string
  clientName: string
  status: InvoiceStatus
  issueDate: string | null
  dueDate: string | null
  paymentDate: string | null
  taxRate: number
  subtotal: number
  taxAmount: number
  total: number
  notes: string | null
  createdAt: string
  lineItems: LineItemMock[]
}

type InvoiceRequestBody = {
  clientId: string
  dueDate?: string | null
  notes?: string | null
  taxRate?: number | null
}

type PayInvoiceRequestBody = {
  paymentDate?: string | null
}

// Mirrors the backend's configurable default tax rate (see InvoiceFormModal's "usa el valor por
// defecto" label) — the mock always falls back to this when taxRate isn't provided.
const DEFAULT_TAX_RATE = 0.21

function buildLineItemMock(id: string, request: LineItemRequestBody): LineItemMock {
  return {
    id,
    description: request.description,
    quantity: request.quantity,
    unitPrice: request.unitPrice,
    subtotal: request.quantity * request.unitPrice,
    linkedJobId: request.linkedJobId ?? null,
  }
}

// Builds a small but *structurally valid* PDF (correct xref offsets, trailer, %%EOF) instead of a
// string that merely starts with "%PDF" — a real PDF viewer (Chrome, Acrobat) rejects the latter
// outright with "error loading document", which is indistinguishable from a real corruption bug
// when manually testing the download flow against this mock (as opposed to the real backend) in
// local dev. Content is plain ASCII throughout, so JS string .length is a valid byte offset.
function buildMinimalPdf(bodyText: string): string {
  const objects = [
    '1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n',
    '2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n',
    '3 0 obj\n<< /Type /Page /Parent 2 0 R /Resources << /Font << /F1 5 0 R >> >> ' +
      '/MediaBox [0 0 300 150] /Contents 4 0 R >>\nendobj\n',
    (() => {
      const stream = `BT /F1 14 Tf 20 100 Td (${bodyText}) Tj ET`
      return `4 0 obj\n<< /Length ${stream.length} >>\nstream\n${stream}\nendstream\nendobj\n`
    })(),
    '5 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>\nendobj\n',
  ]

  let body = '%PDF-1.4\n'
  const offsets: number[] = [0]
  for (const object of objects) {
    offsets.push(body.length)
    body += object
  }

  const xrefStart = body.length
  let xref = `xref\n0 ${objects.length + 1}\n0000000000 65535 f \n`
  for (let i = 1; i <= objects.length; i++) {
    xref += `${String(offsets[i]).padStart(10, '0')} 00000 n \n`
  }
  const trailer = `trailer\n<< /Size ${objects.length + 1} /Root 1 0 R >>\nstartxref\n${xrefStart}\n%%EOF`

  return body + xref + trailer
}

function recalcInvoiceTotals(invoice: InvoiceMock): InvoiceMock {
  const subtotal = invoice.lineItems.reduce((sum, lineItem) => sum + lineItem.subtotal, 0)
  const taxAmount = subtotal * invoice.taxRate
  return { ...invoice, subtotal, taxAmount, total: subtotal + taxAmount }
}

export const SEED_INVOICES: InvoiceMock[] = [
  recalcInvoiceTotals({
    id: 'invoice-1',
    invoiceNumber: 'INV-2026-00001',
    clientId: SEED_CLIENTS[0]!.id,
    clientName: SEED_CLIENTS[0]!.name,
    status: 'DRAFT',
    issueDate: null,
    dueDate: '2026-08-01',
    paymentDate: null,
    taxRate: DEFAULT_TAX_RATE,
    subtotal: 0,
    taxAmount: 0,
    total: 0,
    notes: null,
    createdAt: '2026-07-05T09:00:00Z',
    lineItems: [
      buildLineItemMock('line-item-1', { description: 'Transporte de mercancía', quantity: 2, unitPrice: 150 }),
      buildLineItemMock('line-item-2', { description: 'Combustible', quantity: 1, unitPrice: 80 }),
    ],
  }),
  recalcInvoiceTotals({
    id: 'invoice-2',
    invoiceNumber: 'INV-2026-00002',
    clientId: SEED_CLIENTS[1]!.id,
    clientName: SEED_CLIENTS[1]!.name,
    status: 'ISSUED',
    issueDate: '2026-07-06',
    dueDate: '2026-08-06',
    paymentDate: null,
    taxRate: DEFAULT_TAX_RATE,
    subtotal: 0,
    taxAmount: 0,
    total: 0,
    notes: null,
    createdAt: '2026-07-06T09:00:00Z',
    lineItems: [
      buildLineItemMock('line-item-3', { description: 'Servicio de flete', quantity: 1, unitPrice: 500 }),
    ],
  }),
  recalcInvoiceTotals({
    id: 'invoice-3',
    invoiceNumber: 'INV-2026-00003',
    clientId: SEED_CLIENTS[0]!.id,
    clientName: SEED_CLIENTS[0]!.name,
    status: 'PAID',
    issueDate: '2026-06-01',
    dueDate: '2026-07-01',
    paymentDate: '2026-06-28',
    taxRate: DEFAULT_TAX_RATE,
    subtotal: 0,
    taxAmount: 0,
    total: 0,
    notes: 'Pagada por transferencia',
    createdAt: '2026-06-01T09:00:00Z',
    lineItems: [
      buildLineItemMock('line-item-4', { description: 'Transporte de mercancía', quantity: 3, unitPrice: 200 }),
    ],
  }),
]

let invoices: InvoiceMock[] = [...SEED_INVOICES]

export function resetInvoicesMock() {
  invoices = [...SEED_INVOICES]
}

let lineItemSequence = SEED_INVOICES.reduce((count, invoice) => count + invoice.lineItems.length, 0)

type ExpenseCategory = 'MAINTENANCE' | 'FUEL' | 'INSURANCE' | 'LEASING_RENTING' | 'TOLL' | 'OTHER'
type SupplierInvoiceStatus = 'PENDING' | 'PAID'

type SupplierLineItemMock = {
  id: string
  description: string
  quantity: number
  unitPrice: number
  subtotal: number
  vehicleId: string | null
  maintenanceRecordId: string | null
}

type SupplierLineItemRequestBody = {
  description: string
  quantity: number
  subtotal: number
  vehicleId?: string | null
  maintenanceRecordId?: string | null
}

type SupplierInvoiceMock = {
  id: string
  supplierId: string
  supplierName: string
  supplierInvoiceNumber: string | null
  category: ExpenseCategory
  invoiceDate: string
  dueDate: string | null
  paymentDate: string | null
  status: SupplierInvoiceStatus
  subtotal: number
  taxAmount: number
  total: number
  vehicleId: string | null
  vehicleLicensePlate: string | null
  vehicleMake: string | null
  vehicleModel: string | null
  notes: string | null
  documentPath: string | null
  createdAt: string
  lineItems: SupplierLineItemMock[]
}

type SupplierInvoiceRequestBody = {
  supplierId: string
  supplierInvoiceNumber?: string | null
  category: ExpenseCategory
  invoiceDate: string
  dueDate?: string | null
  vehicleId?: string | null
  subtotal: number
  taxAmount: number
  total: number
  notes?: string | null
  documentPath?: string | null
}

function buildSupplierLineItemMock(id: string, request: SupplierLineItemRequestBody): SupplierLineItemMock {
  return {
    id,
    description: request.description,
    quantity: request.quantity,
    // unitPrice is derived (average price), mirroring the backend: subtotal / quantity, rounded
    // to 2 decimals — subtotal itself is the user-entered total cost, mapped straight through.
    unitPrice: Math.round((request.subtotal / request.quantity) * 100) / 100,
    subtotal: request.subtotal,
    vehicleId: request.vehicleId ?? null,
    maintenanceRecordId: request.maintenanceRecordId ?? null,
  }
}

export const SEED_SUPPLIER_INVOICES: SupplierInvoiceMock[] = [
  {
    id: 'supplier-invoice-1',
    supplierId: SEED_SUPPLIERS[0]!.id,
    supplierName: SEED_SUPPLIERS[0]!.name,
    supplierInvoiceNumber: 'F-2026-0456',
    category: 'MAINTENANCE',
    invoiceDate: '2026-07-01',
    dueDate: '2026-07-31',
    paymentDate: null,
    status: 'PENDING',
    subtotal: 100,
    taxAmount: 21,
    total: 121,
    vehicleId: SEED_VEHICLES[0]!.id,
    vehicleLicensePlate: SEED_VEHICLES[0]!.licensePlate,
    vehicleMake: SEED_VEHICLES[0]!.make,
    vehicleModel: SEED_VEHICLES[0]!.model,
    notes: null,
    documentPath: null,
    createdAt: '2026-07-01T09:00:00Z',
    lineItems: [],
  },
  {
    id: 'supplier-invoice-2',
    supplierId: SEED_SUPPLIERS[1]!.id,
    supplierName: SEED_SUPPLIERS[1]!.name,
    supplierInvoiceNumber: null,
    category: 'FUEL',
    invoiceDate: '2026-07-05',
    dueDate: '2026-08-05',
    paymentDate: null,
    status: 'PENDING',
    subtotal: 50,
    taxAmount: 10.5,
    total: 60.5,
    vehicleId: null,
    vehicleLicensePlate: null,
    vehicleMake: null,
    vehicleModel: null,
    notes: null,
    documentPath: null,
    createdAt: '2026-07-05T09:00:00Z',
    lineItems: [],
  },
  {
    id: 'supplier-invoice-3',
    supplierId: SEED_SUPPLIERS[2]!.id,
    supplierName: SEED_SUPPLIERS[2]!.name,
    supplierInvoiceNumber: 'POL-2026-778',
    category: 'INSURANCE',
    invoiceDate: '2026-06-01',
    dueDate: '2026-06-30',
    paymentDate: '2026-06-25',
    status: 'PAID',
    subtotal: 200,
    taxAmount: 42,
    total: 242,
    vehicleId: SEED_VEHICLES[1]!.id,
    vehicleLicensePlate: SEED_VEHICLES[1]!.licensePlate,
    vehicleMake: SEED_VEHICLES[1]!.make,
    vehicleModel: SEED_VEHICLES[1]!.model,
    notes: 'Pagada por transferencia',
    documentPath: null,
    createdAt: '2026-06-01T09:00:00Z',
    lineItems: [],
  },
  // Already-split shared fuel invoice — no header vehicle, costs attributed per vehicle via
  // line items. Exercises the "already split" rendering path (allocation indicator, disabled
  // header vehicle select) without requiring a test to add a line item first.
  {
    id: 'supplier-invoice-4',
    supplierId: SEED_SUPPLIERS[5]!.id,
    supplierName: SEED_SUPPLIERS[5]!.name,
    supplierInvoiceNumber: 'F-2026-0999',
    category: 'FUEL',
    invoiceDate: '2026-07-08',
    dueDate: '2026-08-08',
    paymentDate: null,
    status: 'PENDING',
    subtotal: 90,
    taxAmount: 18.9,
    total: 108.9,
    vehicleId: null,
    vehicleLicensePlate: null,
    vehicleMake: null,
    vehicleModel: null,
    notes: 'Repostaje compartido — dividido por vehículo',
    documentPath: null,
    createdAt: '2026-07-08T09:00:00Z',
    lineItems: [
      buildSupplierLineItemMock('supplier-line-item-1', {
        description: 'Gasoil - Toyota Hilux',
        quantity: 40,
        subtotal: 60,
        vehicleId: SEED_VEHICLES[0]!.id,
      }),
      buildSupplierLineItemMock('supplier-line-item-2', {
        description: 'Gasoil - Excavadora CAT 320',
        quantity: 20,
        subtotal: 30,
        vehicleId: SEED_VEHICLES[1]!.id,
      }),
    ],
  },
]

let supplierInvoices: SupplierInvoiceMock[] = [...SEED_SUPPLIER_INVOICES]
let supplierLineItemSequence = SEED_SUPPLIER_INVOICES.reduce(
  (count, invoice) => count + invoice.lineItems.length,
  0,
)

export function resetSupplierInvoicesMock() {
  supplierInvoices = [...SEED_SUPPLIER_INVOICES]
}

type AuditActionMock = 'CREATE' | 'UPDATE' | 'DELETE' | 'LOGIN' | 'LOGOUT' | 'ACCESS_DENIED' | 'ACCOUNT_LOCKED'

type AuditLogMock = {
  id: string
  entityType: string
  entityId: string
  action: AuditActionMock
  performedByUserId: string | null
  performedByEmail: string | null
  performedAt: string
  ipAddress: string | null
  oldValues: string | null
  newValues: string | null
  details: string | null
}

type FleetSummaryMock = {
  activeVehicles: number
  totalVehicles: number
  inWorkshop: number
  pendingMaintenance: number
  pendingMaintenanceDueSoon: number
}

// Mirrors the actual SEED_VEHICLES/SEED_SCHEDULES counts instead of the Stitch design mockup's
// enterprise-scale placeholder numbers: 2 vehicles (1 ACTIVE, 1 MAINTENANCE), 2 PENDING schedules
// out of 3 (schedule-1, schedule-3 — schedule-2 is IN_PROGRESS). pendingMaintenanceDueSoon stays
// illustrative (not literally derived from scheduledDate) — neither PENDING schedule's date
// actually falls within 48h of "today" in this fixed seed, and shifting a shared schedule date to
// force it would risk the Workshop feature's own tests that depend on those exact dates/rangeTags.
export const SEED_FLEET_SUMMARY: FleetSummaryMock = {
  activeVehicles: 1,
  totalVehicles: 2,
  inWorkshop: 1,
  pendingMaintenance: 2,
  pendingMaintenanceDueSoon: 1,
}

type UpcomingInvoiceMock = {
  id: string
  number: string
  counterpartyId: string
  counterparty: string
  amount: number
  dueDate: string
  overdue: boolean
}

type FinancialSummaryMock = {
  monthlyCosts: number
  upcomingReceivables: UpcomingInvoiceMock[]
  upcomingPayables: UpcomingInvoiceMock[]
}

// Read-only feature (no dedicated CRUD screen) — mirrors SEED_FLEET_SUMMARY's scale, plus two
// short invoice lists each containing one overdue and one not-yet-due row so Dashboard.test.tsx
// can assert both the "Vencida" marker and its absence without depending on the current date.
export const SEED_FINANCIAL_SUMMARY: FinancialSummaryMock = {
  monthlyCosts: 8420.5,
  upcomingReceivables: [
    {
      id: 'invoice-2',
      number: 'INV-2026-00002',
      counterpartyId: 'client-2',
      counterparty: 'Transportes Ibérica',
      amount: 605,
      dueDate: '2026-07-10',
      overdue: true,
    },
    {
      id: 'invoice-5',
      number: 'INV-2026-00005',
      counterpartyId: 'client-1',
      counterparty: 'Acme Logistics',
      amount: 350,
      dueDate: '2026-07-18',
      overdue: false,
    },
  ],
  upcomingPayables: [
    {
      id: 'supplier-invoice-1',
      number: 'F-2026-0456',
      counterpartyId: 'supplier-1',
      counterparty: 'Taller Mecánico Norte',
      amount: 121,
      dueDate: '2026-07-05',
      overdue: true,
    },
    {
      id: 'supplier-invoice-2',
      number: 'F-2026-0501',
      counterpartyId: 'supplier-2',
      counterparty: 'Estación de Servicio Sur',
      amount: 60.5,
      dueDate: '2026-07-20',
      overdue: false,
    },
  ],
}

type ProfitabilityMock = {
  vehicleId: string
  vehicleLicensePlate: string | null
  vehicleMake: string
  vehicleModel: string
  revenue: number
  costs: number
  margin: number
}

// Read-only feature (no create/update/delete) — mirrors SEED_VEHICLES 1:1 (one profitability row
// per seed vehicle) so Dashboard.test.tsx can assert chart/summary totals derived from real seed
// identities instead of unrelated placeholder numbers.
export const SEED_PROFITABILITY: ProfitabilityMock[] = [
  {
    vehicleId: SEED_VEHICLES[0]!.id,
    vehicleLicensePlate: SEED_VEHICLES[0]!.licensePlate,
    vehicleMake: SEED_VEHICLES[0]!.make,
    vehicleModel: SEED_VEHICLES[0]!.model,
    revenue: 12500,
    costs: 4200,
    margin: 8300,
  },
  {
    vehicleId: SEED_VEHICLES[1]!.id,
    vehicleLicensePlate: SEED_VEHICLES[1]!.licensePlate,
    vehicleMake: SEED_VEHICLES[1]!.make,
    vehicleModel: SEED_VEHICLES[1]!.model,
    revenue: 6200,
    costs: 5100,
    margin: 1100,
  },
]

// vehicleId is mock-internal (used to filter by :vehicleId) — stripped before the handler
// returns a response, since the real VehicleRevenueLineItem type has no vehicleId field
// (already scoped by the URL path param).
type VehicleRevenueLineItemMock = {
  vehicleId: string
  invoiceNumber: string
  issueDate: string
  description: string
  quantity: number
  unitPrice: number
  subtotal: number
}

export const SEED_VEHICLE_REVENUE: VehicleRevenueLineItemMock[] = [
  {
    vehicleId: SEED_VEHICLES[0]!.id,
    invoiceNumber: 'INV-2026-00001',
    issueDate: '2026-07-05',
    description: 'Transporte de mercancía',
    quantity: 1,
    unitPrice: 500,
    subtotal: 500,
  },
  {
    vehicleId: SEED_VEHICLES[0]!.id,
    invoiceNumber: 'INV-2026-00002',
    issueDate: '2026-06-15',
    description: 'Transporte de mercancía',
    quantity: 1,
    unitPrice: 300,
    subtotal: 300,
  },
]

type MonthlyFinancialMock = {
  month: string
  revenue: number
  costs: number
}

// Fixed 12-month fixture (2025-08 through 2026-07, "now" per this project's fixed dev clock) so
// Dashboard.test.tsx can assert deterministic totals for the 3/6/12-month selector without any
// dependency on the actual current date. Magnitudes mirror SEED_FINANCIAL_SUMMARY.monthlyCosts
// (~8420.5/month) rather than arbitrary enterprise-scale placeholders.
export const SEED_FINANCIAL_TREND: MonthlyFinancialMock[] = [
  { month: '2025-08', revenue: 11200, costs: 6100 },
  { month: '2025-09', revenue: 9800, costs: 5200 },
  { month: '2025-10', revenue: 13400, costs: 7300 },
  { month: '2025-11', revenue: 10500, costs: 6800 },
  { month: '2025-12', revenue: 15200, costs: 8420.5 },
  { month: '2026-01', revenue: 9200, costs: 5100 },
  { month: '2026-02', revenue: 12100, costs: 6400 },
  { month: '2026-03', revenue: 14300, costs: 7900 },
  { month: '2026-04', revenue: 10800, costs: 6000 },
  { month: '2026-05', revenue: 13900, costs: 7200 },
  { month: '2026-06', revenue: 11600, costs: 6500 },
  { month: '2026-07', revenue: 12500, costs: 6900 },
]

// Read-only feature (no create/update/delete) — a plain const seed is enough, no mutable copy or
// reset function needed like the other mocks above.
export const SEED_AUDIT_LOGS: AuditLogMock[] = [
  {
    id: 'audit-1',
    entityType: 'Invoice',
    entityId: 'invoice-1',
    action: 'CREATE',
    performedByUserId: 'user-1',
    performedByEmail: 'admin@fleetmgm.com',
    performedAt: '2026-07-10T09:15:00Z',
    ipAddress: '127.0.0.1',
    oldValues: null,
    newValues: '{"status":"DRAFT"}',
    details: null,
  },
  {
    id: 'audit-2',
    entityType: 'SupplierInvoice',
    entityId: 'supplier-invoice-1',
    action: 'UPDATE',
    performedByUserId: 'user-2',
    performedByEmail: 'manager@fleetmgm.com',
    performedAt: '2026-07-11T14:30:00Z',
    ipAddress: '127.0.0.1',
    oldValues: '{"status":"PENDING"}',
    newValues: '{"status":"PAID"}',
    details: null,
  },
  {
    id: 'audit-3',
    entityType: 'User',
    entityId: 'user-3',
    action: 'LOGIN',
    performedByUserId: 'user-3',
    performedByEmail: 'driver@fleetmgm.com',
    performedAt: '2026-07-12T08:00:00Z',
    ipAddress: '203.0.113.10',
    oldValues: null,
    newValues: null,
    details: null,
  },
  {
    id: 'audit-4',
    entityType: 'User',
    entityId: 'unknown',
    action: 'ACCESS_DENIED',
    performedByUserId: null,
    performedByEmail: 'driver@fleetmgm.com',
    performedAt: '2026-07-12T08:05:00Z',
    ipAddress: '203.0.113.10',
    oldValues: null,
    newValues: null,
    details: 'Attempted to access /api/v1/audit',
  },
]

export const handlers = [
  http.get('/api/v1/gps/latest', ({ request }) => {
    const url = new URL(request.url)
    const category = url.searchParams.get('category')
    const vehicleId = url.searchParams.get('vehicleId')
    const filtered = gpsPositions.filter(
      (position) =>
        (!category || position.vehicleCategory === category) && (!vehicleId || position.vehicleId === vehicleId),
    )

    return HttpResponse.json(filtered)
  }),

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

  http.get('/api/v1/clients/:id', ({ params }) => {
    const client = clients.find((c) => c.id === params.id)

    if (!client) {
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

    return HttpResponse.json(client)
  }),

  http.get('/api/v1/suppliers', ({ request }) => {
    const url = new URL(request.url)
    const page = Number(url.searchParams.get('page') ?? 0)
    const size = Number(url.searchParams.get('size') ?? 20)
    const start = page * size
    const content = suppliers.slice(start, start + size)

    return HttpResponse.json({
      content,
      page,
      size,
      totalElements: suppliers.length,
      totalPages: Math.max(1, Math.ceil(suppliers.length / size)),
    })
  }),

  http.post('/api/v1/suppliers', async ({ request }) => {
    const body = (await request.json()) as SupplierRequestBody

    const newSupplier: Supplier = {
      id: `supplier-${suppliers.length + 1}`,
      name: body.name,
      taxId: body.taxId ?? null,
      email: body.email ?? null,
      phone: body.phone ?? null,
      address: body.address ?? null,
      createdAt: new Date().toISOString(),
    }
    suppliers = [...suppliers, newSupplier]

    return HttpResponse.json(newSupplier, { status: 201 })
  }),

  http.get('/api/v1/suppliers/:id', ({ params }) => {
    const supplier = suppliers.find((s) => s.id === params.id)

    if (!supplier) {
      return HttpResponse.json(
        {
          status: 404,
          code: 'SUPPLIER_NOT_FOUND',
          message: `Supplier ${params.id} not found`,
          correlationId: 'test-correlation-id',
        },
        { status: 404 },
      )
    }

    return HttpResponse.json(supplier)
  }),

  http.put('/api/v1/suppliers/:id', async ({ request, params }) => {
    const body = (await request.json()) as SupplierRequestBody
    const index = suppliers.findIndex((supplier) => supplier.id === params.id)
    const existing = suppliers[index]

    if (!existing) {
      return HttpResponse.json(
        {
          status: 404,
          code: 'SUPPLIER_NOT_FOUND',
          message: `Supplier ${params.id} not found`,
          correlationId: 'test-correlation-id',
        },
        { status: 404 },
      )
    }

    const updated: Supplier = {
      ...existing,
      name: body.name,
      taxId: body.taxId ?? null,
      email: body.email ?? null,
      phone: body.phone ?? null,
      address: body.address ?? null,
    }
    suppliers = suppliers.map((supplier, i) => (i === index ? updated : supplier))

    return HttpResponse.json(updated)
  }),

  http.delete('/api/v1/suppliers/:id', ({ params }) => {
    suppliers = suppliers.filter((supplier) => supplier.id !== params.id)
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

    // Mirrors the backend's ORDER BY: jobs with no actual start sort first, then most recently
    // started first among the rest.
    const sorted = [...source].sort((a, b) => {
      if (a.actualStart == null && b.actualStart == null) return 0
      if (a.actualStart == null) return -1
      if (b.actualStart == null) return 1
      return b.actualStart.localeCompare(a.actualStart)
    })

    const start = page * size
    const content = sorted.slice(start, start + size)

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
      actualStart: body.actualStart ?? null,
      actualEnd: body.actualEnd ?? null,
      startUsageValue: null,
      endUsageValue: null,
      createdAt: new Date().toISOString(),
    }
    jobs = [...jobs, newJob]

    return HttpResponse.json(newJob, { status: 201 })
  }),

  http.put('/api/v1/jobs/:id', async ({ request, params }) => {
    const body = (await request.json()) as JobRequestBody
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

    const updated: Job = {
      ...existing,
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
      originLocation: body.originLocation,
      destinationLocation: body.destinationLocation,
      notes: body.notes ?? null,
      scheduledStart: body.scheduledStart ?? null,
      scheduledEnd: body.scheduledEnd ?? null,
      actualStart: body.actualStart ?? null,
      actualEnd: body.actualEnd ?? null,
    }
    jobs = jobs.map((job, i) => (i === index ? updated : job))

    return HttpResponse.json(updated)
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
      // Preserve a manually-set actualStart instead of overwriting it — mirrors JobService.start().
      actualStart: existing.actualStart ?? new Date().toISOString(),
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
      // Preserve a manually-set actualEnd instead of overwriting it — mirrors JobService.complete().
      actualEnd: existing.actualEnd ?? new Date().toISOString(),
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
    const vehicleId = url.searchParams.get('vehicleId')
    const year = url.searchParams.get('year')
    const month = url.searchParams.get('month')

    const filtered = maintenanceRecords.filter((record) => {
      if (vehicleId && record.vehicleId !== vehicleId) return false
      if ((year || month) && !record.workshopEntryDate) return false
      if (year && record.workshopEntryDate!.slice(0, 4) !== year) return false
      if (month && Number(record.workshopEntryDate!.slice(5, 7)) !== Number(month)) return false
      return true
    })
    const start = page * size
    const content = filtered.slice(start, start + size)

    return HttpResponse.json({
      content,
      page,
      size,
      totalElements: filtered.length,
      totalPages: Math.max(1, Math.ceil(filtered.length / size)),
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
      workshopEntryTime: null,
      workshopExitTime: null,
      technicianId: technician?.id ?? null,
      technicianName: technician?.fullName ?? null,
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
      // Fixed, deterministic time — mirrors the rangeTags comment above: real-clock-derived
      // values here would make time-ordering assertions flaky depending on when the test runs.
      workshopEntryTime: '08:00:00',
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
      // Fixed, deterministic time — same rationale as the /start handler above.
      workshopExitTime: '16:00:00',
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
      scheduledStartTime: body.scheduledStartTime,
      scheduledEndTime: body.scheduledEndTime,
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
      scheduledStartTime: body.scheduledStartTime ?? null,
      scheduledEndTime: body.scheduledEndTime ?? null,
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

  http.get('/api/v1/invoices', ({ request }) => {
    const url = new URL(request.url)
    const page = Number(url.searchParams.get('page') ?? 0)
    const size = Number(url.searchParams.get('size') ?? 20)
    const start = page * size
    const content = invoices.slice(start, start + size)

    return HttpResponse.json({
      content,
      page,
      size,
      totalElements: invoices.length,
      totalPages: Math.max(1, Math.ceil(invoices.length / size)),
    })
  }),

  http.post('/api/v1/invoices', async ({ request }) => {
    const body = (await request.json()) as InvoiceRequestBody

    const client = clients.find((c) => c.id === body.clientId)
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

    const newInvoice = recalcInvoiceTotals({
      id: `invoice-${invoices.length + 1}`,
      invoiceNumber: `INV-2026-${String(invoices.length + 1).padStart(5, '0')}`,
      clientId: client.id,
      clientName: client.name,
      status: 'DRAFT',
      issueDate: null,
      dueDate: body.dueDate ?? null,
      paymentDate: null,
      taxRate: body.taxRate ?? DEFAULT_TAX_RATE,
      subtotal: 0,
      taxAmount: 0,
      total: 0,
      notes: body.notes ?? null,
      createdAt: new Date().toISOString(),
      lineItems: [],
    })
    invoices = [...invoices, newInvoice]

    return HttpResponse.json(newInvoice, { status: 201 })
  }),

  http.get('/api/v1/invoices/:id', ({ params }) => {
    const invoice = invoices.find((i) => i.id === params.id)

    if (!invoice) {
      return HttpResponse.json(
        {
          status: 404,
          code: 'INVOICE_NOT_FOUND',
          message: `Invoice ${params.id} not found`,
          correlationId: 'test-correlation-id',
        },
        { status: 404 },
      )
    }

    return HttpResponse.json(invoice)
  }),

  http.put('/api/v1/invoices/:id', async ({ request, params }) => {
    const body = (await request.json()) as InvoiceRequestBody
    const index = invoices.findIndex((invoice) => invoice.id === params.id)
    const existing = invoices[index]

    if (!existing) {
      return HttpResponse.json(
        {
          status: 404,
          code: 'INVOICE_NOT_FOUND',
          message: `Invoice ${params.id} not found`,
          correlationId: 'test-correlation-id',
        },
        { status: 404 },
      )
    }

    if (existing.status !== 'DRAFT') {
      return HttpResponse.json(
        {
          status: 409,
          code: 'INVOICE_INVALID_STATE_TRANSITION',
          message: `Invoice ${params.id} cannot be updated from state ${existing.status}`,
          correlationId: 'test-correlation-id',
        },
        { status: 409 },
      )
    }

    const client = clients.find((c) => c.id === body.clientId)
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

    const updated = recalcInvoiceTotals({
      ...existing,
      clientId: client.id,
      clientName: client.name,
      dueDate: body.dueDate ?? null,
      notes: body.notes ?? null,
      taxRate: body.taxRate ?? DEFAULT_TAX_RATE,
    })
    invoices = invoices.map((invoice, i) => (i === index ? updated : invoice))

    return HttpResponse.json(updated)
  }),

  http.delete('/api/v1/invoices/:id', ({ params }) => {
    const existing = invoices.find((invoice) => invoice.id === params.id)

    if (!existing) {
      return HttpResponse.json(
        {
          status: 404,
          code: 'INVOICE_NOT_FOUND',
          message: `Invoice ${params.id} not found`,
          correlationId: 'test-correlation-id',
        },
        { status: 404 },
      )
    }

    if (existing.status !== 'DRAFT') {
      return HttpResponse.json(
        {
          status: 409,
          code: 'INVOICE_INVALID_STATE_TRANSITION',
          message: `Invoice ${params.id} cannot be deleted from state ${existing.status}`,
          correlationId: 'test-correlation-id',
        },
        { status: 409 },
      )
    }

    invoices = invoices.filter((invoice) => invoice.id !== params.id)
    return new HttpResponse(null, { status: 204 })
  }),

  http.patch('/api/v1/invoices/:id/issue', ({ params }) => {
    const index = invoices.findIndex((invoice) => invoice.id === params.id)
    const existing = invoices[index]

    if (!existing) {
      return HttpResponse.json(
        {
          status: 404,
          code: 'INVOICE_NOT_FOUND',
          message: `Invoice ${params.id} not found`,
          correlationId: 'test-correlation-id',
        },
        { status: 404 },
      )
    }

    if (existing.status !== 'DRAFT') {
      return HttpResponse.json(
        {
          status: 409,
          code: 'INVOICE_INVALID_STATE_TRANSITION',
          message: `Invoice ${params.id} cannot be issued from state ${existing.status}`,
          correlationId: 'test-correlation-id',
        },
        { status: 409 },
      )
    }

    if (existing.lineItems.length === 0) {
      return HttpResponse.json(
        {
          status: 409,
          code: 'INVOICE_NO_LINE_ITEMS',
          message: `Invoice ${params.id} has no line items`,
          correlationId: 'test-correlation-id',
        },
        { status: 409 },
      )
    }

    const updated: InvoiceMock = {
      ...existing,
      status: 'ISSUED',
      issueDate: new Date().toISOString().slice(0, 10),
    }
    invoices = invoices.map((invoice, i) => (i === index ? updated : invoice))

    return HttpResponse.json(updated)
  }),

  http.patch('/api/v1/invoices/:id/pay', async ({ request, params }) => {
    const index = invoices.findIndex((invoice) => invoice.id === params.id)
    const existing = invoices[index]

    if (!existing) {
      return HttpResponse.json(
        {
          status: 404,
          code: 'INVOICE_NOT_FOUND',
          message: `Invoice ${params.id} not found`,
          correlationId: 'test-correlation-id',
        },
        { status: 404 },
      )
    }

    if (existing.status !== 'ISSUED') {
      return HttpResponse.json(
        {
          status: 409,
          code: 'INVOICE_INVALID_STATE_TRANSITION',
          message: `Invoice ${params.id} cannot be paid from state ${existing.status}`,
          correlationId: 'test-correlation-id',
        },
        { status: 409 },
      )
    }

    const body = (await request.json().catch(() => null)) as PayInvoiceRequestBody | null

    const updated: InvoiceMock = {
      ...existing,
      status: 'PAID',
      paymentDate: body?.paymentDate ?? new Date().toISOString().slice(0, 10),
    }
    invoices = invoices.map((invoice, i) => (i === index ? updated : invoice))

    return HttpResponse.json(updated)
  }),

  http.post('/api/v1/invoices/:id/line-items', async ({ request, params }) => {
    const index = invoices.findIndex((invoice) => invoice.id === params.id)
    const existing = invoices[index]

    if (!existing) {
      return HttpResponse.json(
        {
          status: 404,
          code: 'INVOICE_NOT_FOUND',
          message: `Invoice ${params.id} not found`,
          correlationId: 'test-correlation-id',
        },
        { status: 404 },
      )
    }

    if (existing.status !== 'DRAFT') {
      return HttpResponse.json(
        {
          status: 409,
          code: 'INVOICE_INVALID_STATE_TRANSITION',
          message: `Invoice ${params.id} cannot receive line items from state ${existing.status}`,
          correlationId: 'test-correlation-id',
        },
        { status: 409 },
      )
    }

    const body = (await request.json()) as LineItemRequestBody
    lineItemSequence += 1
    const newLineItem = buildLineItemMock(`line-item-${lineItemSequence}`, body)

    const updated = recalcInvoiceTotals({ ...existing, lineItems: [...existing.lineItems, newLineItem] })
    invoices = invoices.map((invoice, i) => (i === index ? updated : invoice))

    return HttpResponse.json(updated, { status: 201 })
  }),

  http.get('/api/v1/invoices/:id/pdf', ({ params }) => {
    const existing = invoices.find((invoice) => invoice.id === params.id)

    if (!existing) {
      return HttpResponse.json(
        {
          status: 404,
          code: 'INVOICE_NOT_FOUND',
          message: `Invoice ${params.id} not found`,
          correlationId: 'test-correlation-id',
        },
        { status: 404 },
      )
    }

    // A real (if minimal) PDF — asserting actual invoice content is the backend's responsibility
    // (PdfExportServiceTest); this only needs to exercise the blob-download plumbing, but it must
    // still be a structurally valid PDF so manually testing the download against this mock in
    // local dev (VITE_ENABLE_MSW=true) actually opens, instead of failing with a viewer error that
    // looks identical to a real corruption bug.
    return new HttpResponse(buildMinimalPdf(`Mock PDF for ${existing.invoiceNumber}`), {
      headers: {
        'Content-Type': 'application/pdf',
        'Content-Disposition': `attachment; filename="${existing.invoiceNumber}.pdf"`,
      },
    })
  }),

  http.get('/api/v1/supplier-invoices', ({ request }) => {
    const url = new URL(request.url)
    const page = Number(url.searchParams.get('page') ?? 0)
    const size = Number(url.searchParams.get('size') ?? 20)
    const vehicleId = url.searchParams.get('vehicleId')
    const category = url.searchParams.get('category') as ExpenseCategory | null
    const supplierId = url.searchParams.get('supplierId')
    const status = url.searchParams.get('status') as SupplierInvoiceStatus | null
    const invoiceDateFrom = url.searchParams.get('invoiceDateFrom')
    const invoiceDateTo = url.searchParams.get('invoiceDateTo')
    const dueDateFrom = url.searchParams.get('dueDateFrom')
    const dueDateTo = url.searchParams.get('dueDateTo')
    const totalMin = url.searchParams.get('totalMin')
    const totalMax = url.searchParams.get('totalMax')

    const source = supplierInvoices.filter(
      (invoice) =>
        (vehicleId == null || invoice.vehicleId === vehicleId) &&
        (category == null || invoice.category === category) &&
        (supplierId == null || invoice.supplierId === supplierId) &&
        (status == null || invoice.status === status) &&
        (invoiceDateFrom == null || invoice.invoiceDate >= invoiceDateFrom) &&
        (invoiceDateTo == null || invoice.invoiceDate <= invoiceDateTo) &&
        (dueDateFrom == null || (invoice.dueDate != null && invoice.dueDate >= dueDateFrom)) &&
        (dueDateTo == null || (invoice.dueDate != null && invoice.dueDate <= dueDateTo)) &&
        (totalMin == null || invoice.total >= Number(totalMin)) &&
        (totalMax == null || invoice.total <= Number(totalMax)),
    )
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

  http.post('/api/v1/supplier-invoices', async ({ request }) => {
    const body = (await request.json()) as SupplierInvoiceRequestBody

    const supplier = suppliers.find((s) => s.id === body.supplierId)
    if (!supplier) {
      return HttpResponse.json(
        {
          status: 404,
          code: 'SUPPLIER_NOT_FOUND',
          message: `Supplier ${body.supplierId} not found`,
          correlationId: 'test-correlation-id',
        },
        { status: 404 },
      )
    }

    let vehicle: Vehicle | undefined
    if (body.vehicleId) {
      vehicle = vehicles.find((v) => v.id === body.vehicleId)
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
    }

    const newInvoice: SupplierInvoiceMock = {
      id: `supplier-invoice-${supplierInvoices.length + 1}`,
      supplierId: supplier.id,
      supplierName: supplier.name,
      supplierInvoiceNumber: body.supplierInvoiceNumber ?? null,
      category: body.category,
      invoiceDate: body.invoiceDate,
      dueDate: body.dueDate ?? null,
      paymentDate: null,
      status: 'PENDING',
      subtotal: body.subtotal,
      taxAmount: body.taxAmount,
      total: body.total,
      vehicleId: vehicle?.id ?? null,
      vehicleLicensePlate: vehicle?.licensePlate ?? null,
      vehicleMake: vehicle?.make ?? null,
      vehicleModel: vehicle?.model ?? null,
      notes: body.notes ?? null,
      documentPath: body.documentPath ?? null,
      createdAt: new Date().toISOString(),
      lineItems: [],
    }
    supplierInvoices = [...supplierInvoices, newInvoice]

    return HttpResponse.json(newInvoice, { status: 201 })
  }),

  http.get('/api/v1/supplier-invoices/:id', ({ params }) => {
    const invoice = supplierInvoices.find((i) => i.id === params.id)

    if (!invoice) {
      return HttpResponse.json(
        {
          status: 404,
          code: 'SUPPLIER_INVOICE_NOT_FOUND',
          message: `Supplier invoice ${params.id} not found`,
          correlationId: 'test-correlation-id',
        },
        { status: 404 },
      )
    }

    return HttpResponse.json(invoice)
  }),

  http.put('/api/v1/supplier-invoices/:id', async ({ request, params }) => {
    const body = (await request.json()) as SupplierInvoiceRequestBody
    const index = supplierInvoices.findIndex((invoice) => invoice.id === params.id)
    const existing = supplierInvoices[index]

    if (!existing) {
      return HttpResponse.json(
        {
          status: 404,
          code: 'SUPPLIER_INVOICE_NOT_FOUND',
          message: `Supplier invoice ${params.id} not found`,
          correlationId: 'test-correlation-id',
        },
        { status: 404 },
      )
    }

    if (existing.status !== 'PENDING') {
      return HttpResponse.json(
        {
          status: 409,
          code: 'SUPPLIER_INVOICE_INVALID_STATE_TRANSITION',
          message: `Supplier invoice ${params.id} cannot be updated from state ${existing.status}`,
          correlationId: 'test-correlation-id',
        },
        { status: 409 },
      )
    }

    if (body.vehicleId && existing.lineItems.length > 0) {
      return HttpResponse.json(
        {
          status: 409,
          code: 'SUPPLIER_INVOICE_VEHICLE_LINE_ITEMS_CONFLICT',
          message: `Supplier invoice ${params.id} has line items and cannot also have a header vehicle`,
          correlationId: 'test-correlation-id',
        },
        { status: 409 },
      )
    }

    const supplier = suppliers.find((s) => s.id === body.supplierId)
    if (!supplier) {
      return HttpResponse.json(
        {
          status: 404,
          code: 'SUPPLIER_NOT_FOUND',
          message: `Supplier ${body.supplierId} not found`,
          correlationId: 'test-correlation-id',
        },
        { status: 404 },
      )
    }

    let vehicle: Vehicle | undefined
    if (body.vehicleId) {
      vehicle = vehicles.find((v) => v.id === body.vehicleId)
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
    }

    const updated: SupplierInvoiceMock = {
      ...existing,
      supplierId: supplier.id,
      supplierName: supplier.name,
      supplierInvoiceNumber: body.supplierInvoiceNumber ?? null,
      category: body.category,
      invoiceDate: body.invoiceDate,
      dueDate: body.dueDate ?? null,
      vehicleId: vehicle?.id ?? null,
      vehicleLicensePlate: vehicle?.licensePlate ?? null,
      vehicleMake: vehicle?.make ?? null,
      vehicleModel: vehicle?.model ?? null,
      subtotal: body.subtotal,
      taxAmount: body.taxAmount,
      total: body.total,
      notes: body.notes ?? null,
      documentPath: body.documentPath ?? null,
    }
    supplierInvoices = supplierInvoices.map((invoice, i) => (i === index ? updated : invoice))

    return HttpResponse.json(updated)
  }),

  http.patch('/api/v1/supplier-invoices/:id/pay', async ({ request, params }) => {
    const index = supplierInvoices.findIndex((invoice) => invoice.id === params.id)
    const existing = supplierInvoices[index]

    if (!existing) {
      return HttpResponse.json(
        {
          status: 404,
          code: 'SUPPLIER_INVOICE_NOT_FOUND',
          message: `Supplier invoice ${params.id} not found`,
          correlationId: 'test-correlation-id',
        },
        { status: 404 },
      )
    }

    if (existing.status !== 'PENDING') {
      return HttpResponse.json(
        {
          status: 409,
          code: 'SUPPLIER_INVOICE_INVALID_STATE_TRANSITION',
          message: `Supplier invoice ${params.id} cannot be paid from state ${existing.status}`,
          correlationId: 'test-correlation-id',
        },
        { status: 409 },
      )
    }

    if (existing.vehicleId == null && existing.lineItems.length > 0) {
      const linesSum = existing.lineItems.reduce((sum, lineItem) => sum + lineItem.subtotal, 0)
      if (Math.round(linesSum * 100) !== Math.round(existing.subtotal * 100)) {
        return HttpResponse.json(
          {
            status: 409,
            code: 'SUPPLIER_INVOICE_ALLOCATION_INCOMPLETE',
            message: `Supplier invoice ${params.id} line items total ${linesSum} but the invoice subtotal is ${existing.subtotal}`,
            correlationId: 'test-correlation-id',
          },
          { status: 409 },
        )
      }
    }

    const body = (await request.json().catch(() => null)) as PayInvoiceRequestBody | null

    const updated: SupplierInvoiceMock = {
      ...existing,
      status: 'PAID',
      paymentDate: body?.paymentDate ?? new Date().toISOString().slice(0, 10),
    }
    supplierInvoices = supplierInvoices.map((invoice, i) => (i === index ? updated : invoice))

    return HttpResponse.json(updated)
  }),

  http.post('/api/v1/supplier-invoices/:id/line-items', async ({ request, params }) => {
    const index = supplierInvoices.findIndex((invoice) => invoice.id === params.id)
    const existing = supplierInvoices[index]

    if (!existing) {
      return HttpResponse.json(
        {
          status: 404,
          code: 'SUPPLIER_INVOICE_NOT_FOUND',
          message: `Supplier invoice ${params.id} not found`,
          correlationId: 'test-correlation-id',
        },
        { status: 404 },
      )
    }

    if (existing.status !== 'PENDING') {
      return HttpResponse.json(
        {
          status: 409,
          code: 'SUPPLIER_INVOICE_INVALID_STATE_TRANSITION',
          message: `Supplier invoice ${params.id} cannot receive line items from state ${existing.status}`,
          correlationId: 'test-correlation-id',
        },
        { status: 409 },
      )
    }

    if (existing.vehicleId != null) {
      return HttpResponse.json(
        {
          status: 409,
          code: 'SUPPLIER_INVOICE_VEHICLE_LINE_ITEMS_CONFLICT',
          message: `Supplier invoice ${params.id} has a header vehicle and cannot also receive line items`,
          correlationId: 'test-correlation-id',
        },
        { status: 409 },
      )
    }

    const body = (await request.json()) as SupplierLineItemRequestBody
    supplierLineItemSequence += 1
    const newLineItem = buildSupplierLineItemMock(`supplier-line-item-${supplierLineItemSequence}`, body)

    const updated: SupplierInvoiceMock = { ...existing, lineItems: [...existing.lineItems, newLineItem] }
    supplierInvoices = supplierInvoices.map((invoice, i) => (i === index ? updated : invoice))

    // The real backend endpoint returns only the created line item (SupplierLineItemResponse),
    // not the parent invoice — see SupplierInvoiceController.addLineItem. The caller relies on
    // query invalidation (useAddSupplierLineItem) to refresh the invoice's lineItems array.
    return HttpResponse.json(newLineItem, { status: 201 })
  }),

  http.put('/api/v1/supplier-invoices/:id/line-items/:lineItemId', async ({ request, params }) => {
    const index = supplierInvoices.findIndex((invoice) => invoice.id === params.id)
    const existing = supplierInvoices[index]

    if (!existing) {
      return HttpResponse.json(
        {
          status: 404,
          code: 'SUPPLIER_INVOICE_NOT_FOUND',
          message: `Supplier invoice ${params.id} not found`,
          correlationId: 'test-correlation-id',
        },
        { status: 404 },
      )
    }

    if (existing.status !== 'PENDING') {
      return HttpResponse.json(
        {
          status: 409,
          code: 'SUPPLIER_INVOICE_INVALID_STATE_TRANSITION',
          message: `Supplier invoice ${params.id} line items cannot be modified from state ${existing.status}`,
          correlationId: 'test-correlation-id',
        },
        { status: 409 },
      )
    }

    const lineItemIndex = existing.lineItems.findIndex((lineItem) => lineItem.id === params.lineItemId)
    if (lineItemIndex === -1) {
      return HttpResponse.json(
        {
          status: 404,
          code: 'SUPPLIER_LINE_ITEM_NOT_FOUND',
          message: `Line item ${params.lineItemId} not found on invoice ${params.id}`,
          correlationId: 'test-correlation-id',
        },
        { status: 404 },
      )
    }

    const body = (await request.json()) as SupplierLineItemRequestBody
    const updatedLineItem = buildSupplierLineItemMock(params.lineItemId as string, body)
    const updated: SupplierInvoiceMock = {
      ...existing,
      lineItems: existing.lineItems.map((lineItem, i) => (i === lineItemIndex ? updatedLineItem : lineItem)),
    }
    supplierInvoices = supplierInvoices.map((invoice, i) => (i === index ? updated : invoice))

    return HttpResponse.json(updatedLineItem)
  }),

  http.delete('/api/v1/supplier-invoices/:id/line-items/:lineItemId', ({ params }) => {
    const index = supplierInvoices.findIndex((invoice) => invoice.id === params.id)
    const existing = supplierInvoices[index]

    if (!existing) {
      return HttpResponse.json(
        {
          status: 404,
          code: 'SUPPLIER_INVOICE_NOT_FOUND',
          message: `Supplier invoice ${params.id} not found`,
          correlationId: 'test-correlation-id',
        },
        { status: 404 },
      )
    }

    if (existing.status !== 'PENDING') {
      return HttpResponse.json(
        {
          status: 409,
          code: 'SUPPLIER_INVOICE_INVALID_STATE_TRANSITION',
          message: `Supplier invoice ${params.id} line items cannot be modified from state ${existing.status}`,
          correlationId: 'test-correlation-id',
        },
        { status: 409 },
      )
    }

    const lineItemExists = existing.lineItems.some((lineItem) => lineItem.id === params.lineItemId)
    if (!lineItemExists) {
      return HttpResponse.json(
        {
          status: 404,
          code: 'SUPPLIER_LINE_ITEM_NOT_FOUND',
          message: `Line item ${params.lineItemId} not found on invoice ${params.id}`,
          correlationId: 'test-correlation-id',
        },
        { status: 404 },
      )
    }

    const updated: SupplierInvoiceMock = {
      ...existing,
      lineItems: existing.lineItems.filter((lineItem) => lineItem.id !== params.lineItemId),
    }
    supplierInvoices = supplierInvoices.map((invoice, i) => (i === index ? updated : invoice))

    return new HttpResponse(null, { status: 204 })
  }),

  http.get('/api/v1/audit', ({ request }) => {
    const url = new URL(request.url)
    const page = Number(url.searchParams.get('page') ?? 0)
    const size = Number(url.searchParams.get('size') ?? 20)
    const entityType = url.searchParams.get('entityType')
    const action = url.searchParams.get('action') as AuditActionMock | null
    const from = url.searchParams.get('from')
    const to = url.searchParams.get('to')
    const performedByEmail = url.searchParams.get('performedByEmail')

    const source = SEED_AUDIT_LOGS.filter(
      (entry) =>
        (entityType == null || entry.entityType === entityType) &&
        (action == null || entry.action === action) &&
        (from == null || entry.performedAt >= from) &&
        (to == null || entry.performedAt <= to) &&
        (performedByEmail == null ||
          (entry.performedByEmail ?? '').toLowerCase().includes(performedByEmail.toLowerCase())),
    )
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

  http.get('/api/v1/audit/performers', () => {
    const emails = [
      ...new Set(
        SEED_AUDIT_LOGS.map((entry) => entry.performedByEmail).filter(
          (email): email is string => email != null,
        ),
      ),
    ].sort()

    return HttpResponse.json(emails.map((email) => ({ email })))
  }),

  http.get('/api/v1/reports/fleet-summary', () => {
    return HttpResponse.json(SEED_FLEET_SUMMARY)
  }),

  http.get('/api/v1/reports/financial-summary', () => {
    return HttpResponse.json(SEED_FINANCIAL_SUMMARY)
  }),

  http.get('/api/v1/reports/profitability', ({ request }) => {
    const url = new URL(request.url)
    const page = Number(url.searchParams.get('page') ?? 0)
    const size = Number(url.searchParams.get('size') ?? 20)

    const start = page * size
    const content = SEED_PROFITABILITY.slice(start, start + size)

    return HttpResponse.json({
      content,
      page,
      size,
      totalElements: SEED_PROFITABILITY.length,
      totalPages: Math.max(1, Math.ceil(SEED_PROFITABILITY.length / size)),
    })
  }),

  // Last N months ending "now" — simplest deterministic slice of the fixed 12-entry fixture is
  // just the last N entries, since SEED_FINANCIAL_TREND's final entry already represents the
  // current month.
  http.get('/api/v1/reports/profitability/trend', ({ request }) => {
    const url = new URL(request.url)
    const months = Number(url.searchParams.get('months') ?? 6)

    return HttpResponse.json(SEED_FINANCIAL_TREND.slice(-months))
  }),

  // Registered before the :vehicleId catch-all below — same shadowing risk as /trend above,
  // this time for the /:vehicleId/revenue sub-route.
  http.get('/api/v1/reports/profitability/:vehicleId/revenue', ({ params, request }) => {
    const url = new URL(request.url)
    const year = url.searchParams.get('year')
    const month = url.searchParams.get('month')

    const content = SEED_VEHICLE_REVENUE.filter((item) => item.vehicleId === params.vehicleId)
      .filter((item) => !year || item.issueDate.slice(0, 4) === year)
      .filter((item) => !month || Number(item.issueDate.slice(5, 7)) === Number(month))
      .map(({ vehicleId: _vehicleId, ...rest }) => rest)

    return HttpResponse.json(content)
  }),

  // Registered after the more specific /reports/profitability/trend handler above — MSW matches
  // handlers in array order, and this :vehicleId pattern would otherwise shadow /trend requests.
  http.get('/api/v1/reports/profitability/:vehicleId', ({ params }) => {
    const profitability = SEED_PROFITABILITY.find((entry) => entry.vehicleId === params.vehicleId)

    if (!profitability) {
      return HttpResponse.json(
        {
          status: 404,
          code: 'VEHICLE_NOT_FOUND',
          message: `Vehicle ${params.vehicleId} not found`,
          correlationId: 'test-correlation-id',
        },
        { status: 404 },
      )
    }

    return HttpResponse.json(profitability)
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
