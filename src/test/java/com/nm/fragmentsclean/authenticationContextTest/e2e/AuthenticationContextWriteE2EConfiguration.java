package com.nm.fragmentsclean.authenticationContextTest.e2e;

import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DateTimeProvider;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.time.Instant;

@TestConfiguration
public class AuthenticationContextWriteE2EConfiguration {

    @Bean
    @Primary
    DateTimeProvider systemDateTimeProviderForAuthTests() {
        return Instant::now;
    }
}
