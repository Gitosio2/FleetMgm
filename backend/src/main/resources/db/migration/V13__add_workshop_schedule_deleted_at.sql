-- workshop_schedules shipped in V6 without deleted_at; add it so the entity can
-- adopt the standard @SQLRestriction("deleted_at IS NULL") soft-delete pattern
-- used by every other entity in the project (same rationale as V11 for maintenance_records).
ALTER TABLE workshop_schedules ADD COLUMN deleted_at TIMESTAMPTZ;
