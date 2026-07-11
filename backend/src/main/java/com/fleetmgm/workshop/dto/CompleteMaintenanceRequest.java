package com.fleetmgm.workshop.dto;

import java.math.BigDecimal;
import java.time.LocalTime;

public record CompleteMaintenanceRequest(
        BigDecimal cost,
        LocalTime exitTime
) {}
