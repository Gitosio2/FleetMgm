package com.fleetmgm.worker.infrastructure;

import com.fleetmgm.worker.domain.Worker;
import com.fleetmgm.worker.domain.WorkerRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface WorkerRepository extends JpaRepository<Worker, UUID> {

    boolean existsByNationalId(String nationalId);

    boolean existsByNationalIdAndIdNot(String nationalId, UUID id);

    Optional<Worker> findByUserId(UUID userId);

    // All three filters are optional and case-insensitive partial matches — same
    // "(CAST(:param AS string) IS NULL OR ...)" JPQL idiom as SupplierRepository.search(). No
    // ORDER BY here: Spring Data applies the Pageable's own Sort automatically (the controller
    // passes @PageableDefault(sort = "lastName")). There's no single "name" column — firstName and
    // lastName are concatenated the same way Worker.getFullName() does, so the filter matches what
    // the UI actually displays as a combined name.
    // :name/:nationalId need BOTH occurrences cast, not just the IS NULL one — see
    // SupplierRepository.search()'s comment: leaving the CONCAT occurrence uncast fails against
    // real Postgres ("function lower(bytea) does not exist"), because each JPQL "?" placeholder
    // binds independently and pgjdbc binds an untyped null parameter to bytea by default.
    // :workerRole is an enum but gets the same CAST(:param AS string) IS NULL treatment as the
    // string params, not a bare ":workerRole IS NULL" — see AuditLogRepository's comment on the
    // same idiom: a parameter that appears ONLY in a bare IS NULL check gives Postgres no type
    // context to infer from, even for enums, and fails 500 on every request that leaves it null.
    @Query("SELECT w FROM Worker w "
            + "WHERE (CAST(:name AS string) IS NULL OR LOWER(CONCAT(w.firstName, ' ', w.lastName)) LIKE "
            + "     LOWER(CONCAT('%', CAST(:name AS string), '%'))) "
            + "AND (CAST(:nationalId AS string) IS NULL OR LOWER(w.nationalId) LIKE "
            + "     LOWER(CONCAT('%', CAST(:nationalId AS string), '%'))) "
            + "AND (CAST(:workerRole AS string) IS NULL OR w.workerRole = :workerRole)")
    Page<Worker> search(@Param("name") String name, @Param("nationalId") String nationalId,
            @Param("workerRole") WorkerRole workerRole, Pageable pageable);
}
