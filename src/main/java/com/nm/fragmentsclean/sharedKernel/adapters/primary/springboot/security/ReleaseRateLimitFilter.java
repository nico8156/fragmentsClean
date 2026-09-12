package com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.function.Supplier;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public final class ReleaseRateLimitFilter extends OncePerRequestFilter {
    private final ReleaseRequestRateLimiter limiter;
    private final Supplier<Instant> clock;

    public ReleaseRateLimitFilter(ReleaseRequestRateLimiter limiter) {
        this(limiter, Instant::now);
    }

    ReleaseRateLimitFilter(ReleaseRequestRateLimiter limiter, Supplier<Instant> clock) {
        this.limiter = limiter;
        this.clock = clock;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        String principal = authentication != null && authentication.isAuthenticated() ? authentication.getName() : null;
        var decision = limiter.acquire(request.getMethod(), request.getRequestURI(), principal, clock.get());
        if (decision.permitted()) {
            chain.doFilter(request, response);
            return;
        }
        response.setStatus(429);
        response.setHeader("Retry-After", Long.toString(decision.retryAfterSeconds()));
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"error\":\"RATE_LIMITED\",\"reason\":\"Too many requests\"}");
    }
}
