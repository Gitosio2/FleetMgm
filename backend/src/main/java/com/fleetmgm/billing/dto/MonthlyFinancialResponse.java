package com.fleetmgm.billing.dto;

import java.math.BigDecimal;

public record MonthlyFinancialResponse(String month, BigDecimal revenue, BigDecimal costs) {}
