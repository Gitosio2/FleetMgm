export type PageResponse<T> = {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export type ApiError = {
  status: number
  code: string
  message: string
  correlationId: string
}

export type Client = {
  id: string
  name: string
  taxId: string
  email: string | null
  phone: string | null
  address: string | null
  createdAt: string
}

export type CreateClientRequest = {
  name: string
  taxId: string
  email?: string
  phone?: string
  address?: string
}

export type UpdateClientRequest = CreateClientRequest

export type VehicleCategory = 'LIGHT_VEHICLE' | 'HEAVY_VEHICLE' | 'HEAVY_MACHINERY'
export type UsageMeasure = 'KILOMETERS' | 'HOURS'
export type VehicleStatus = 'ACTIVE' | 'MAINTENANCE' | 'INACTIVE' | 'DECOMMISSIONED'
export type AcquisitionType = 'PURCHASED' | 'LEASING' | 'RENTING'

export type Vehicle = {
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

export type CreateVehicleRequest = {
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

export type UpdateVehicleRequest = CreateVehicleRequest

export type WorkerRole = 'DRIVER' | 'TECHNICIAN' | 'BOTH'

export type Worker = {
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

export type CreateWorkerRequest = {
  firstName: string
  lastName: string
  workerRole: WorkerRole
  nationalId: string
  phone?: string | null
  licenseType?: string | null
  licenseExpiry?: string | null
  userId?: string | null
}

export type UpdateWorkerRequest = {
  firstName: string
  lastName: string
  workerRole: WorkerRole
  phone?: string | null
  licenseType?: string | null
  licenseExpiry?: string | null
}

export type Assignment = {
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

export type CreateAssignmentRequest = {
  driverId: string
  vehicleId: string
  startDate: string
  notes?: string | null
}

export type JobStatus = 'PENDING' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED'

export type Job = {
  id: string
  title: string
  description: string | null
  vehicleId: string
  vehicleLicensePlate: string | null
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

export type CreateJobRequest = {
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

export type UpdateJobRequest = CreateJobRequest
