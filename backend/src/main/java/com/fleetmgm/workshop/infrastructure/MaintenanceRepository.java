package com.fleetmgm.workshop.infrastructure;

import com.fleetmgm.workshop.domain.MaintenanceCategory;
import com.fleetmgm.workshop.domain.MaintenanceRecord;
import com.fleetmgm.workshop.domain.MaintenanceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public interface MaintenanceRepository extends JpaRepository<MaintenanceRecord, UUID> {

    // List query denormalizes vehicle/technician fields into MaintenanceResponse — JOIN FETCH
    // avoids N+1 (CLAUDE.md JPA rule). Safe with Pageable: these are to-one joins, not to-many collections.
    // vehicleId keeps its original bare "(:param IS NULL OR ...)" form untouched. workshopEntryDateFrom/
    // workshopEntryDateTo (also used by useVehicleMaintenanceHistory — replacing the old year/month
    // EXTRACT-based filter with an optional Desde/Hasta range, matching InvoiceRepository's issueDateFrom/
    // issueDateTo convention) and type/category/status/technicianId/costFrom/costTo (Órdenes de
    // mantenimiento filter bar) all use the "(CAST(:param AS string) IS NULL OR ...)" idiom — see
    // JobRepository.search()'s comment: that CAST is required whenever a param is wrapped in LOWER/CONCAT
    // (the :type LIKE clause here), and applied consistently across these params rather than mixing idioms.
    @Query("SELECT m FROM MaintenanceRecord m JOIN FETCH m.vehicle "
            + "LEFT JOIN FETCH m.technician "
            + "WHERE (:vehicleId IS NULL OR m.vehicle.id = :vehicleId) "
            + "AND (CAST(:workshopEntryDateFrom AS string) IS NULL OR m.workshopEntryDate >= :workshopEntryDateFrom) "
            + "AND (CAST(:workshopEntryDateTo AS string) IS NULL OR m.workshopEntryDate <= :workshopEntryDateTo) "
            + "AND (CAST(:type AS string) IS NULL OR LOWER(m.type) LIKE "
            + "     LOWER(CONCAT('%', CAST(:type AS string), '%'))) "
            + "AND (CAST(:category AS string) IS NULL OR m.category = :category) "
            + "AND (CAST(:status AS string) IS NULL OR m.status = :status) "
            + "AND (CAST(:technicianId AS string) IS NULL OR m.technician.id = :technicianId) "
            + "AND (CAST(:costFrom AS string) IS NULL OR m.cost >= :costFrom) "
            + "AND (CAST(:costTo AS string) IS NULL OR m.cost <= :costTo)")
    Page<MaintenanceRecord> findAllJoinFetch(
            @Param("vehicleId") UUID vehicleId,
            @Param("workshopEntryDateFrom") LocalDate workshopEntryDateFrom,
            @Param("workshopEntryDateTo") LocalDate workshopEntryDateTo,
            @Param("type") String type,
            @Param("category") MaintenanceCategory category,
            @Param("status") MaintenanceStatus status,
            @Param("technicianId") UUID technicianId,
            @Param("costFrom") BigDecimal costFrom,
            @Param("costTo") BigDecimal costTo,
            Pageable pageable);

    // Used by MaintenanceEventListener to decide whether a completed maintenance should return the
    // vehicle to ACTIVE — it must stay in the workshop while another maintenance is still IN_PROGRESS.
    boolean existsByVehicleIdAndStatus(UUID vehicleId, MaintenanceStatus status);

    // Fleet-summary KPI (dashboard) — monthly maintenance costs, summed alongside
    // SupplierInvoiceRepository.sumTotalByInvoiceDateBetween. COALESCE guards against SUM
    // returning null when no rows fall in the range.
    @Query("SELECT COALESCE(SUM(m.cost), 0) FROM MaintenanceRecord m WHERE m.workshopEntryDate BETWEEN :from AND :to")
    BigDecimal sumCostByWorkshopEntryDateBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);
}
