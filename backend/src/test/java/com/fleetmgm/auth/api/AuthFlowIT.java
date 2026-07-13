package com.fleetmgm.auth.api;

import com.fleetmgm.auth.domain.AppRole;
import com.fleetmgm.auth.domain.User;
import com.fleetmgm.auth.dto.AuthResponse;
import com.fleetmgm.auth.dto.LoginRequest;
import com.fleetmgm.auth.infrastructure.UserRepository;
import com.fleetmgm.shared.exception.ErrorResponse;
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

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

// Full-flow critical path (CLAUDE.md: "login → job → invoice") — the only test in the codebase
// that drives the real JWT filter chain + lockout end-to-end through a live embedded server,
// as opposed to AuthServiceTest (Mockito, no Spring context) or AuthControllerTest (@WebMvcTest,
// security filter chain disabled).
@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class AuthFlowIT {

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

    @Test
    void login_succeeds_andGrantsAccessToProtectedEndpoint() {
        String email = persistUser();

        ResponseEntity<AuthResponse> loginResponse = restTemplate.postForEntity(
                "/api/v1/auth/login", new LoginRequest(email, PASSWORD), AuthResponse.class);

        assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        String accessToken = loginResponse.getBody().accessToken();
        assertThat(accessToken).isNotBlank();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        ResponseEntity<String> protectedResponse = restTemplate.exchange(
                "/api/v1/vehicles", HttpMethod.GET, new HttpEntity<>(headers), String.class);

        assertThat(protectedResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void protectedEndpoint_returns401_withoutToken() {
        ResponseEntity<ErrorResponse> response =
                restTemplate.getForEntity("/api/v1/vehicles", ErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void fifthFailedLogin_locksAccount_andSubsequentLoginAttemptsReturn401() {
        String email = persistUser();

        for (int attempt = 1; attempt <= 5; attempt++) {
            ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
                    "/api/v1/auth/login", new LoginRequest(email, "wrong-password"), ErrorResponse.class);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        User locked = userRepository.findByEmail(email).orElseThrow();
        assertThat(locked.getFailedLoginAttempts()).isEqualTo(5);
        assertThat(locked.isLocked()).isTrue();

        // Even the correct password is rejected while the account is locked — there is no
        // distinct ACCOUNT_LOCKED error code surfaced to the client, only INVALID_CREDENTIALS.
        ResponseEntity<ErrorResponse> lockedLoginResponse = restTemplate.postForEntity(
                "/api/v1/auth/login", new LoginRequest(email, PASSWORD), ErrorResponse.class);

        assertThat(lockedLoginResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(lockedLoginResponse.getBody().code()).isEqualTo("INVALID_CREDENTIALS");
    }

    private String persistUser() {
        String email = "auth-flow-" + UUID.randomUUID() + "@fleetmgm.test";
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(PASSWORD));
        user.setAppRole(AppRole.ADMIN);
        user.setEnabled(true);
        userRepository.save(user);
        return email;
    }
}
