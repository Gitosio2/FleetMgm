package com.fleetmgm.shared.infrastructure;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

class CorrelationIdFilterTest {

    private final CorrelationIdFilter filter = new CorrelationIdFilter();
    private ListAppender<ILoggingEvent> logAppender;
    private ch.qos.logback.classic.Logger filterLogger;

    @BeforeEach
    void attachLogCapture() {
        logAppender = new ListAppender<>();
        logAppender.start();
        filterLogger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(CorrelationIdFilter.class);
        filterLogger.addAppender(logAppender);
    }

    @AfterEach
    void detachLogCapture() {
        filterLogger.detachAppender(logAppender);
    }

    @Test
    void putsACorrelationId_inMdcWhileTheChainRuns() throws Exception {
        AtomicReference<String> seenDuringChain = new AtomicReference<>();
        FilterChain chain = (req, res) -> seenDuringChain.set(MDC.get(CorrelationIdFilter.MDC_KEY));

        filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), chain);

        assertThat(seenDuringChain.get()).isNotBlank();
    }

    @Test
    void exposesTheSameCorrelationId_asAResponseHeader() throws Exception {
        AtomicReference<String> seenDuringChain = new AtomicReference<>();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, res) -> seenDuringChain.set(MDC.get(CorrelationIdFilter.MDC_KEY));

        filter.doFilter(new MockHttpServletRequest(), response, chain);

        assertThat(response.getHeader("X-Correlation-Id")).isEqualTo(seenDuringChain.get());
    }

    @Test
    void clearsMdc_afterTheChainCompletes() throws Exception {
        FilterChain chain = (req, res) -> { };

        filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), chain);

        assertThat(MDC.get(CorrelationIdFilter.MDC_KEY)).isNull();
    }

    @Test
    void clearsMdc_evenWhenTheChainThrows() throws Exception {
        FilterChain chain = mock(FilterChain.class);
        doThrow(new ServletException("boom")).when(chain).doFilter(any(), any());

        assertThatThrownBy(() -> filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), chain))
                .isInstanceOf(ServletException.class);

        assertThat(MDC.get(CorrelationIdFilter.MDC_KEY)).isNull();
    }

    @Test
    void logsOneAccessLine_perRequest_evenWhenNothingElseLogs() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/vehicles");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(200);
        FilterChain chain = (req, res) -> { };

        filter.doFilter(request, response, chain);

        assertThat(logAppender.list).hasSize(1);
        assertThat(logAppender.list.get(0).getFormattedMessage()).contains("GET").contains("/api/v1/vehicles").contains("200");
    }

    @Test
    void generatesADifferentId_forEachRequest() throws Exception {
        MockHttpServletResponse first = new MockHttpServletResponse();
        MockHttpServletResponse second = new MockHttpServletResponse();
        FilterChain chain = (req, res) -> { };

        filter.doFilter(new MockHttpServletRequest(), first, chain);
        filter.doFilter(new MockHttpServletRequest(), second, chain);

        assertThat(first.getHeader("X-Correlation-Id")).isNotEqualTo(second.getHeader("X-Correlation-Id"));
    }
}
