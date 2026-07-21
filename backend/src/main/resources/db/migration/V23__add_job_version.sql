-- Optimistic locking column for jobs.status transitions (start/complete/cancel). Without it, two
-- concurrent requests against the same job (double-click, duplicate retry) can both pass the status
-- guard in JobService and each commit, double-firing JobCompletedEvent (duplicate usage log +
-- duplicate invoice line item).
ALTER TABLE jobs ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
