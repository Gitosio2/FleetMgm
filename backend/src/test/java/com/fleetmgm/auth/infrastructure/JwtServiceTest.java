package com.fleetmgm.auth.infrastructure;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class JwtServiceTest {

    private static final String DEV_SECRET = "changeme-changeme-changeme-changeme-changeme-changeme-changeme-changeme";
    private static final String VALID_PROD_SECRET = "secure-production-jwt-secret-key-that-is-at-least-64-bytes-long-and-random-123456";
    private static final long EXPIRATION_MS = 900_000L; // 15 minutes

    @Nested
    @DisplayName("Dev / Default profile behavior")
    class DevProfileBehavior {

        @Test
        @DisplayName("Should generate and validate JWT tokens correctly with dev secret")
        void shouldGenerateAndValidateTokens() {
            JwtService jwtService = new JwtService(DEV_SECRET, EXPIRATION_MS);

            String token = jwtService.generateAccessToken("admin@fleetmgm.com", "ADMIN");

            assertThat(token).isNotBlank();
            assertThat(jwtService.isTokenValid(token)).isTrue();
            assertThat(jwtService.extractEmail(token)).isEqualTo("admin@fleetmgm.com");
            assertThat(jwtService.extractRole(token)).isEqualTo("ADMIN");
        }

        @Test
        @DisplayName("Should return false for invalid token")
        void shouldReturnFalseForInvalidToken() {
            JwtService jwtService = new JwtService(DEV_SECRET, EXPIRATION_MS);

            assertThat(jwtService.isTokenValid("invalid.token.structure")).isFalse();
        }
    }

    @Nested
    @DisplayName("Prod profile fail-fast validation")
    class ProdProfileValidation {

        @Test
        @DisplayName("Should fail fast in prod profile if secret is the default dev secret")
        void shouldFailIfDefaultDevSecretInProd() {
            Environment env = mock(Environment.class);
            given(env.acceptsProfiles(Profiles.of("prod"))).willReturn(true);

            assertThatThrownBy(() -> new JwtService(DEV_SECRET, EXPIRATION_MS, env))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("cannot be the default development secret");
        }

        @Test
        @DisplayName("Should fail fast in prod profile if secret is blank")
        void shouldFailIfSecretIsBlankInProd() {
            Environment env = mock(Environment.class);
            given(env.acceptsProfiles(Profiles.of("prod"))).willReturn(true);

            assertThatThrownBy(() -> new JwtService("   ", EXPIRATION_MS, env))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("environment variable is required in production");
        }

        @Test
        @DisplayName("Should fail fast in prod profile if secret is shorter than 64 bytes")
        void shouldFailIfSecretIsShortInProd() {
            Environment env = mock(Environment.class);
            given(env.acceptsProfiles(Profiles.of("prod"))).willReturn(true);

            assertThatThrownBy(() -> new JwtService("short-secret-less-than-64-bytes", EXPIRATION_MS, env))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("must be at least 64 bytes long");
        }

        @Test
        @DisplayName("Should succeed in prod profile if valid 64+ byte non-default secret is provided")
        void shouldSucceedWithValidProdSecret() {
            Environment env = mock(Environment.class);
            given(env.acceptsProfiles(Profiles.of("prod"))).willReturn(true);

            JwtService jwtService = new JwtService(VALID_PROD_SECRET, EXPIRATION_MS, env);
            String token = jwtService.generateAccessToken("produser@fleetmgm.com", "MANAGER");

            assertThat(token).isNotBlank();
            assertThat(jwtService.isTokenValid(token)).isTrue();
            assertThat(jwtService.extractEmail(token)).isEqualTo("produser@fleetmgm.com");
            assertThat(jwtService.extractRole(token)).isEqualTo("MANAGER");
        }
    }
}
