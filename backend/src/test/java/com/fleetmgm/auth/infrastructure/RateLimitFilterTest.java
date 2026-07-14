package com.fleetmgm.auth.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class RateLimitFilterTest {

    private final RateLimitFilter filter = new RateLimitFilter(new ObjectMapper());

    @Test
    void allowsRequests_upToTheLimit() throws Exception {
        FilterChain chain = mock(FilterChain.class);

        for (int i = 0; i < 10; i++) {
            MockHttpServletRequest request = loginRequest("10.0.0.1");
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilter(request, response, chain);
            assertThat(response.getStatus()).isEqualTo(200);
        }

        verify(chain, times(10)).doFilter(any(), any());
    }

    @Test
    void blocksThe11thRequest_fromTheSameIpWithinTheWindow() throws Exception {
        FilterChain chain = mock(FilterChain.class);

        for (int i = 0; i < 10; i++) {
            filter.doFilter(loginRequest("10.0.0.2"), new MockHttpServletResponse(), chain);
        }

        MockHttpServletResponse blockedResponse = new MockHttpServletResponse();
        filter.doFilter(loginRequest("10.0.0.2"), blockedResponse, chain);

        assertThat(blockedResponse.getStatus()).isEqualTo(429);
        assertThat(blockedResponse.getHeader("Retry-After")).isEqualTo("60");
        assertThat(blockedResponse.getContentAsString()).contains("RATE_LIMIT_EXCEEDED");
    }

    @Test
    void tracksLimitsIndependently_perIpAddress() throws Exception {
        FilterChain chain = mock(FilterChain.class);

        for (int i = 0; i < 10; i++) {
            filter.doFilter(loginRequest("10.0.0.3"), new MockHttpServletResponse(), chain);
        }

        MockHttpServletResponse otherIpResponse = new MockHttpServletResponse();
        filter.doFilter(loginRequest("10.0.0.4"), otherIpResponse, chain);

        assertThat(otherIpResponse.getStatus()).isEqualTo(200);
    }

    @Test
    void tracksLimitsIndependently_perEndpoint() throws Exception {
        FilterChain chain = mock(FilterChain.class);

        for (int i = 0; i < 10; i++) {
            filter.doFilter(loginRequest("10.0.0.5"), new MockHttpServletResponse(), chain);
        }

        MockHttpServletRequest refreshRequest = new MockHttpServletRequest("POST", "/api/v1/auth/refresh");
        refreshRequest.setRemoteAddr("10.0.0.5");
        MockHttpServletResponse refreshResponse = new MockHttpServletResponse();
        filter.doFilter(refreshRequest, refreshResponse, chain);

        assertThat(refreshResponse.getStatus()).isEqualTo(200);
    }

    @Test
    void doesNotRateLimit_unrelatedEndpoints() throws Exception {
        FilterChain chain = mock(FilterChain.class);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/vehicles");
        request.setRemoteAddr("10.0.0.6");

        for (int i = 0; i < 20; i++) {
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilter(request, response, chain);
            assertThat(response.getStatus()).isEqualTo(200);
        }
    }

    private MockHttpServletRequest loginRequest(String ip) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        request.setRemoteAddr(ip);
        return request;
    }
}
