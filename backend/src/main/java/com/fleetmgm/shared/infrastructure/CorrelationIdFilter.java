package com.fleetmgm.shared.infrastructure;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

// Always generates the ID server-side rather than trusting an inbound X-Correlation-Id from the
// client — accepting a client-supplied value would let a caller inject arbitrary strings into
// every log line for their own requests (CLAUDE.md: user-supplied data is untrusted until
// validated). Runs first in the chain, before RateLimitFilter/JwtAuthenticationFilter, so even a
// 429 or 401 response is tagged and logged with a correlation ID.
//
// MDC.clear() in the finally block matters: Tomcat reuses worker threads across requests, so
// without it a correlation ID could leak into an unrelated later request's log lines on the same
// thread once this one finishes.
@Component
public class CorrelationIdFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(CorrelationIdFilter.class);

    public static final String MDC_KEY = "correlationId";
    private static final String RESPONSE_HEADER = "X-Correlation-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String correlationId = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        MDC.put(MDC_KEY, correlationId);
        response.setHeader(RESPONSE_HEADER, correlationId);
        long start = System.currentTimeMillis();
        try {
            filterChain.doFilter(request, response);
        } finally {
            // One INFO line per request regardless of whether anything else logged — without this,
            // a request that never hits application code that logs (e.g. a plain GET that succeeds)
            // would carry a correlation ID in MDC and the response header with no log line to
            // actually show it, defeating the point of correlating client-visible IDs to server logs.
            log.info("{} {} -> {} ({}ms)", request.getMethod(), request.getRequestURI(),
                    response.getStatus(), System.currentTimeMillis() - start);
            MDC.remove(MDC_KEY);
        }
    }
}
