package com.fleetmgm.workshop.dto;

import java.math.BigDecimal;

public record CompleteMaintenanceRequest(
        BigDecimal cost
) {}
