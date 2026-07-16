package com.fleetmgm.job.api;

import com.fleetmgm.auth.domain.AppRole;
import com.fleetmgm.auth.domain.User;
import com.fleetmgm.auth.dto.AuthResponse;
import com.fleetmgm.auth.dto.LoginRequest;
import com.fleetmgm.auth.infrastructure.UserRepository;
import com.fleetmgm.job.dto.CompleteJobRequest;
import com.fleetmgm.job.dto.CreateJobRequest;
import com.fleetmgm.job.dto.JobResponse;
import com.fleetmgm.job.dto.StartJobRequest;
import com.fleetmgm.vehicle.domain.UsageLog;
import com.fleetmgm.vehicle.domain.UsageMeasure;
import com.fleetmgm.vehicle.domain.UsageSource;
import com.fleetmgm.vehicle.domain.Vehicle;
import com.fleetmgm.vehicle.domain.VehicleCategory;
import com.fleetmgm.vehicle.infrastructure.UsageLogRepository;
import com.fleetmgm.vehicle.infrastructure.VehicleRepository;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

// Full-flow critical path (CLAUDE.md: "login → job → invoice"). Drives the real HTTP server
// (not MockMvc) deliberately: JobEventListener.onJobCompleted is a
// @TransactionalEventListener(phase = AFTER_COMMIT), which only fires once the completing
// request's transaction actually commits — a MockMvc call wrapped in a test-level @Transactional
// would roll back before that commit and the listener would never run.
@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class JobLifecycleIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    private static final String PASSWORD = "correct-horse-battery-1";

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    UserRepository userRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    VehicleRepository vehicleRepository;

    @Autowired
    UsageLogRepository usageLogRepository;

    @Test
    void createStartComplete_updatesVehicleUsage_andCreatesUsageLog() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(loginAsAdmin());

        Vehicle vehicle = persistVehicle();

        // clientId left null so InvoiceJobCompletionListener (which only fires when both clientId
        // and price are set) stays a no-op — this test only cares about the vehicle-usage side.
        CreateJobRequest createRequest = new CreateJobRequest(
                vehicle.getId(), null, null, "Reparto centro", null,
                "Almacén central", "Cliente final", null, null, null, null, null, null);
        ResponseEntity<JobResponse> createResponse = restTemplate.exchange(
                "/api/v1/jobs", HttpMethod.POST, new HttpEntity<>(createRequest, headers), JobResponse.class);
        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        UUID jobId = createResponse.getBody().id();

        ResponseEntity<JobResponse> startResponse = restTemplate.exchange(
                "/api/v1/jobs/{id}/start", HttpMethod.PATCH,
                new HttpEntity<>(new StartJobRequest(1000L), headers), JobResponse.class, jobId);
        assertThat(startResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<JobResponse> completeResponse = restTemplate.exchange(
                "/api/v1/jobs/{id}/complete", HttpMethod.PATCH,
                new HttpEntity<>(new CompleteJobRequest(1500L), headers), JobResponse.class, jobId);
        assertThat(completeResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        Vehicle updatedVehicle = vehicleRepository.findById(vehicle.getId()).orElseThrow();
        assertThat(updatedVehicle.getCurrentKm()).isEqualTo(1500L);

        List<UsageLog> logs = usageLogRepository.findAll().stream()
                .filter(log -> log.getVehicle().getId().equals(vehicle.getId()))
                .toList();
        assertThat(logs).hasSize(1);
        assertThat(logs.get(0).getValue()).isEqualTo(1500L);
        assertThat(logs.get(0).getSource()).isEqualTo(UsageSource.JOB_COMPLETION);
    }

    private String loginAsAdmin() {
        String email = "job-flow-" + UUID.randomUUID() + "@fleetmgm.test";
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(PASSWORD));
        user.setAppRole(AppRole.ADMIN);
        user.setEnabled(true);
        userRepository.save(user);

        ResponseEntity<AuthResponse> loginResponse = restTemplate.postForEntity(
                "/api/v1/auth/login", new LoginRequest(email, PASSWORD), AuthResponse.class);
        return loginResponse.getBody().accessToken();
    }

    private Vehicle persistVehicle() {
        Vehicle vehicle = new Vehicle();
        vehicle.setVehicleCategory(VehicleCategory.LIGHT_VEHICLE);
        vehicle.setUsageMeasure(UsageMeasure.KILOMETERS);
        vehicle.setMake("Toyota");
        vehicle.setModel("Hilux");
        vehicle.setYear(2020);
        vehicle.setLicensePlate("IT-" + UUID.randomUUID().toString().substring(0, 8));
        vehicle.setCurrentKm(1000L);
        return vehicleRepository.save(vehicle);
    }
}
