package com.nm.fragmentsclean.authenticationContextTest.e2e;

import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.providers.DeterministicDateTimeProvider;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DateTimeProvider;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class AuthenticationContextWriteE2EConfiguration {

    @Primary
    @Bean
    public DateTimeProvider deterministicClockProvider() {
        return new DeterministicDateTimeProvider();
    }
}
