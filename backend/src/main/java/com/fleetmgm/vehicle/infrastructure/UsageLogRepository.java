package com.fleetmgm.vehicle.infrastructure;

import com.fleetmgm.vehicle.domain.UsageLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UsageLogRepository extends JpaRepository<UsageLog, UUID> {
}
