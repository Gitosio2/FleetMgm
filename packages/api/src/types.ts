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
