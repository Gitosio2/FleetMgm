package com.fleetmgm.auth.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fleetmgm.shared.exception.ErrorResponse;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

// Insurance-Design D/G (planning.md): rate-limits /auth/login and /auth/refresh independently of
// the account-lockout mechanism in AuthService — lockout stops a single account from being brute
// forced, this stops one client (by IP) from hammering the endpoint across many different
// accounts/emails, which lockout alone does not address.
//
// In-memory (ConcurrentHashMap<ip, Bucket>), not a distributed store (Redis, etc.) — this backend
// runs as a single instance for this project's scale (solo-dev thesis demo, not a multi-node
// deployment), so a shared external rate-limit store would be complexity with no real benefit
// here. If this backend is ever horizontally scaled, this filter would need to move to a shared
// store or a reverse-proxy-level limiter instead.
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Set<String> LIMITED_PATHS = Set.of("/api/v1/auth/login", "/api/v1/auth/refresh");
    private static final int CAPACITY = 10;
    private static final Duration REFILL_PERIOD = Duration.ofMinutes(1);

    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    public RateLimitFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!LIMITED_PATHS.contains(request.getRequestURI())) {
            filterChain.doFilter(request, response);
            return;
        }

        // Keyed by path + IP, not IP alone, so a client hammering /login doesn't also burn its
        // /refresh budget (and vice versa) — the two endpoints have very different legitimate
        // call patterns (login: occasional, user-driven; refresh: automatic, once per access-token
        // lifetime per active session).
        String key = request.getRequestURI() + '|' + request.getRemoteAddr();
        Bucket bucket = buckets.computeIfAbsent(key, k -> newBucket());

        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
            return;
        }

        writeTooManyRequests(response);
    }

    private Bucket newBucket() {
        return Bucket.builder()
                .addLimit(Bandwidth.simple(CAPACITY, REFILL_PERIOD))
                .build();
    }

    private void writeTooManyRequests(HttpServletResponse response) throws IOException {
        response.setStatus(429);
        response.setHeader("Retry-After", String.valueOf(REFILL_PERIOD.toSeconds()));
        response.setContentType("application/json");
        response.getWriter().write(objectMapper.writeValueAsString(
                new ErrorResponse(429, "RATE_LIMIT_EXCEEDED", "Too many requests — try again later", correlationId())));
    }

    private String correlationId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }
}
