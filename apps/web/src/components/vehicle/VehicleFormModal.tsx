import { useEffect, useState, type FormEvent } from 'react'
import type {
  AcquisitionType,
  CreateVehicleRequest,
  UsageMeasure,
  Vehicle,
  VehicleCategory,
} from '@fleetmgm/api'
import { useCreateVehicle, useUpdateVehicle } from '@fleetmgm/hooks'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'

const VEHICLE_CATEGORIES: VehicleCategory[] = ['LIGHT_VEHICLE', 'HEAVY_VEHICLE', 'HEAVY_MACHINERY']
const USAGE_MEASURES: UsageMeasure[] = ['KILOMETERS', 'HOURS']
const ACQUISITION_TYPES: AcquisitionType[] = ['PURCHASED', 'LEASING', 'RENTING']

const VEHICLE_CATEGORY_LABEL: Record<VehicleCategory, string> = {
  LIGHT_VEHICLE: 'Vehículo ligero',
  HEAVY_VEHICLE: 'Vehículo pesado',
  HEAVY_MACHINERY: 'Maquinaria pesada',
}

const USAGE_MEASURE_LABEL: Record<UsageMeasure, string> = {
  KILOMETERS: 'Kilómetros',
  HOURS: 'Horas',
}

const ACQUISITION_TYPE_LABEL: Record<AcquisitionType, string> = {
  PURCHASED: 'Compra',
  LEASING: 'Leasing',
  RENTING: 'Renting',
}

const selectClassName =
  'flex h-11 w-full rounded-lg border border-outline-variant bg-surface-container-lowest px-3 py-2 text-sm text-on-surface focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-secondary-container disabled:cursor-not-allowed disabled:opacity-50'

type VehicleFormModalProps = {
  open: boolean
  onOpenChange: (open: boolean) => void
  vehicle?: Vehicle
}

function toNullableNumber(value: string): number | null {
  return value === '' ? null : Number(value)
}

function toNullableString(value: string): string | null {
  return value === '' ? null : value
}

export function VehicleFormModal({ open, onOpenChange, vehicle }: VehicleFormModalProps) {
  const isEditing = vehicle != null
  const createVehicle = useCreateVehicle()
  const updateVehicle = useUpdateVehicle()

  const [vehicleCategory, setVehicleCategory] = useState<VehicleCategory>('LIGHT_VEHICLE')
  const [usageMeasure, setUsageMeasure] = useState<UsageMeasure>('KILOMETERS')
  const [make, setMake] = useState('')
  const [model, setModel] = useState('')
  const [year, setYear] = useState('')
  const [licensePlate, setLicensePlate] = useState('')
  const [heavySubtype, setHeavySubtype] = useState('')
  const [vin, setVin] = useState('')
  const [color, setColor] = useState('')
  const [currentKm, setCurrentKm] = useState('')
  const [currentHours, setCurrentHours] = useState('')
  const [acquisitionType, setAcquisitionType] = useState<AcquisitionType | ''>('')
  const [acquisitionDate, setAcquisitionDate] = useState('')
  const [purchasePrice, setPurchasePrice] = useState('')
  const [amortizationYears, setAmortizationYears] = useState('')
  const [monthlyFee, setMonthlyFee] = useState('')
  const [contractEndDate, setContractEndDate] = useState('')

  useEffect(() => {
    if (!open) {
      return
    }
    setVehicleCategory(vehicle?.vehicleCategory ?? 'LIGHT_VEHICLE')
    setUsageMeasure(vehicle?.usageMeasure ?? 'KILOMETERS')
    setMake(vehicle?.make ?? '')
    setModel(vehicle?.model ?? '')
    setYear(vehicle ? String(vehicle.year) : '')
    setLicensePlate(vehicle?.licensePlate ?? '')
    setHeavySubtype(vehicle?.heavySubtype ?? '')
    setVin(vehicle?.vin ?? '')
    setColor(vehicle?.color ?? '')
    setCurrentKm(vehicle?.currentKm != null ? String(vehicle.currentKm) : '')
    setCurrentHours(vehicle?.currentHours != null ? String(vehicle.currentHours) : '')
    setAcquisitionType(vehicle?.acquisitionType ?? '')
    setAcquisitionDate(vehicle?.acquisitionDate ?? '')
    setPurchasePrice(vehicle?.purchasePrice != null ? String(vehicle.purchasePrice) : '')
    setAmortizationYears(vehicle?.amortizationYears != null ? String(vehicle.amortizationYears) : '')
    setMonthlyFee(vehicle?.monthlyFee != null ? String(vehicle.monthlyFee) : '')
    setContractEndDate(vehicle?.contractEndDate ?? '')
  }, [open, vehicle])

  const isPending = createVehicle.isPending || updateVehicle.isPending

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()

    const request: CreateVehicleRequest = {
      vehicleCategory,
      usageMeasure,
      make,
      model,
      year: Number(year),
      licensePlate: toNullableString(licensePlate),
      heavySubtype: toNullableString(heavySubtype),
      vin: toNullableString(vin),
      color: toNullableString(color),
      currentKm: toNullableNumber(currentKm),
      currentHours: toNullableNumber(currentHours),
      acquisitionType: acquisitionType === '' ? null : acquisitionType,
      acquisitionDate: toNullableString(acquisitionDate),
      purchasePrice: toNullableNumber(purchasePrice),
      amortizationYears: toNullableNumber(amortizationYears),
      monthlyFee: toNullableNumber(monthlyFee),
      contractEndDate: toNullableString(contractEndDate),
    }

    if (vehicle) {
      updateVehicle.mutate({ id: vehicle.id, request }, { onSuccess: () => onOpenChange(false) })
    } else {
      createVehicle.mutate(request, { onSuccess: () => onOpenChange(false) })
    }
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-h-[85vh] max-w-2xl overflow-y-auto">
        <DialogHeader>
          <DialogTitle>{isEditing ? 'Editar vehículo' : 'Nuevo vehículo'}</DialogTitle>
        </DialogHeader>

        <form className="flex flex-col gap-4" onSubmit={handleSubmit}>
          <div className="grid grid-cols-2 gap-4">
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="vehicle-category">Categoría</Label>
              <select
                id="vehicle-category"
                className={selectClassName}
                value={vehicleCategory}
                onChange={(e) => setVehicleCategory(e.target.value as VehicleCategory)}
              >
                {VEHICLE_CATEGORIES.map((category) => (
                  <option key={category} value={category}>
                    {VEHICLE_CATEGORY_LABEL[category]}
                  </option>
                ))}
              </select>
            </div>

            <div className="flex flex-col gap-1.5">
              <Label htmlFor="vehicle-usage-measure">Unidad de uso</Label>
              <select
                id="vehicle-usage-measure"
                className={selectClassName}
                value={usageMeasure}
                onChange={(e) => setUsageMeasure(e.target.value as UsageMeasure)}
              >
                {USAGE_MEASURES.map((measure) => (
                  <option key={measure} value={measure}>
                    {USAGE_MEASURE_LABEL[measure]}
                  </option>
                ))}
              </select>
            </div>
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="vehicle-make">Marca</Label>
              <Input id="vehicle-make" value={make} onChange={(e) => setMake(e.target.value)} required />
            </div>
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="vehicle-model">Modelo</Label>
              <Input id="vehicle-model" value={model} onChange={(e) => setModel(e.target.value)} required />
            </div>
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="vehicle-year">Año</Label>
              <Input
                id="vehicle-year"
                type="number"
                value={year}
                onChange={(e) => setYear(e.target.value)}
                required
              />
            </div>
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="vehicle-license-plate">Matrícula</Label>
              <Input
                id="vehicle-license-plate"
                value={licensePlate}
                onChange={(e) => setLicensePlate(e.target.value)}
              />
            </div>
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="vehicle-heavy-subtype">Subtipo pesado</Label>
              <Input
                id="vehicle-heavy-subtype"
                value={heavySubtype}
                onChange={(e) => setHeavySubtype(e.target.value)}
              />
            </div>
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="vehicle-vin">VIN</Label>
              <Input id="vehicle-vin" value={vin} onChange={(e) => setVin(e.target.value)} />
            </div>
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="vehicle-color">Color</Label>
              <Input id="vehicle-color" value={color} onChange={(e) => setColor(e.target.value)} />
            </div>
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="vehicle-acquisition-type">Tipo de adquisición</Label>
              <select
                id="vehicle-acquisition-type"
                className={selectClassName}
                value={acquisitionType}
                onChange={(e) => setAcquisitionType(e.target.value as AcquisitionType | '')}
              >
                <option value="">—</option>
                {ACQUISITION_TYPES.map((type) => (
                  <option key={type} value={type}>
                    {ACQUISITION_TYPE_LABEL[type]}
                  </option>
                ))}
              </select>
            </div>
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="vehicle-current-km">Km actuales</Label>
              <Input
                id="vehicle-current-km"
                type="number"
                value={currentKm}
                onChange={(e) => setCurrentKm(e.target.value)}
              />
            </div>
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="vehicle-current-hours">Horas actuales</Label>
              <Input
                id="vehicle-current-hours"
                type="number"
                value={currentHours}
                onChange={(e) => setCurrentHours(e.target.value)}
              />
            </div>
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="vehicle-acquisition-date">Fecha de adquisición</Label>
              <Input
                id="vehicle-acquisition-date"
                type="date"
                value={acquisitionDate}
                onChange={(e) => setAcquisitionDate(e.target.value)}
              />
            </div>
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="vehicle-contract-end-date">Fecha de fin de contrato</Label>
              <Input
                id="vehicle-contract-end-date"
                type="date"
                value={contractEndDate}
                onChange={(e) => setContractEndDate(e.target.value)}
              />
            </div>
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="vehicle-purchase-price">Precio de compra</Label>
              <Input
                id="vehicle-purchase-price"
                type="number"
                value={purchasePrice}
                onChange={(e) => setPurchasePrice(e.target.value)}
              />
            </div>
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="vehicle-amortization-years">Años de amortización</Label>
              <Input
                id="vehicle-amortization-years"
                type="number"
                value={amortizationYears}
                onChange={(e) => setAmortizationYears(e.target.value)}
              />
            </div>
          </div>

          <div className="flex flex-col gap-1.5">
            <Label htmlFor="vehicle-monthly-fee">Cuota mensual</Label>
            <Input
              id="vehicle-monthly-fee"
              type="number"
              value={monthlyFee}
              onChange={(e) => setMonthlyFee(e.target.value)}
            />
          </div>

          <DialogFooter>
            <Button type="submit" disabled={isPending}>
              {isEditing ? 'Guardar cambios' : 'Crear vehículo'}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}
