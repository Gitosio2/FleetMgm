package com.fleetmgm.auth.application;

import com.fleetmgm.auth.domain.AppRole;
import com.fleetmgm.auth.domain.RefreshToken;
import com.fleetmgm.auth.domain.User;
import com.fleetmgm.auth.dto.AuthResponse;
import com.fleetmgm.auth.dto.LoginRequest;
import com.fleetmgm.auth.dto.RefreshRequest;
import com.fleetmgm.auth.infrastructure.JwtService;
import com.fleetmgm.auth.infrastructure.RefreshTokenRepository;
import com.fleetmgm.auth.infrastructure.UserRepository;
import com.fleetmgm.shared.domain.AuditAction;
import com.fleetmgm.shared.domain.AuditLog;
import com.fleetmgm.shared.exception.BadCredentialsException;
import com.fleetmgm.shared.infrastructure.AuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock RefreshTokenRepository refreshTokenRepository;
    @Mock JwtService jwtService;
    @Mock PasswordEncoder passwordEncoder;
    @Mock AuditLogRepository auditLogRepository;

    AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                userRepository, refreshTokenRepository, jwtService,
                passwordEncoder, auditLogRepository, 604_800_000L);
    }

    // --- login() ---

    @Test
    void login_returnsTokens_whenCredentialsValid() {
        User user = activeUser();
        when(userRepository.findByEmail("driver@fleetmgm.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("secret", user.getPasswordHash())).thenReturn(true);
        when(jwtService.generateAccessToken(anyString(), anyString())).thenReturn("access.token");

        AuthResponse response = authService.login(new LoginRequest("driver@fleetmgm.com", "secret"));

        assertThat(response.accessToken()).isEqualTo("access.token");
        assertThat(response.role()).isEqualTo(AppRole.DRIVER.name());
        verify(userRepository).save(user);
        assertThat(user.getFailedLoginAttempts()).isZero();

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertThat(captor.getValue().getAction()).isEqualTo(AuditAction.LOGIN);
    }

    @Test
    void login_throwsBadCredentials_whenUserNotFound() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("unknown@x.com", "pass")))
                .isInstanceOf(BadCredentialsException.class);
        verify(auditLogRepository, never()).save(any());
    }

    @Test
    void login_throwsBadCredentials_whenAccountDisabled() {
        User user = activeUser();
        user.setEnabled(false);
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(new LoginRequest("driver@fleetmgm.com", "secret")))
                .isInstanceOf(BadCredentialsException.class);
        verify(auditLogRepository, never()).save(any());
    }

    @Test
    void login_throwsBadCredentials_whenAccountLocked() {
        User user = activeUser();
        user.setLockedUntil(Instant.now().plusSeconds(900));
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(new LoginRequest("driver@fleetmgm.com", "secret")))
                .isInstanceOf(BadCredentialsException.class);
        verify(auditLogRepository, never()).save(any());
    }

    @Test
    void login_incrementsFailedAttempts_onWrongPassword() {
        User user = activeUser();
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("driver@fleetmgm.com", "wrong")))
                .isInstanceOf(BadCredentialsException.class);

        assertThat(user.getFailedLoginAttempts()).isEqualTo(1);
        assertThat(user.getLockedUntil()).isNull();
        verify(userRepository).save(user);
        verify(auditLogRepository, never()).save(any());
    }

    @Test
    void login_locksAccount_afterFiveFailures() {
        User user = activeUser();
        user.setFailedLoginAttempts(4);
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("driver@fleetmgm.com", "wrong")))
                .isInstanceOf(BadCredentialsException.class);

        assertThat(user.getFailedLoginAttempts()).isEqualTo(5);
        assertThat(user.getLockedUntil()).isNotNull();
        assertThat(user.isLocked()).isTrue();
        verify(userRepository).save(user);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertThat(captor.getValue().getAction()).isEqualTo(AuditAction.ACCOUNT_LOCKED);
    }

    // --- refresh() ---

    @Test
    void refresh_throwsBadCredentials_whenUserIsDisabled() {
        User user = activeUser();
        user.setEnabled(false);
        RefreshToken token = validToken(user);
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> authService.refresh(new RefreshRequest("raw-token")))
                .isInstanceOf(BadCredentialsException.class);
        verify(refreshTokenRepository).delete(token);
    }

    @Test
    void refresh_throwsBadCredentials_whenUserIsLocked() {
        User user = activeUser();
        user.setLockedUntil(Instant.now().plusSeconds(900));
        RefreshToken token = validToken(user);
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> authService.refresh(new RefreshRequest("raw-token")))
                .isInstanceOf(BadCredentialsException.class);
        verify(refreshTokenRepository).delete(token);
    }

    @Test
    void refresh_returnsNewAccessToken_whenTokenValidAndUserActive() {
        User user = activeUser();
        RefreshToken token = validToken(user);
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(token));
        when(jwtService.generateAccessToken(anyString(), anyString())).thenReturn("new.access.token");

        AuthResponse response = authService.refresh(new RefreshRequest("raw-token"));

        assertThat(response.accessToken()).isEqualTo("new.access.token");
        assertThat(response.refreshToken()).isEqualTo("raw-token");
    }

    @Test
    void refresh_throwsBadCredentials_whenTokenNotFound() {
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refresh(new RefreshRequest("unknown")))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void refresh_throwsBadCredentials_andDeletesToken_whenExpired() {
        User user = activeUser();
        RefreshToken token = expiredToken(user);
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> authService.refresh(new RefreshRequest("expired-token")))
                .isInstanceOf(BadCredentialsException.class);
        verify(refreshTokenRepository).delete(token);
    }

    // --- logout() ---

    @Test
    void logout_deletesToken_whenExists() {
        RefreshToken token = validToken(activeUser());
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(token));

        authService.logout(new RefreshRequest("raw-token"));

        verify(refreshTokenRepository).delete(token);
    }

    @Test
    void logout_doesNothing_whenTokenNotFound() {
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.empty());

        authService.logout(new RefreshRequest("unknown"));

        verify(refreshTokenRepository, never()).delete(any());
    }

    // --- helpers ---

    private User activeUser() {
        User user = new User();
        setId(user, UUID.randomUUID());
        user.setEmail("driver@fleetmgm.com");
        user.setPasswordHash("$2a$12$hashedpassword");
        user.setAppRole(AppRole.DRIVER);
        user.setEnabled(true);
        return user;
    }

    private static void setId(User user, UUID id) {
        try {
            var field = User.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(user, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Could not set User.id", e);
        }
    }

    private RefreshToken validToken(User user) {
        RefreshToken token = new RefreshToken();
        token.setUser(user);
        token.setTokenHash("hash");
        token.setExpiresAt(Instant.now().plusSeconds(3600));
        return token;
    }

    private RefreshToken expiredToken(User user) {
        RefreshToken token = new RefreshToken();
        token.setUser(user);
        token.setTokenHash("hash");
        token.setExpiresAt(Instant.now().minusSeconds(1));
        return token;
    }
}
