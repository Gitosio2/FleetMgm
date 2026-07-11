package com.fleetmgm.billing.infrastructure;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Interface-based projection for {@link ProfitabilityRepository#findProfitabilityByVehicle}.
 * Native queries cannot use JPQL {@code new com.foo.Bar(...)} constructor expressions, so Spring
 * Data's column-alias-to-getter-name convention is used instead — the native query's {@code AS}
 * aliases must match these getter names exactly (case-insensitive, underscores ignored).
 * <p>
 * {@code margin} is intentionally NOT part of this projection: it is derived math
 * ({@code revenue - costs}), and per this project's convention (see tax calculations in
 * {@code InvoiceService}), derived values are computed in the application layer, not in SQL.
 */
public interface VehicleProfitabilityProjection {

    UUID getVehicleId();

    String getVehicleLicensePlate();

    String getVehicleMake();

    String getVehicleModel();

    BigDecimal getRevenue();

    BigDecimal getCosts();
}
