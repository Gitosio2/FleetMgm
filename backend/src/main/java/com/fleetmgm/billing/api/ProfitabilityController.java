package com.fleetmgm.billing.api;

import com.fleetmgm.billing.application.ProfitabilityService;
import com.fleetmgm.billing.dto.MonthlyFinancialResponse;
import com.fleetmgm.billing.dto.ProfitabilityResponse;
import com.fleetmgm.billing.dto.VehicleExpenseResponse;
import com.fleetmgm.billing.dto.VehicleRevenueLineItemResponse;
import com.fleetmgm.shared.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reports/profitability")
public class ProfitabilityController {

    private final ProfitabilityService profitabilityService;

    public ProfitabilityController(ProfitabilityService profitabilityService) {
        this.profitabilityService = profitabilityService;
    }

    // No default sort field: results aren't tied to a timestamp column (they're a computed
    // aggregate view, not an entity with createdAt), so `size` default is enough.
    @GetMapping
    public ResponseEntity<PageResponse<ProfitabilityResponse>> list(@PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(profitabilityService.list(pageable));
    }

    // from/to are optional (VehicleProfitabilityPanel's Desde/Hasta range, replacing the old
    // month/year selector) — unset means full history, matching this endpoint's original behavior.
    @GetMapping("/{vehicleId}")
    public ResponseEntity<ProfitabilityResponse> getByVehicleId(
            @PathVariable UUID vehicleId,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to) {
        return ResponseEntity.ok(profitabilityService.getByVehicleId(vehicleId, from, to));
    }

    // Historial de ingresos (Hito 44) — declared alongside /trend as a static-looking sub-path of
    // /{vehicleId}; Spring MVC resolves "/revenue" as a literal segment rather than a second
    // path-variable capture, same precedent as /trend above.
    @GetMapping("/{vehicleId}/revenue")
    public ResponseEntity<List<VehicleRevenueLineItemResponse>> getRevenueByVehicle(
            @PathVariable UUID vehicleId,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to) {
        return ResponseEntity.ok(profitabilityService.getRevenueByVehicle(vehicleId, from, to));
    }

    // Vehicle profitability panel's merged "Historial de gastos" list (Hito 45) — declared alongside
    // /revenue as a static-looking sub-path of /{vehicleId}, same precedent as /revenue and /trend
    // above (Spring MVC resolves "/expenses" as a literal segment, not a second path variable).
    @GetMapping("/{vehicleId}/expenses")
    public ResponseEntity<List<VehicleExpenseResponse>> getExpensesByVehicle(
            @PathVariable UUID vehicleId,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to) {
        return ResponseEntity.ok(profitabilityService.getExpensesByVehicle(vehicleId, from, to));
    }

    // Fleet-wide monthly Ingresos/Gastos trend backing the Dashboard chart (Hito 43 redesign).
    // Declared after /{vehicleId} in source but Spring MVC still matches this static segment
    // ahead of the path-variable mapping, so "/trend" never gets captured as a vehicleId.
    @GetMapping("/trend")
    public ResponseEntity<List<MonthlyFinancialResponse>> financialTrend(
            @RequestParam(defaultValue = "6") int months) {
        return ResponseEntity.ok(profitabilityService.getFinancialTrend(months));
    }
}
