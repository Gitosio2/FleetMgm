-- Distinguishes preventive (planned) from corrective (reactive/breakdown) maintenance,
-- so cost/profitability reports can slice by nature of the work, not just by vehicle.
ALTER TABLE maintenance_records ADD COLUMN category VARCHAR(10) NOT NULL DEFAULT 'PREVENTIVE';
