package com.fleetmgm.supplier.infrastructure;

import com.fleetmgm.supplier.domain.Supplier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface SupplierRepository extends JpaRepository<Supplier, UUID> {

    boolean existsByTaxId(String taxId);

    boolean existsByTaxIdAndIdNot(String taxId, UUID id);

    // Both filters are optional and case-insensitive partial matches — the standard
    // "(CAST(:param AS string) IS NULL OR ...)" JPQL idiom keeps the query fully parameterized
    // (no string concatenation, satisfying the dynamic-query SQL injection rule) while still
    // supporting Pageable. No ORDER BY here (unlike the billing repositories' findAllJoinFetch
    // methods): this list has no DRAFT/pagination-tiebreak concerns, so Spring Data applies the
    // Pageable's own Sort automatically (the controller passes @PageableDefault(sort = "name")).
    // Each of :name/:taxId needs BOTH occurrences cast, not just the IS NULL one — see
    // SupplierInvoiceRepository.findAllJoinFetch's comment on the identical supplierInvoiceNumber
    // filter: leaving the CONCAT occurrence uncast fails against real Postgres ("function
    // lower(bytea) does not exist"), because each JPQL "?" placeholder binds independently and
    // pgjdbc binds an untyped null parameter to bytea by default.
    @Query("SELECT s FROM Supplier s "
            + "WHERE (CAST(:name AS string) IS NULL OR LOWER(s.name) LIKE "
            + "     LOWER(CONCAT('%', CAST(:name AS string), '%'))) "
            + "AND (CAST(:taxId AS string) IS NULL OR LOWER(s.taxId) LIKE "
            + "     LOWER(CONCAT('%', CAST(:taxId AS string), '%')))")
    Page<Supplier> search(@Param("name") String name, @Param("taxId") String taxId, Pageable pageable);
}
