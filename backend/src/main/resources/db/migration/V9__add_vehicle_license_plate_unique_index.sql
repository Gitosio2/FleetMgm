-- Safety net for concurrent vehicle creates/updates with the same license plate.
-- The service-level existsByLicensePlate check handles the common case with a user-friendly
-- message; this index enforces uniqueness atomically and prevents the race condition.
-- Partial index: excludes null plates (heavy machinery) and soft-deleted records.
CREATE UNIQUE INDEX uq_vehicles_license_plate_active
    ON vehicles (license_plate)
    WHERE license_plate IS NOT NULL AND deleted_at IS NULL;
