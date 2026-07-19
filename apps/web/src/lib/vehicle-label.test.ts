import { describe, expect, it } from 'vitest'
import { formatVehicleLabel, formatVehicleSelectLabel } from './vehicle-label'

describe('formatVehicleLabel', () => {
  it('returns the license plate when present', () => {
    expect(
      formatVehicleLabel({
        vehicleLicensePlate: '1234ABC',
        vehicleMake: 'Toyota',
        vehicleModel: 'Hilux',
      }),
    ).toBe('1234ABC')
  })

  it('falls back to "<make> <model>" when there is no license plate', () => {
    expect(
      formatVehicleLabel({
        vehicleLicensePlate: null,
        vehicleMake: 'Caterpillar',
        vehicleModel: '320 Excavator',
      }),
    ).toBe('Caterpillar 320 Excavator')
  })

  it('returns "—" when there is no license plate, make, or model', () => {
    expect(
      formatVehicleLabel({
        vehicleLicensePlate: null,
        vehicleMake: null,
        vehicleModel: null,
      }),
    ).toBe('—')
  })
})

describe('formatVehicleSelectLabel', () => {
  it('puts the license plate first, followed by make and model', () => {
    expect(
      formatVehicleSelectLabel({
        make: 'Toyota',
        model: 'Hilux',
        licensePlate: '1234ABC',
      }),
    ).toBe('1234ABC - Toyota Hilux')
  })

  it('falls back to "<make> <model>" when there is no license plate', () => {
    expect(
      formatVehicleSelectLabel({
        make: 'Caterpillar',
        model: '320 Excavator',
        licensePlate: null,
      }),
    ).toBe('Caterpillar 320 Excavator')
  })
})
