-- Safety net for concurrent worker creates with the same nationalId.
-- The service-level existsByNationalId check handles the common case with a user-friendly
-- message; this index enforces uniqueness atomically and prevents the race condition.
-- Partial index: excludes soft-deleted records so a deleted worker's nationalId can be reused.
CREATE UNIQUE INDEX uq_workers_national_id_active
    ON workers (national_id)
    WHERE deleted_at IS NULL;
