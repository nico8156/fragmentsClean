package com.nm.fragmentsclean.authenticationContextTest.e2e;

import com.nm.fragmentsclean.authenticationContext.write.adapters.secondary.FakeGoogleAuthService;
import com.nm.fragmentsclean.authenticationContext.write.businesslogic.gateways.GoogleAuthService;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DateTimeProvider;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;


import java.time.Instant;

@TestConfiguration
public class AuthenticationContextWriteE2EConfiguration {

    @Bean
    DateTimeProvider systemDateTimeProviderForAuthTests() {
        return Instant::now;
    }

    @Bean
    @Primary
    GoogleAuthService googleAuthService() {
        return new FakeGoogleAuthService();
    }
}
