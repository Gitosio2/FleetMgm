package com.fleetmgm.billing.infrastructure;

import com.fleetmgm.billing.domain.Invoice;
import com.fleetmgm.billing.domain.InvoiceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {

    // List query denormalizes client fields into InvoiceResponse — JOIN FETCH avoids N+1
    // (CLAUDE.md JPA rule). Safe with Pageable: this is a to-one join, not a to-many collection.
    @Query("SELECT i FROM Invoice i JOIN FETCH i.client")
    Page<Invoice> findAllJoinFetch(Pageable pageable);

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
