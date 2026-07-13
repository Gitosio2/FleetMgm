package com.fleetmgm.dashboard.dto;

public record FleetSummaryResponse(
        long activeVehicles,
        long totalVehicles,
        long inWorkshop,
        long pendingMaintenance,
        long pendingMaintenanceDueSoon
) {}
