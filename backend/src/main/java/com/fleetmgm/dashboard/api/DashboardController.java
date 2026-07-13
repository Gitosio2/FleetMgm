package com.fleetmgm.dashboard.api;

import com.fleetmgm.dashboard.application.DashboardService;
import com.fleetmgm.dashboard.dto.FinancialSummaryResponse;
import com.fleetmgm.dashboard.dto.FleetSummaryResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reports")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/fleet-summary")
    public ResponseEntity<FleetSummaryResponse> fleetSummary() {
        return ResponseEntity.ok(dashboardService.getFleetSummary());
    }

    @GetMapping("/financial-summary")
    public ResponseEntity<FinancialSummaryResponse> financialSummary() {
        return ResponseEntity.ok(dashboardService.getFinancialSummary());
    }
}
