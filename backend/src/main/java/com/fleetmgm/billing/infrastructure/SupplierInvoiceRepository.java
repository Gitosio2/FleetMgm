package com.fleetmgm.billing.infrastructure;

import com.fleetmgm.billing.domain.ExpenseCategory;
import com.fleetmgm.billing.domain.SupplierInvoice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface SupplierInvoiceRepository extends JpaRepository<SupplierInvoice, UUID> {

    // List query denormalizes vehicle and supplier fields into SupplierInvoiceResponse — JOIN FETCH
    // avoids N+1 (CLAUDE.md JPA rule). vehicle is nullable (LEFT join); supplier is a mandatory
    // relation like Invoice.client, so it uses a plain JOIN FETCH. Both filters are optional — the
    // standard "(:param IS NULL OR ...)" JPQL idiom keeps the query fully parameterized (no string
    // concatenation), satisfying the dynamic-query SQL injection rule while still supporting
    // Pageable on a to-one join.
    @Query("SELECT si FROM SupplierInvoice si JOIN FETCH si.supplier LEFT JOIN FETCH si.vehicle "
            + "WHERE (:vehicleId IS NULL OR si.vehicle.id = :vehicleId) "
            + "AND (:category IS NULL OR si.category = :category)")
    Page<SupplierInvoice> findAllJoinFetch(
            @Param("vehicleId") UUID vehicleId, @Param("category") ExpenseCategory category, Pageable pageable);
}
