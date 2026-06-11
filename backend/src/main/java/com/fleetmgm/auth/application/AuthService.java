package com.fleetmgm.auth.application;

import com.fleetmgm.auth.domain.RefreshToken;
import com.fleetmgm.auth.domain.User;
import com.fleetmgm.auth.dto.AuthResponse;
import com.fleetmgm.auth.dto.LoginRequest;
import com.fleetmgm.auth.dto.RefreshRequest;
import com.fleetmgm.auth.infrastructure.JwtService;
import com.fleetmgm.auth.infrastructure.RefreshTokenRepository;
import com.fleetmgm.auth.infrastructure.UserRepository;
import com.fleetmgm.shared.exception.BadCredentialsException;
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

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final long refreshTokenExpirationMs;

    public AuthService(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            JwtService jwtService,
            PasswordEncoder passwordEncoder,
            @Value("${jwt.refresh-token-expiration-ms}") long refreshTokenExpirationMs) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.refreshTokenExpirationMs = refreshTokenExpirationMs;
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(BadCredentialsException::new);

        if (!user.isEnabled() || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException();
        }

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
        String accessToken = jwtService.generateAccessToken(user.getEmail(), user.getAppRole().name());
        return new AuthResponse(accessToken, request.refreshToken(), user.getAppRole().name());
    }

    @Transactional
    public void logout(RefreshRequest request) {
        refreshTokenRepository.findByTokenHash(sha256(request.refreshToken()))
                .ifPresent(refreshTokenRepository::delete);
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
