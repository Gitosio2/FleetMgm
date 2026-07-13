package com.fleetmgm.billing.api;

import com.fleetmgm.billing.application.ProfitabilityService;
import com.fleetmgm.billing.dto.ProfitabilityResponse;
import com.fleetmgm.shared.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

    @GetMapping("/{vehicleId}")
    public ResponseEntity<ProfitabilityResponse> getByVehicleId(@PathVariable UUID vehicleId) {
        return ResponseEntity.ok(profitabilityService.getByVehicleId(vehicleId));
    }
}
