package com.fleetmgm.billing.infrastructure;

import com.fleetmgm.billing.domain.Invoice;
import com.fleetmgm.billing.domain.InvoiceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {

    // List query denormalizes client fields into InvoiceResponse — JOIN FETCH avoids N+1
    // (CLAUDE.md JPA rule). Safe with Pageable: this is a to-one join, not a to-many collection.
    // Pure chronological order, newest first — status does not influence sort (matches the
    // list-view convention of QuickBooks/Xero/Stripe Invoicing: status is a filterable
    // column, not a sort bucket). createdAt is used instead of issueDate/dueDate since DRAFT
    // has neither populated yet. Callers must pass an unsorted Pageable — this ORDER BY is the
    // whole point of the query.
    // Filters mirror SupplierInvoiceRepository.findAllJoinFetch(): clientId follows the bare
    // ":param IS NULL" idiom (precedent: that method's vehicleId, also a UUID, uses the same bare
    // form and works). status/the six date bounds/the two total bounds use
    // "CAST(:param AS string) IS NULL" instead — see AuditLogRepository's comment on that idiom:
    // a parameter that appears ONLY in a bare IS NULL check gives Postgres no type context to
    // infer from, even for enums, and fails 500 on every request that leaves it null. invoiceNumber
    // was tried with the bare form first on the theory that its second appearance in the
    // LIKE/CONCAT clause would give Postgres a String type to infer from — but that failed
    // empirically (InvoiceRepositoryTest, real Postgres via Testcontainers): "function lower(bytea)
    // does not exist". Casting only the IS NULL occurrence wasn't enough either, and failed with the
    // same error — each JPQL "?" placeholder is bound independently, so the CONCAT occurrence needs
    // its own CAST too; pgjdbc binds an untyped null parameter to bytea by default. Both occurrences
    // of :invoiceNumber below are cast for that reason.
    //
    // totalMin/totalMax compare against an "effective total" CASE expression, not i.total directly:
    // a DRAFT invoice's subtotal/taxAmount/total stay 0 until issue() computes them, but the
    // frontend (InvoiceTable/invoice-shared.ts's displayInvoiceTotal) shows a preview total for
    // DRAFT rows computed from their line items — filtering against the raw i.total would silently
    // exclude a DRAFT invoice the user can see visibly satisfies the range they typed. The
    // correlated subquery mirrors that same preview computation so the filter agrees with what's
    // displayed. Repeated once per bound (JPQL/Hibernate has no local CTE/WITH to factor it into)
    // — same repetition tradeoff already accepted elsewhere in this query.
    @Query("SELECT i FROM Invoice i JOIN FETCH i.client "
            + "WHERE (:clientId IS NULL OR i.client.id = :clientId) "
            + "AND (CAST(:invoiceNumber AS string) IS NULL OR LOWER(i.invoiceNumber) LIKE "
            + "     LOWER(CONCAT('%', CAST(:invoiceNumber AS string), '%'))) "
            + "AND (CAST(:status AS string) IS NULL OR i.status = :status) "
            + "AND (CAST(:issueDateFrom AS string) IS NULL OR i.issueDate >= :issueDateFrom) "
            + "AND (CAST(:issueDateTo AS string) IS NULL OR i.issueDate <= :issueDateTo) "
            + "AND (CAST(:dueDateFrom AS string) IS NULL OR i.dueDate >= :dueDateFrom) "
            + "AND (CAST(:dueDateTo AS string) IS NULL OR i.dueDate <= :dueDateTo) "
            + "AND (CAST(:paymentDateFrom AS string) IS NULL OR i.paymentDate >= :paymentDateFrom) "
            + "AND (CAST(:paymentDateTo AS string) IS NULL OR i.paymentDate <= :paymentDateTo) "
            + "AND (CAST(:totalMin AS string) IS NULL OR "
            + "     (CASE WHEN i.status = com.fleetmgm.billing.domain.InvoiceStatus.DRAFT "
            + "       THEN (SELECT COALESCE(SUM(li.subtotal), 0) FROM InvoiceLineItem li WHERE li.invoice = i) "
            + "            * (1 + i.taxRate) "
            + "       ELSE i.total END) >= :totalMin) "
            + "AND (CAST(:totalMax AS string) IS NULL OR "
            + "     (CASE WHEN i.status = com.fleetmgm.billing.domain.InvoiceStatus.DRAFT "
            + "       THEN (SELECT COALESCE(SUM(li.subtotal), 0) FROM InvoiceLineItem li WHERE li.invoice = i) "
            + "            * (1 + i.taxRate) "
            + "       ELSE i.total END) <= :totalMax) "
            + "ORDER BY i.createdAt DESC")
    Page<Invoice> findAllJoinFetch(
            @Param("clientId") UUID clientId,
            @Param("invoiceNumber") String invoiceNumber,
            @Param("status") InvoiceStatus status,
            @Param("issueDateFrom") LocalDate issueDateFrom,
            @Param("issueDateTo") LocalDate issueDateTo,
            @Param("dueDateFrom") LocalDate dueDateFrom,
            @Param("dueDateTo") LocalDate dueDateTo,
            @Param("paymentDateFrom") LocalDate paymentDateFrom,
            @Param("paymentDateTo") LocalDate paymentDateTo,
            @Param("totalMin") BigDecimal totalMin,
            @Param("totalMax") BigDecimal totalMax,
            Pageable pageable);

    // Financial-summary KPI (dashboard) — top-N unpaid client invoices due soon. No lower bound on
    // dueDate: already-overdue invoices (dueDate in the past) must be included too, not filtered
    // out — the frontend flags them separately (see DashboardService.getFinancialSummary()). This
    // mirrors InvoiceStatus.OVERDUE being dead in this codebase (nothing ever transitions to it);
    // "overdue" is computed live from dueDate, never read from status.
    @Query("SELECT i FROM Invoice i JOIN FETCH i.client "
            + "WHERE i.status = com.fleetmgm.billing.domain.InvoiceStatus.ISSUED AND i.dueDate <= :to "
            + "ORDER BY i.dueDate ASC")
    List<Invoice> findUpcomingReceivables(@Param("to") LocalDate to, Pageable pageable);

    // Used by InvoiceJobCompletionListener to find the client's existing open DRAFT invoice to
    // append a line item to, instead of creating a new invoice per completed job. Oldest-first
    // (createdAt ASC) is an arbitrary but deterministic tie-break when more than one exists.
    Optional<Invoice> findFirstByClientIdAndStatusOrderByCreatedAtAsc(UUID clientId, InvoiceStatus status);

    // Dashboard "Cobros del mes" card — cash actually collected this month, as opposed to
    // monthlyRevenue (accrued by issueDate, any status). Deliberately subtotal (not total, which
    // includes IVA) so it's directly comparable to monthlyRevenue on the same tax-exclusive basis.
    @Query("SELECT COALESCE(SUM(i.subtotal), 0) FROM Invoice i "
            + "WHERE i.status = com.fleetmgm.billing.domain.InvoiceStatus.PAID "
            + "AND i.paymentDate BETWEEN :from AND :to")
    BigDecimal sumSubtotalByPaymentDateBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);

    // Backs InvoiceNumberGenerator's INV-<year>-<00001> numbering scheme. A native scalar query
    // is the simplest correct option here — no EntityManager/JdbcTemplate precedent exists
    // elsewhere in this codebase, and every other repository in this project is a plain
    // JpaRepository, so keeping the sequence pull inside the entity's own repository stays
    // consistent with that pattern instead of introducing a new access mechanism.
    @Query(value = "SELECT nextval('invoice_number_seq')", nativeQuery = true)
    long nextInvoiceNumberSequenceValue();
}
