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

export type Supplier = {
  id: string
  name: string
  taxId: string | null
  email: string | null
  phone: string | null
  address: string | null
  createdAt: string
}

export type CreateSupplierRequest = {
  name: string
  taxId?: string | null
  email?: string
  phone?: string
  address?: string
}

export type UpdateSupplierRequest = CreateSupplierRequest

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
  actualStart?: string | null
  actualEnd?: string | null
}

export type UpdateJobRequest = CreateJobRequest

export type MaintenanceStatus = 'SCHEDULED' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED'
export type MaintenanceCategory = 'PREVENTIVE' | 'CORRECTIVE'

export type MaintenanceRecord = {
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

export type CreateMaintenanceRequest = {
  vehicleId: string
  type: string
  description?: string | null
  technicianId?: string | null
  category?: MaintenanceCategory | null
  scheduledDate: string
}

export type UpdateMaintenanceRequest = {
  vehicleId: string
  type: string
  description?: string | null
  technicianId?: string | null
  category: MaintenanceCategory
}

export type SchedulePriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT'
export type WorkshopStatus = 'PENDING' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED'
export type ScheduleRange = 'today' | 'week' | 'month'

export type WorkshopSchedule = {
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
}

export type CreateScheduleRequest = {
  vehicleId: string
  technicianId?: string | null
  maintenanceRecordId?: string | null
  scheduledDate: string
  scheduledStartTime?: string | null
  scheduledEndTime?: string | null
  type: string
  priority?: SchedulePriority | null
  notes?: string | null
  category?: MaintenanceCategory | null
}

export type UpdateScheduleRequest = {
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

export type InvoiceStatus = 'DRAFT' | 'ISSUED' | 'PAID' | 'OVERDUE' | 'CANCELLED'

export type LineItemResponse = {
  id: string
  description: string
  quantity: number
  unitPrice: number
  subtotal: number
  linkedJobId: string | null
}

export type LineItemRequest = {
  description: string
  quantity: number
  unitPrice: number
  linkedJobId?: string | null
}

export type Invoice = {
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
  lineItems: LineItemResponse[]
}

export type CreateInvoiceRequest = {
  clientId: string
  dueDate?: string | null
  notes?: string | null
  taxRate?: number | null
}

export type UpdateInvoiceRequest = CreateInvoiceRequest

export type PayInvoiceRequest = {
  paymentDate?: string | null
}

export type ExpenseCategory = 'MAINTENANCE' | 'FUEL' | 'INSURANCE' | 'LEASING_RENTING' | 'TOLL' | 'OTHER'
export type SupplierInvoiceStatus = 'PENDING' | 'PAID'

export type SupplierLineItemResponse = {
  id: string
  description: string
  quantity: number
  unitPrice: number
  subtotal: number
  vehicleId: string | null
  maintenanceRecordId: string | null
}

export type SupplierLineItemRequest = {
  description: string
  quantity: number
  subtotal: number
  vehicleId?: string | null
  maintenanceRecordId?: string | null
}

export type SupplierInvoice = {
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
  lineItems: SupplierLineItemResponse[]
}

export type CreateSupplierInvoiceRequest = {
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

export type UpdateSupplierInvoiceRequest = CreateSupplierInvoiceRequest

export type AuditAction = 'CREATE' | 'UPDATE' | 'DELETE' | 'LOGIN' | 'LOGOUT' | 'ACCESS_DENIED' | 'ACCOUNT_LOCKED'

export type AuditLog = {
  id: string
  entityType: string
  entityId: string
  action: AuditAction
  performedByUserId: string | null
  performedByEmail: string | null
  performedAt: string
  ipAddress: string | null
  oldValues: string | null
  newValues: string | null
  details: string | null
}

export type AuditLogPerformer = {
  email: string
}

export type FleetSummary = {
  activeVehicles: number
  totalVehicles: number
  inWorkshop: number
  pendingMaintenance: number
  pendingMaintenanceDueSoon: number
}

export type UpcomingInvoice = {
  id: string
  number: string
  counterpartyId: string
  counterparty: string
  amount: number
  dueDate: string
  overdue: boolean
}

export type FinancialSummary = {
  monthlyCosts: number
  upcomingReceivables: UpcomingInvoice[]
  upcomingPayables: UpcomingInvoice[]
}

export type Profitability = {
  vehicleId: string
  vehicleLicensePlate: string | null
  vehicleMake: string
  vehicleModel: string
  revenue: number
  costs: number
  margin: number
}

export type MonthlyFinancial = {
  month: string
  revenue: number
  costs: number
}

export type VehicleRevenueLineItem = {
  invoiceNumber: string
  issueDate: string
  description: string
  quantity: number
  unitPrice: number
  subtotal: number
}

export type GpsSource = 'MOCK' | 'DEVICE'

export type GpsPosition = {
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
  source: GpsSource
}
