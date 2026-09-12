package com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ReleaseRateLimitConfiguration {
    @Bean
    ReleaseRequestRateLimiter releaseRequestRateLimiter(
            @Value("${fragments.rate-limit.enabled:false}") boolean enabled,
            @Value("${fragments.rate-limit.ticket-per-minute:10}") int ticketLimit,
            @Value("${fragments.rate-limit.media-per-minute:30}") int mediaLimit,
            @Value("${fragments.rate-limit.ugc-per-minute:60}") int ugcLimit) {
        return new ReleaseRequestRateLimiter(enabled, ticketLimit, mediaLimit, ugcLimit);
    }
}
