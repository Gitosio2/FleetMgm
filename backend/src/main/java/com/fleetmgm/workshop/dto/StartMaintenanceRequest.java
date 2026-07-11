package com.fleetmgm.workshop.dto;

import java.time.LocalTime;

public record StartMaintenanceRequest(
        Long usageAtService,
        LocalTime entryTime
) {}
