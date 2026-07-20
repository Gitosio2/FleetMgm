package com.fleetmgm.billing.application;

import com.fleetmgm.billing.domain.InvoiceLineItem;
import com.fleetmgm.billing.dto.MonthlyFinancialResponse;
import com.fleetmgm.billing.dto.ProfitabilityResponse;
import com.fleetmgm.billing.dto.VehicleRevenueLineItemResponse;
import com.fleetmgm.billing.infrastructure.LineItemRepository;
import com.fleetmgm.billing.infrastructure.ProfitabilityRepository;
import com.fleetmgm.billing.infrastructure.VehicleProfitabilityProjection;
import com.fleetmgm.shared.PageResponse;
import com.fleetmgm.shared.exception.NotFoundException;
import com.fleetmgm.vehicle.domain.UsageMeasure;
import com.fleetmgm.vehicle.domain.Vehicle;
import com.fleetmgm.vehicle.infrastructure.UsageLogRepository;
import com.fleetmgm.vehicle.infrastructure.VehicleRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

@Service
public class ProfitabilityService {

    // Matches the Matriz de Permisos row "Informes de rentabilidad" (ADMIN/MANAGER/ADMINISTRATIVE
    // all checked) — mirrors InvoiceService.ROLES/SupplierInvoiceService.ROLES/PdfExportService.ROLES
    // verbatim. The Hito 34 checklist text says "solo ADMIN/MANAGER", but the Matriz is the actual
    // permission source of truth for this project and it disagrees with that abbreviation.
    private static final String ROLES = "hasAnyRole('ADMIN', 'MANAGER', 'ADMINISTRATIVE')";

    // Bounds for the Dashboard's monthly Ingresos/Gastos trend selector (3/6/12 months). Clamped
    // server-side rather than trusting the client-supplied `months` query param blindly — this
    // project's convention of never trusting external input (see CLAUDE.md's OWASP notes).
    private static final int MIN_MONTHS = 1;
    private static final int MAX_MONTHS = 12;

    private final ProfitabilityRepository profitabilityRepository;
    private final VehicleRepository vehicleRepository;
    private final LineItemRepository lineItemRepository;
    private final UsageLogRepository usageLogRepository;

    public ProfitabilityService(ProfitabilityRepository profitabilityRepository,
                                 VehicleRepository vehicleRepository,
                                 LineItemRepository lineItemRepository,
                                 UsageLogRepository usageLogRepository) {
        this.profitabilityRepository = profitabilityRepository;
        this.vehicleRepository = vehicleRepository;
        this.lineItemRepository = lineItemRepository;
        this.usageLogRepository = usageLogRepository;
    }

    @Transactional(readOnly = true)
    @PreAuthorize(ROLES)
    public PageResponse<ProfitabilityResponse> list(Pageable pageable) {
        return PageResponse.from(profitabilityRepository.findProfitabilityByVehicle(pageable)
                .map(this::toResponse));
    }

    // from/to are optional (VehicleProfitabilityPanel's Desde/Hasta range) — when both are null this
    // must produce byte-identical results to the previously all-time-only behavior, since
    // ProfitabilityRepository's CAST-to-date idiom short-circuits to "no bound" on a null param.
    @Transactional(readOnly = true)
    @PreAuthorize(ROLES)
    public ProfitabilityResponse getByVehicleId(UUID vehicleId, LocalDate from, LocalDate to) {
        VehicleProfitabilityProjection projection = profitabilityRepository
                .findProfitabilityByVehicleId(vehicleId, from, to)
                .orElseThrow(() -> new NotFoundException("VEHICLE_NOT_FOUND", "Vehicle " + vehicleId + " not found"));

        // Needed for usageMeasure (km vs hora label) and getCurrentUsageValue() (the unbounded-`to`
        // fallback below) — not loaded by the aggregate projection above.
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new NotFoundException("VEHICLE_NOT_FOUND", "Vehicle " + vehicleId + " not found"));

        Long usageInRange = computeUsageInRange(vehicle, from, to);
        BigDecimal margin = projection.getRevenue().subtract(projection.getCosts());
        BigDecimal costPerUsageUnit = (usageInRange != null && usageInRange > 0)
                ? projection.getCosts().divide(BigDecimal.valueOf(usageInRange), 2, RoundingMode.HALF_UP)
                : null;
        BigDecimal profitPerUsageUnit = (usageInRange != null && usageInRange > 0)
                ? margin.divide(BigDecimal.valueOf(usageInRange), 2, RoundingMode.HALF_UP)
                : null;

        return toResponse(projection, costPerUsageUnit, profitPerUsageUnit, vehicle.getUsageMeasure());
    }

    // usage_logs.value is a CUMULATIVE reading (not a delta), so the amount used within [from, to]
    // is (reading at end of period) - (reading at start of period). Deliberately returns null
    // (rather than guessing 0) whenever a true baseline/end reading can't be established — dividing
    // period cost by lifetime odometer would be misleading, and fabricating a "0 before" baseline
    // would be worse (see CLAUDE.md's OWASP section N — never silently substitute a default for
    // missing/uncertain data).
    private Long computeUsageInRange(Vehicle vehicle, LocalDate from, LocalDate to) {
        Long endValue = (to != null)
                ? usageLogRepository.findLatestValueUpToDate(vehicle.getId(), to).orElse(null)
                : vehicle.getCurrentUsageValue();
        if (endValue == null) {
            return null;
        }
        Long startValue = (from != null)
                ? usageLogRepository.findLatestValueBeforeDate(vehicle.getId(), from).orElse(null)
                : usageLogRepository.findEarliestValue(vehicle.getId()).orElse(null);
        if (startValue == null) {
            return null;
        }
        long delta = endValue - startValue;
        return delta >= 0 ? delta : null;
    }

    // Fleet-wide monthly Ingresos/Gastos trend for the Dashboard chart (Hito 43 redesign) — not
    // per-vehicle. `months` is clamped to [MIN_MONTHS, MAX_MONTHS] before being used to compute the
    // date range, so an out-of-range client value can't force an unbounded query.
    @Transactional(readOnly = true)
    @PreAuthorize(ROLES)
    public List<MonthlyFinancialResponse> getFinancialTrend(int months) {
        int clamped = Math.max(MIN_MONTHS, Math.min(MAX_MONTHS, months));
        YearMonth currentMonth = YearMonth.now();
        LocalDate to = currentMonth.atDay(1);
        LocalDate from = currentMonth.minusMonths(clamped - 1L).atDay(1);
        return profitabilityRepository.findMonthlyFinancialTrend(from, to).stream()
                .map(p -> new MonthlyFinancialResponse(p.getMonth(), p.getRevenue(), p.getCosts()))
                .toList();
    }

    // Historial de ingresos (Hito 44) — per-vehicle invoice line items for a given month/year,
    // backing the "Historial de ingresos" list in VehicleProfitabilityPanel. Existence check is
    // explicit here (unlike getByVehicleId, which relies on the aggregate projection being absent)
    // because an empty line-item list is also the correct result for a real vehicle with no revenue
    // in the given period — the two cases must be distinguishable.
    @Transactional(readOnly = true)
    @PreAuthorize(ROLES)
    public List<VehicleRevenueLineItemResponse> getRevenueByVehicle(UUID vehicleId, LocalDate from, LocalDate to) {
        vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new NotFoundException("VEHICLE_NOT_FOUND", "Vehicle " + vehicleId + " not found"));
        return lineItemRepository.findAllByVehicleIdAndPeriod(vehicleId, from, to).stream()
                .map(this::toRevenueResponse)
                .toList();
    }

    private VehicleRevenueLineItemResponse toRevenueResponse(InvoiceLineItem lineItem) {
        return new VehicleRevenueLineItemResponse(
                lineItem.getInvoice().getInvoiceNumber(),
                lineItem.getInvoice().getIssueDate(),
                lineItem.getDescription(),
                lineItem.getQuantity(),
                lineItem.getUnitPrice(),
                lineItem.getSubtotal()
        );
    }

    // margin is derived math (revenue - costs) — computed here in the application layer, not in
    // SQL, mirroring how InvoiceService keeps tax/total calculations out of the database.
    // Used by list() (fleet-wide paged table): costPerUsageUnit/profitPerUsageUnit/usageMeasure are
    // always null there — computing a per-vehicle usage-log query for every row of a page would be
    // an N+1 query pattern, and this feature only applies to the single-vehicle detail panel.
    private ProfitabilityResponse toResponse(VehicleProfitabilityProjection projection) {
        return toResponse(projection, null, null, null);
    }

    // Used by getByVehicleId (single-vehicle detail panel), where costPerUsageUnit/
    // profitPerUsageUnit/usageMeasure are already computed via computeUsageInRange.
    private ProfitabilityResponse toResponse(VehicleProfitabilityProjection projection,
                                              BigDecimal costPerUsageUnit,
                                              BigDecimal profitPerUsageUnit,
                                              UsageMeasure usageMeasure) {
        return new ProfitabilityResponse(
                projection.getVehicleId(),
                projection.getVehicleLicensePlate(),
                projection.getVehicleMake(),
                projection.getVehicleModel(),
                projection.getRevenue(),
                projection.getCosts(),
                projection.getRevenue().subtract(projection.getCosts()),
                costPerUsageUnit,
                profitPerUsageUnit,
                usageMeasure
        );
    }
}
