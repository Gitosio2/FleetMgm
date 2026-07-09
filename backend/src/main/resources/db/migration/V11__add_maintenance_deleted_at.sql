-- maintenance_records shipped in V6 without deleted_at; add it so the entity can
-- adopt the standard @SQLRestriction("deleted_at IS NULL") soft-delete pattern
-- used by every other entity in the project.
ALTER TABLE maintenance_records ADD COLUMN deleted_at TIMESTAMPTZ;
