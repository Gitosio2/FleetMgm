package com.fleetmgm.worker.infrastructure;

import com.fleetmgm.worker.domain.Worker;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface WorkerRepository extends JpaRepository<Worker, UUID> {

    boolean existsByNationalId(String nationalId);

    boolean existsByNationalIdAndIdNot(String nationalId, UUID id);

    Optional<Worker> findByUserId(UUID userId);
}
