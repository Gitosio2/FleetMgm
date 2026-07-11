package com.fleetmgm.billing.application;

import com.fleetmgm.billing.dto.ProfitabilityResponse;
import com.fleetmgm.billing.infrastructure.ProfitabilityRepository;
import com.fleetmgm.billing.infrastructure.VehicleProfitabilityProjection;
import com.fleetmgm.shared.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProfitabilityService {

    // Matches the Matriz de Permisos row "Informes de rentabilidad" (ADMIN/MANAGER/ADMINISTRATIVE
    // all checked) — mirrors InvoiceService.ROLES/SupplierInvoiceService.ROLES/PdfExportService.ROLES
    // verbatim. The Hito 34 checklist text says "solo ADMIN/MANAGER", but the Matriz is the actual
    // permission source of truth for this project and it disagrees with that abbreviation.
    private static final String ROLES = "hasAnyRole('ADMIN', 'MANAGER', 'ADMINISTRATIVE')";

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
