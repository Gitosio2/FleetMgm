-- Adds free-form (non-slot) time-of-day columns so the workshop agenda can be ordered by
-- hour, not just by date: scheduled_start_time/scheduled_end_time are the PLANNED slot on
-- workshop_schedules, workshop_entry_time/workshop_exit_time are the ACTUAL time-of-day
-- complement to the existing workshop_entry_date/workshop_exit_date on maintenance_records.
-- All nullable — a record with no time set remains fully valid, same as scheduled_date-only
-- records today. No overlap constraint: explicitly out of scope for this milestone.
ALTER TABLE workshop_schedules ADD COLUMN scheduled_start_time TIME;
ALTER TABLE workshop_schedules ADD COLUMN scheduled_end_time TIME;
ALTER TABLE maintenance_records ADD COLUMN workshop_entry_time TIME;
ALTER TABLE maintenance_records ADD COLUMN workshop_exit_time TIME;
