package com.fleetmgm.dashboard.dto;

import java.math.BigDecimal;
import java.util.List;

public record FinancialSummaryResponse(
        BigDecimal monthlyCosts,
        BigDecimal monthlyRevenue,
        BigDecimal previousMonthMargin,
        List<UpcomingInvoiceResponse> upcomingReceivables,
        List<UpcomingInvoiceResponse> upcomingPayables
) {}
