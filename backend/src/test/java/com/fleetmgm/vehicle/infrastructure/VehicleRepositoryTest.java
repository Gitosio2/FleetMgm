package com.fleetmgm.vehicle.infrastructure;

import com.fleetmgm.vehicle.domain.UsageMeasure;
import com.fleetmgm.vehicle.domain.Vehicle;
import com.fleetmgm.vehicle.domain.VehicleCategory;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE;

@Tag("integration")
@DataJpaTest
@AutoConfigureTestDatabase(replace = NONE)
@Testcontainers
class VehicleRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @DynamicPropertySource
    static void datasourceProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    VehicleRepository vehicleRepository;

    @Autowired
    EntityManager em;

    @Test
    void findAll_excludesSoftDeleted() {
        Vehicle active = buildVehicle("1111AAA");
        Vehicle deleted = buildVehicle("2222BBB");
        deleted.setDeletedAt(Instant.now());

        vehicleRepository.saveAll(List.of(active, deleted));
        em.flush();
        em.clear();

        List<Vehicle> results = vehicleRepository.findAll();

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getLicensePlate()).isEqualTo("1111AAA");
    }

    @Test
    void existsByLicensePlate_returnsTrue_whenExists() {
        vehicleRepository.save(buildVehicle("1234ABC"));

        assertThat(vehicleRepository.existsByLicensePlate("1234ABC")).isTrue();
        assertThat(vehicleRepository.existsByLicensePlate("XXXX")).isFalse();
    }

    private Vehicle buildVehicle(String licensePlate) {
        Vehicle vehicle = new Vehicle();
        vehicle.setVehicleCategory(VehicleCategory.LIGHT_VEHICLE);
        vehicle.setUsageMeasure(UsageMeasure.KILOMETERS);
        vehicle.setMake("Toyota");
        vehicle.setModel("Corolla");
        vehicle.setYear(2020);
        vehicle.setLicensePlate(licensePlate);
        return vehicle;
    }
}
