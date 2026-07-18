package com.fleetmgm.vehicle.infrastructure;

import com.fleetmgm.config.AuditorAwareImpl;
import com.fleetmgm.config.JpaAuditingConfig;
import com.fleetmgm.vehicle.domain.UsageMeasure;
import com.fleetmgm.vehicle.domain.Vehicle;
import com.fleetmgm.vehicle.domain.VehicleCategory;
import com.fleetmgm.vehicle.domain.VehicleStatus;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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
@Import({JpaAuditingConfig.class, AuditorAwareImpl.class})
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

    @Test
    void countByStatus_countsOnlyVehiclesInThatStatus() {
        Vehicle active = buildVehicle("3333CCC");
        Vehicle maintenance = buildVehicle("4444DDD");
        maintenance.setStatus(VehicleStatus.MAINTENANCE);
        vehicleRepository.saveAll(List.of(active, maintenance));
        em.flush();
        em.clear();

        assertThat(vehicleRepository.countByStatus(VehicleStatus.ACTIVE)).isEqualTo(1);
        assertThat(vehicleRepository.countByStatus(VehicleStatus.MAINTENANCE)).isEqualTo(1);
    }

    @Test
    void countByStatusNot_excludesOnlyTheGivenStatus() {
        Vehicle active = buildVehicle("5555EEE");
        Vehicle decommissioned = buildVehicle("6666FFF");
        decommissioned.setStatus(VehicleStatus.DECOMMISSIONED);
        vehicleRepository.saveAll(List.of(active, decommissioned));
        em.flush();
        em.clear();

        assertThat(vehicleRepository.countByStatusNot(VehicleStatus.DECOMMISSIONED)).isEqualTo(1);
    }

    @Test
    void search_narrowsByCategory_whenProvided() {
        Vehicle light = buildVehicle("Toyota", "Corolla", VehicleCategory.LIGHT_VEHICLE,
                VehicleStatus.ACTIVE, "7777GGG");
        Vehicle heavy = buildVehicle("Volvo", "FH16", VehicleCategory.HEAVY_VEHICLE,
                VehicleStatus.ACTIVE, "8888HHH");
        vehicleRepository.saveAll(List.of(light, heavy));
        em.flush();
        em.clear();

        Page<Vehicle> result = vehicleRepository.search(VehicleCategory.HEAVY_VEHICLE, null, null, null,
                PageRequest.of(0, 20));

        assertThat(result.getContent()).extracting(Vehicle::getLicensePlate).containsExactly("8888HHH");
    }

    @Test
    void search_narrowsByStatus_whenProvided() {
        Vehicle active = buildVehicle("Toyota", "Corolla", VehicleCategory.LIGHT_VEHICLE,
                VehicleStatus.ACTIVE, "9999III");
        Vehicle maintenance = buildVehicle("Toyota", "Hilux", VehicleCategory.LIGHT_VEHICLE,
                VehicleStatus.MAINTENANCE, "0000JJJ");
        vehicleRepository.saveAll(List.of(active, maintenance));
        em.flush();
        em.clear();

        Page<Vehicle> result = vehicleRepository.search(null, VehicleStatus.MAINTENANCE, null, null,
                PageRequest.of(0, 20));

        assertThat(result.getContent()).extracting(Vehicle::getLicensePlate).containsExactly("0000JJJ");
    }

    @Test
    void search_narrowsByLicensePlate_partialMatch_caseInsensitive_whenProvided() {
        Vehicle match = buildVehicle("Toyota", "Corolla", VehicleCategory.LIGHT_VEHICLE,
                VehicleStatus.ACTIVE, "1212KKK");
        Vehicle other = buildVehicle("Toyota", "Corolla", VehicleCategory.LIGHT_VEHICLE,
                VehicleStatus.ACTIVE, "3434LLL");
        vehicleRepository.saveAll(List.of(match, other));
        em.flush();
        em.clear();

        Page<Vehicle> result = vehicleRepository.search(null, null, "212k", null, PageRequest.of(0, 20));

        assertThat(result.getContent()).extracting(Vehicle::getLicensePlate).containsExactly("1212KKK");
    }

    @Test
    void search_narrowsByVehicle_matchesCombinedMakeAndModel_caseInsensitive_whenProvided() {
        Vehicle match = buildVehicle("Mercedes-Benz", "Sprinter", VehicleCategory.HEAVY_VEHICLE,
                VehicleStatus.ACTIVE, "5656MMM");
        Vehicle other = buildVehicle("Renault", "Master", VehicleCategory.HEAVY_VEHICLE,
                VehicleStatus.ACTIVE, "7878NNN");
        vehicleRepository.saveAll(List.of(match, other));
        em.flush();
        em.clear();

        Page<Vehicle> result = vehicleRepository.search(null, null, null, "benz sprint", PageRequest.of(0, 20));

        assertThat(result.getContent()).extracting(Vehicle::getLicensePlate).containsExactly("5656MMM");
    }

    @Test
    void search_combinesAllFilters_whenAllProvided() {
        Vehicle match = buildVehicle("Toyota", "Hilux", VehicleCategory.LIGHT_VEHICLE,
                VehicleStatus.ACTIVE, "9090OOO");
        Vehicle wrongCategory = buildVehicle("Toyota", "Hilux", VehicleCategory.HEAVY_VEHICLE,
                VehicleStatus.ACTIVE, "9191PPP");
        Vehicle wrongStatus = buildVehicle("Toyota", "Hilux", VehicleCategory.LIGHT_VEHICLE,
                VehicleStatus.MAINTENANCE, "9292QQQ");
        vehicleRepository.saveAll(List.of(match, wrongCategory, wrongStatus));
        em.flush();
        em.clear();

        Page<Vehicle> result = vehicleRepository.search(
                VehicleCategory.LIGHT_VEHICLE, VehicleStatus.ACTIVE, "9090", "hilux", PageRequest.of(0, 20));

        assertThat(result.getContent()).extracting(Vehicle::getLicensePlate).containsExactly("9090OOO");
    }

    @Test
    void search_returnsAll_whenNoFiltersProvided() {
        vehicleRepository.saveAll(List.of(buildVehicle("1010RRR"), buildVehicle("2020SSS")));
        em.flush();
        em.clear();

        Page<Vehicle> result = vehicleRepository.search(null, null, null, null, PageRequest.of(0, 20));

        assertThat(result.getContent()).hasSize(2);
    }

    private Vehicle buildVehicle(String licensePlate) {
        return buildVehicle("Toyota", "Corolla", VehicleCategory.LIGHT_VEHICLE, VehicleStatus.ACTIVE, licensePlate);
    }

    private Vehicle buildVehicle(String make, String model, VehicleCategory category, VehicleStatus status,
            String licensePlate) {
        Vehicle vehicle = new Vehicle();
        vehicle.setVehicleCategory(category);
        vehicle.setUsageMeasure(UsageMeasure.KILOMETERS);
        vehicle.setMake(make);
        vehicle.setModel(model);
        vehicle.setYear(2020);
        vehicle.setLicensePlate(licensePlate);
        vehicle.setStatus(status);
        return vehicle;
    }
}
