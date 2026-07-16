package com.fleetmgm.billing.infrastructure;

import com.fleetmgm.billing.domain.Invoice;
import com.fleetmgm.billing.domain.InvoiceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {

    // List query denormalizes client fields into InvoiceResponse — JOIN FETCH avoids N+1
    // (CLAUDE.md JPA rule). Safe with Pageable: this is a to-one join, not a to-many collection.
    // Not-yet-PAID invoices (DRAFT/ISSUED) sort before PAID, newest first within each group —
    // createdAt is the tie-break (not issueDate/dueDate) since DRAFT has neither populated yet.
    // Callers must pass an unsorted Pageable — this ORDER BY is the whole point of the query.
    @Query("SELECT i FROM Invoice i JOIN FETCH i.client "
            + "ORDER BY CASE WHEN i.status = com.fleetmgm.billing.domain.InvoiceStatus.PAID THEN 1 ELSE 0 END, "
            + "i.createdAt DESC")
    Page<Invoice> findAllJoinFetch(Pageable pageable);

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

    // Backs InvoiceNumberGenerator's INV-<year>-<00001> numbering scheme. A native scalar query
    // is the simplest correct option here — no EntityManager/JdbcTemplate precedent exists
    // elsewhere in this codebase, and every other repository in this project is a plain
    // JpaRepository, so keeping the sequence pull inside the entity's own repository stays
    // consistent with that pattern instead of introducing a new access mechanism.
    @Query(value = "SELECT nextval('invoice_number_seq')", nativeQuery = true)
    long nextInvoiceNumberSequenceValue();
}
