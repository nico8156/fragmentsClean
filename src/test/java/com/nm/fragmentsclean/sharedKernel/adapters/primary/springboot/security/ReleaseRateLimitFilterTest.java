package com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class ReleaseRateLimitFilterTest {
    private static final Instant NOW = Instant.parse("2026-09-12T10:00:00Z");

    @AfterEach void clearSecurityContext() { SecurityContextHolder.clearContext(); }

    @Test void returns_a_stable_429_contract_without_invoking_the_application() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated("user-a", "n/a", List.of()));
        var filter = new ReleaseRateLimitFilter(new ReleaseRequestRateLimiter(true, 1, 1, 1), () -> NOW);
        var firstChain = new MockFilterChain();
        filter.doFilter(new MockHttpServletRequest("POST", "/api/tickets/verify"), new MockHttpServletResponse(), firstChain);
        assertThat(firstChain.getRequest()).isNotNull();

        var response = new MockHttpServletResponse();
        var rejectedChain = new MockFilterChain();
        filter.doFilter(new MockHttpServletRequest("POST", "/api/tickets/verify"), response, rejectedChain);

        assertThat(rejectedChain.getRequest()).isNull();
        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getHeader("Retry-After")).isEqualTo("60");
        assertThat(response.getContentType()).isEqualTo("application/json");
        assertThat(response.getContentAsString()).isEqualTo(
                "{\"error\":\"RATE_LIMITED\",\"reason\":\"Too many requests\"}");
    }
}
