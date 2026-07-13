package com.fleetmgm.billing.application;

import com.fleetmgm.billing.dto.MonthlyFinancialResponse;
import com.fleetmgm.billing.dto.ProfitabilityResponse;
import com.fleetmgm.billing.infrastructure.ProfitabilityRepository;
import com.fleetmgm.billing.infrastructure.VehicleProfitabilityProjection;
import com.fleetmgm.shared.PageResponse;
import com.fleetmgm.shared.exception.NotFoundException;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    public ProfitabilityService(ProfitabilityRepository profitabilityRepository) {
        this.profitabilityRepository = profitabilityRepository;
    }

    @Transactional(readOnly = true)
    @PreAuthorize(ROLES)
    public PageResponse<ProfitabilityResponse> list(Pageable pageable) {
        return PageResponse.from(profitabilityRepository.findProfitabilityByVehicle(pageable)
                .map(this::toResponse));
    }

    @Transactional(readOnly = true)
    @PreAuthorize(ROLES)
    public ProfitabilityResponse getByVehicleId(UUID vehicleId) {
        VehicleProfitabilityProjection projection = profitabilityRepository.findProfitabilityByVehicleId(vehicleId)
                .orElseThrow(() -> new NotFoundException("VEHICLE_NOT_FOUND", "Vehicle " + vehicleId + " not found"));
        return toResponse(projection);
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

    // margin is derived math (revenue - costs) — computed here in the application layer, not in
    // SQL, mirroring how InvoiceService keeps tax/total calculations out of the database.
    private ProfitabilityResponse toResponse(VehicleProfitabilityProjection projection) {
        return new ProfitabilityResponse(
                projection.getVehicleId(),
                projection.getVehicleLicensePlate(),
                projection.getVehicleMake(),
                projection.getVehicleModel(),
                projection.getRevenue(),
                projection.getCosts(),
                projection.getRevenue().subtract(projection.getCosts())
        );
    }
}
