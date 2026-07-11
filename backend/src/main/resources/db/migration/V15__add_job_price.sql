-- Adds a nullable price/rate column to jobs so a completed job can carry enough information
-- for the billing feature (Hito 31) to auto-generate an invoice line item. Nullable by design:
-- a Job with no clientId bills nothing, and a Job with a clientId but no price is an accepted
-- data gap that the JobCompletedEvent consumer treats as a no-op, not a blocking error.
ALTER TABLE jobs ADD COLUMN price NUMERIC(12,2);
