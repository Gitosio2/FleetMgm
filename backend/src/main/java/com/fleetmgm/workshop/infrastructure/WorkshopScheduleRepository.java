package com.fleetmgm.workshop.infrastructure;

import com.fleetmgm.workshop.domain.WorkshopSchedule;
import com.fleetmgm.workshop.domain.WorkshopStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface WorkshopScheduleRepository extends JpaRepository<WorkshopSchedule, UUID> {

    // List query denormalizes vehicle/technician/maintenance fields into ScheduleResponse — JOIN FETCH
    // avoids N+1 (CLAUDE.md JPA rule). Safe with Pageable: these are to-one joins, not to-many collections.
    // Date-range boundaries (today/week/month) are computed by the service and passed as bound parameters.
    @Query("SELECT ws FROM WorkshopSchedule ws JOIN FETCH ws.vehicle "
            + "LEFT JOIN FETCH ws.technician LEFT JOIN FETCH ws.maintenanceRecord "
            + "WHERE ws.scheduledDate BETWEEN :from AND :to")
    Page<WorkshopSchedule> findAllByScheduledDateBetween(
            @Param("from") LocalDate from, @Param("to") LocalDate to, Pageable pageable);

    // Used by ScheduleCompletionListener to find the schedule (if any) linked to a completed maintenance.
    Optional<WorkshopSchedule> findByMaintenanceRecordId(UUID maintenanceRecordId);

    // Fleet-summary KPIs (dashboard). Plain derived counts — no JOIN FETCH needed here since only
    // the row count is returned, unlike findAllByScheduledDateBetween above which hydrates entities.
    long countByStatus(WorkshopStatus status);

    long countByStatusAndScheduledDateBetween(WorkshopStatus status, LocalDate from, LocalDate to);
}
