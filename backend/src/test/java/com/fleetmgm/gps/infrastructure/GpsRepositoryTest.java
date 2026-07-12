package com.fleetmgm.gps.infrastructure;

import com.fleetmgm.config.AuditorAwareImpl;
import com.fleetmgm.config.JpaAuditingConfig;
import com.fleetmgm.gps.domain.GpsPosition;
import com.fleetmgm.gps.domain.GpsSource;
import com.fleetmgm.vehicle.domain.UsageMeasure;
import com.fleetmgm.vehicle.domain.Vehicle;
import com.fleetmgm.vehicle.domain.VehicleCategory;
import com.fleetmgm.vehicle.domain.VehicleStatus;
import org.hibernate.Hibernate;
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

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE;

@Tag("integration")
@DataJpaTest
@AutoConfigureTestDatabase(replace = NONE)
@Testcontainers
@Import({JpaAuditingConfig.class, AuditorAwareImpl.class})
class GpsRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    GpsRepository gpsRepository;

    @Autowired
    TestEntityManager entityManager;

    @Test
    void findLatestByVehicleId_returnsMostRecentPosition() {
        Vehicle vehicle = persistVehicle("1111AAA", VehicleStatus.ACTIVE);
        persistPosition(vehicle, Instant.now().minusSeconds(60));
        GpsPosition latest = persistPosition(vehicle, Instant.now());
        entityManager.getEntityManager().clear();

        Optional<GpsPosition> result = gpsRepository.findFirstByVehicleIdOrderByRecordedAtDesc(vehicle.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(latest.getId());
        assertThat(Hibernate.isInitialized(result.get().getVehicle())).isTrue();
    }

    @Test
    void findLatestByVehicleId_returnsEmpty_whenVehicleHasNoPositions() {
        Vehicle vehicle = persistVehicle("2222BBB", VehicleStatus.ACTIVE);

        Optional<GpsPosition> result = gpsRepository.findFirstByVehicleIdOrderByRecordedAtDesc(vehicle.getId());

        assertThat(result).isEmpty();
    }

    @Test
    void findLatestForAllActiveVehicles_excludesInactiveVehicles() {
        Vehicle active = persistVehicle("3333CCC", VehicleStatus.ACTIVE);
        Vehicle inactive = persistVehicle("4444DDD", VehicleStatus.INACTIVE);
        persistPosition(active, Instant.now());
        persistPosition(inactive, Instant.now());
        entityManager.getEntityManager().clear();

        List<GpsPosition> result = gpsRepository.findLatestForAllActiveVehicles();

        assertThat(result).extracting(g -> g.getVehicle().getId()).containsExactly(active.getId());
    }

    @Test
    void findLatestForAllActiveVehicles_returnsOnlyLatestPositionPerVehicle() {
        Vehicle vehicleA = persistVehicle("5555EEE", VehicleStatus.ACTIVE);
        Vehicle vehicleB = persistVehicle("6666FFF", VehicleStatus.ACTIVE);
        persistPosition(vehicleA, Instant.now().minusSeconds(60));
        GpsPosition latestA = persistPosition(vehicleA, Instant.now());
        GpsPosition latestB = persistPosition(vehicleB, Instant.now().minusSeconds(30));
        entityManager.getEntityManager().clear();

        List<GpsPosition> result = gpsRepository.findLatestForAllActiveVehicles();

        assertThat(result).extracting(GpsPosition::getId)
                .containsExactlyInAnyOrder(latestA.getId(), latestB.getId());
    }

    private Vehicle persistVehicle(String licensePlate, VehicleStatus status) {
        Vehicle vehicle = new Vehicle();
        vehicle.setVehicleCategory(VehicleCategory.LIGHT_VEHICLE);
        vehicle.setUsageMeasure(UsageMeasure.KILOMETERS);
        vehicle.setMake("Toyota");
        vehicle.setModel("Hilux");
        vehicle.setYear(2020);
        vehicle.setLicensePlate(licensePlate);
        vehicle.setStatus(status);
        return entityManager.persistAndFlush(vehicle);
    }

    private GpsPosition persistPosition(Vehicle vehicle, Instant recordedAt) {
        GpsPosition position = new GpsPosition();
        position.setVehicle(vehicle);
        position.setLatitude(40.0);
        position.setLongitude(-3.0);
        position.setSpeed(50.0);
        position.setRecordedAt(recordedAt);
        position.setSource(GpsSource.MOCK);
        return entityManager.persistAndFlush(position);
    }
}
