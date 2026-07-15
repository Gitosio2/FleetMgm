-- Symmetric to uq_active_assignment_per_driver (V4): a vehicle must also have at most one
-- active assignment (end_date IS NULL) at a time. The service-layer check in
-- AssignmentService.assign() is the primary guard; this is the DB-level backstop against a
-- race between two concurrent assign() calls for the same vehicle.
CREATE UNIQUE INDEX uq_active_assignment_per_vehicle
    ON driver_vehicle_assignments (vehicle_id)
    WHERE end_date IS NULL;
