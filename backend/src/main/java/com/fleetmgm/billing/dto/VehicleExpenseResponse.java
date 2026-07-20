package com.fleetmgm.billing.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record VehicleExpenseResponse(String description, LocalDate date, BigDecimal amount) {}
