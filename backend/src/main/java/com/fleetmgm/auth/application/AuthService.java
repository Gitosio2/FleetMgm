package com.fleetmgm.auth.application;

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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class AuthService {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final long LOCK_DURATION_SECONDS = 900;

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogRepository auditLogRepository;
    private final long refreshTokenExpirationMs;

    public AuthService(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            JwtService jwtService,
            PasswordEncoder passwordEncoder,
            AuditLogRepository auditLogRepository,
            @Value("${jwt.refresh-token-expiration-ms}") long refreshTokenExpirationMs) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.auditLogRepository = auditLogRepository;
        this.refreshTokenExpirationMs = refreshTokenExpirationMs;
    }

    // noRollbackFor is load-bearing: on wrong credentials this method records the failed attempt
    // (and locks the account on the 5th) via userRepository.save() *before* throwing
    // BadCredentialsException — without this, Spring's default rollback-on-RuntimeException
    // behavior would undo that save, and the lockout counter would never actually persist.
    @Transactional(noRollbackFor = BadCredentialsException.class)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(BadCredentialsException::new);

        if (user.isLocked()) {
            throw new BadCredentialsException();
        }

        if (!user.isEnabled()) {
            throw new BadCredentialsException();
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            user.incrementFailedLoginAttempts();
            if (user.getFailedLoginAttempts() >= MAX_FAILED_ATTEMPTS) {
                user.setLockedUntil(Instant.now().plusSeconds(LOCK_DURATION_SECONDS));
                userRepository.save(user);
                auditLog(user, AuditAction.ACCOUNT_LOCKED);
            } else {
                userRepository.save(user);
            }
            throw new BadCredentialsException();
        }

        user.resetFailedLoginAttempts();
        userRepository.save(user);
        auditLog(user, AuditAction.LOGIN);

        String rawToken = UUID.randomUUID().toString();
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setTokenHash(sha256(rawToken));
        refreshToken.setExpiresAt(Instant.now().plusMillis(refreshTokenExpirationMs));
        refreshTokenRepository.save(refreshToken);

        String accessToken = jwtService.generateAccessToken(user.getEmail(), user.getAppRole().name());
        return new AuthResponse(accessToken, rawToken, user.getAppRole().name());
    }

    @Transactional
    public AuthResponse refresh(RefreshRequest request) {
        RefreshToken token = refreshTokenRepository.findByTokenHash(sha256(request.refreshToken()))
                .orElseThrow(BadCredentialsException::new);

        if (token.isExpired()) {
            refreshTokenRepository.delete(token);
            throw new BadCredentialsException();
        }

        User user = token.getUser();
        if (!user.isEnabled() || user.isLocked()) {
            refreshTokenRepository.delete(token);
            throw new BadCredentialsException();
        }
        String accessToken = jwtService.generateAccessToken(user.getEmail(), user.getAppRole().name());
        return new AuthResponse(accessToken, request.refreshToken(), user.getAppRole().name());
    }

    @Transactional
    public void logout(RefreshRequest request) {
        refreshTokenRepository.findByTokenHash(sha256(request.refreshToken()))
                .ifPresent(refreshTokenRepository::delete);
    }

    private void auditLog(User user, AuditAction action) {
        AuditLog log = new AuditLog();
        log.setEntityType("User");
        log.setEntityId(user.getId().toString());
        log.setAction(action);
        log.setPerformedByUserId(user.getId());
        log.setPerformedByEmail(user.getEmail());
        log.setPerformedAt(Instant.now());
        auditLogRepository.save(log);
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
