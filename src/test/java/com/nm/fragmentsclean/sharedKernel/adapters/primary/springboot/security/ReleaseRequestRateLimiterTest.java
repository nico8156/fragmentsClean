package com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class ReleaseRequestRateLimiterTest {
    private static final Instant NOW = Instant.parse("2026-09-12T10:00:00Z");

    @Test void limits_costly_routes_per_authenticated_user_and_category() {
        var limiter = new ReleaseRequestRateLimiter(true, 1, 1, 1);

        assertThat(limiter.acquire("POST", "/api/tickets/verify", "user-a", NOW).permitted()).isTrue();
        var rejected = limiter.acquire("POST", "/api/tickets/verify", "user-a", NOW.plusSeconds(1));
        assertThat(rejected.permitted()).isFalse();
        assertThat(rejected.retryAfterSeconds()).isEqualTo(59);
        assertThat(limiter.acquire("POST", "/api/tickets/verify", "user-b", NOW.plusSeconds(1)).permitted()).isTrue();

        assertThat(limiter.acquire("POST", "/api/experiences/e/media/upload-intents", "user-a", NOW).permitted()).isTrue();
        assertThat(limiter.acquire("POST", "/api/users/me/avatar/m/confirm", "user-a", NOW.plusSeconds(1)).permitted()).isFalse();

        assertThat(limiter.acquire("POST", "/api/experiences/e/publish", "user-a", NOW).permitted()).isTrue();
        assertThat(limiter.acquire("POST", "/api/social/comments/c/reports", "user-a", NOW.plusSeconds(1)).permitted()).isFalse();
    }

    @Test void ignores_unlisted_or_unauthenticated_requests_and_reopens_expired_windows() {
        var limiter = new ReleaseRequestRateLimiter(true, 1, 1, 1);

        assertThat(limiter.acquire("GET", "/api/coffees", "user-a", NOW).permitted()).isTrue();
        assertThat(limiter.acquire("POST", "/api/tickets/verify", null, NOW).permitted()).isTrue();
        assertThat(limiter.acquire("POST", "/api/tickets/verify", "user-a", NOW).permitted()).isTrue();
        assertThat(limiter.acquire("POST", "/api/tickets/verify", "user-a", NOW.plusSeconds(60)).permitted()).isTrue();
    }
}
