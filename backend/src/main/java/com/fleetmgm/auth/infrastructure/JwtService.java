package com.fleetmgm.auth.infrastructure;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {

    public static final String DEFAULT_SECRET = "changeme-changeme-changeme-changeme-changeme-changeme-changeme-changeme";

    private final SecretKey signingKey;
    private final long accessTokenExpirationMs;

    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-expiration-ms}") long accessTokenExpirationMs,
            Environment environment) {
        if (environment != null && environment.acceptsProfiles(Profiles.of("prod"))) {
            validateProdSecret(secret);
        }
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpirationMs = accessTokenExpirationMs;
    }

    public JwtService(
            String secret,
            long accessTokenExpirationMs) {
        this(secret, accessTokenExpirationMs, null);
    }

    private void validateProdSecret(String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("JWT_SECRET environment variable is required in production");
        }
        if (DEFAULT_SECRET.equals(secret)) {
            throw new IllegalStateException("JWT_SECRET in production cannot be the default development secret");
        }
        if (secret.getBytes(StandardCharsets.UTF_8).length < 64) {
            throw new IllegalStateException("JWT_SECRET in production must be at least 64 bytes long");
        }
    }

    public String generateAccessToken(String email, String role) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(email)
                .claim("role", role)
                .issuedAt(new Date(now))
                .expiration(new Date(now + accessTokenExpirationMs))
                .signWith(signingKey, Jwts.SIG.HS512)
                .compact();
    }

    public boolean isTokenValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public String extractEmail(String token) {
        return parseClaims(token).getSubject();
    }

    public String extractRole(String token) {
        return parseClaims(token).get("role", String.class);
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
