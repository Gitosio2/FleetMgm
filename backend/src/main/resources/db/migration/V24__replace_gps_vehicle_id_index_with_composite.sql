-- GpsRepository.findLatestForAllActiveVehicles() now looks up each vehicle's latest position via
-- a LATERAL "WHERE vehicle_id = ? ORDER BY recorded_at DESC LIMIT 1" per active vehicle. A composite
-- index is what makes that a single index probe per vehicle instead of a scan through that vehicle's
-- entire history: measured locally at ~0.2ms for a 28-vehicle fleet with ~20k rows/vehicle (the
-- volume ~7 days of retention leaves in place), versus ~1.6s for the previous correlated-subquery
-- query even with idx_gps_vehicle_id already in place.
--
-- idx_gps_vehicle_id (vehicle_id alone) is dropped, not kept alongside: the leftmost-prefix rule
-- means this composite index already serves any query that only needs to filter by vehicle_id, and
-- gps_positions takes a high insert rate (one row per active vehicle every 30s) — every extra index
-- is extra write cost on every one of those inserts, so a redundant one is not free to leave in place.
DROP INDEX idx_gps_vehicle_id;

CREATE INDEX idx_gps_vehicle_recorded ON gps_positions (vehicle_id, recorded_at DESC);
