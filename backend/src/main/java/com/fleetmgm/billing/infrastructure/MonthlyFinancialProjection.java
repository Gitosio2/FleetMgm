package com.fleetmgm.billing.infrastructure;

import java.math.BigDecimal;

/**
 * Interface-based projection for {@link ProfitabilityRepository#findMonthlyFinancialTrend}.
 * Same alias-to-getter convention as {@link VehicleProfitabilityProjection}: the native query's
 * {@code AS} aliases must match these getter names exactly (case-insensitive, underscores
 * ignored).
 */
public interface MonthlyFinancialProjection {

    String getMonth();

    BigDecimal getRevenue();

    BigDecimal getCosts();
}
