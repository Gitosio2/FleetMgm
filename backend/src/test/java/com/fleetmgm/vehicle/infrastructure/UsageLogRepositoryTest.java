package com.fleetmgm.vehicle.infrastructure;

import com.fleetmgm.config.AuditorAwareImpl;
import com.fleetmgm.config.JpaAuditingConfig;
import com.fleetmgm.vehicle.domain.UsageLog;
import com.fleetmgm.vehicle.domain.UsageMeasure;
import com.fleetmgm.vehicle.domain.UsageSource;
import com.fleetmgm.vehicle.domain.Vehicle;
import com.fleetmgm.vehicle.domain.VehicleCategory;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE;

/**
 * Proves the "cumulative reading, not a delta" idiom {@link ProfitabilityServiceTest} and
 * {@link com.fleetmgm.billing.application.ProfitabilityService} rely on: given a vehicle's
 * {@code usage_logs} history, find the last known reading strictly before a date
 * ({@link #findLatestValueBeforeDate_returnsLastReading_beforeFromDate}), the last known reading
 * at/before the END of a date ({@link #findLatestValueUpToDate_includesEntireToDay_untilMidnight}),
 * and the earliest reading ever recorded ({@link #findEarliestValue_returnsFirstEverReading}).
 */
@Tag("integration")
@DataJpaTest
@AutoConfigureTestDatabase(replace = NONE)
@Testcontainers
@Import({JpaAuditingConfig.class, AuditorAwareImpl.class})
class UsageLogRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    UsageLogRepository usageLogRepository;

    @Autowired
    TestEntityManager entityManager;

    // --- findLatestValueUpToDate: the day-boundary case that matters most ---

    @Test
    void findLatestValueUpToDate_includesEntireToDay_untilMidnight() {
        Vehicle vehicle = persistVehicle("1111AAA");
        LocalDate to = LocalDate.of(2026, 6, 30);
        // Recorded at 23:59 on `to`'s date — must be included, since `to` is meant as an inclusive
        // whole-day boundary, not a timestamp cutoff at 00:00.
        persistUsageLog(vehicle, 15000L, to.atTime(23, 59).toInstant(ZoneOffset.UTC));
        // Recorded at 00:01 the day AFTER `to` — must be excluded.
        persistUsageLog(vehicle, 15100L, to.plusDays(1).atTime(0, 1).toInstant(ZoneOffset.UTC));
        entityManager.getEntityManager().clear();

        Optional<Long> result = usageLogRepository.findLatestValueUpToDate(vehicle.getId(), to);

        assertThat(result).contains(15000L);
    }

    @Test
    void findLatestValueUpToDate_returnsEmpty_whenNoReadingsExist() {
        Vehicle vehicle = persistVehicle("2222BBB");
        entityManager.getEntityManager().clear();

        Optional<Long> result = usageLogRepository.findLatestValueUpToDate(vehicle.getId(), LocalDate.of(2026, 6, 30));

        assertThat(result).isEmpty();
    }

    // --- findLatestValueBeforeDate: the period's baseline ---

    @Test
    void findLatestValueBeforeDate_returnsLastReading_beforeFromDate() {
        Vehicle vehicle = persistVehicle("3333CCC");
        LocalDate from = LocalDate.of(2026, 6, 1);
        persistUsageLog(vehicle, 14000L, from.minusDays(1).atTime(23, 59).toInstant(ZoneOffset.UTC));
        entityManager.getEntityManager().clear();

        Optional<Long> result = usageLogRepository.findLatestValueBeforeDate(vehicle.getId(), from);

        assertThat(result).contains(14000L);
    }

    @Test
    void findLatestValueBeforeDate_excludesReadings_onOrAfterFromDate() {
        Vehicle vehicle = persistVehicle("4444DDD");
        LocalDate from = LocalDate.of(2026, 6, 1);
        // Recorded at 00:00 exactly on `from`'s date — must be excluded, the baseline is strictly
        // before the period starts.
        persistUsageLog(vehicle, 14500L, from.atStartOfDay().toInstant(ZoneOffset.UTC));
        entityManager.getEntityManager().clear();

        Optional<Long> result = usageLogRepository.findLatestValueBeforeDate(vehicle.getId(), from);

        assertThat(result).isEmpty();
    }

    // --- findEarliestValue: fallback baseline when `from` is unset ---

    @Test
    void findEarliestValue_returnsFirstEverReading() {
        Vehicle vehicle = persistVehicle("5555EEE");
        persistUsageLog(vehicle, 13000L, LocalDate.of(2026, 1, 1).atStartOfDay().toInstant(ZoneOffset.UTC));
        persistUsageLog(vehicle, 14000L, LocalDate.of(2026, 3, 1).atStartOfDay().toInstant(ZoneOffset.UTC));
        entityManager.getEntityManager().clear();

        Optional<Long> result = usageLogRepository.findEarliestValue(vehicle.getId());

        assertThat(result).contains(13000L);
    }

    @Test
    void findEarliestValue_returnsEmpty_whenNoReadingsExist() {
        Vehicle vehicle = persistVehicle("6666FFF");
        entityManager.getEntityManager().clear();

        Optional<Long> result = usageLogRepository.findEarliestValue(vehicle.getId());

        assertThat(result).isEmpty();
    }

    private Vehicle persistVehicle(String licensePlate) {
        Vehicle vehicle = new Vehicle();
        vehicle.setVehicleCategory(VehicleCategory.LIGHT_VEHICLE);
        vehicle.setUsageMeasure(UsageMeasure.KILOMETERS);
        vehicle.setMake("Toyota");
        vehicle.setModel("Hilux");
        vehicle.setYear(2020);
        vehicle.setLicensePlate(licensePlate);
        return entityManager.persistAndFlush(vehicle);
    }

    private void persistUsageLog(Vehicle vehicle, long value, java.time.Instant recordedAt) {
        UsageLog usageLog = new UsageLog();
        usageLog.setVehicle(vehicle);
        usageLog.setValue(value);
        usageLog.setMeasureType(UsageMeasure.KILOMETERS);
        usageLog.setRecordedAt(recordedAt);
        usageLog.setSource(UsageSource.JOB_COMPLETION);
        entityManager.persistAndFlush(usageLog);
    }
}
