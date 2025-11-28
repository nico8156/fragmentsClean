package com.nm.fragmentsclean.authContext.e2e;

import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.providers.DeterministicDateTimeProvider;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.providers.LoggingOutboxEventSender;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DateTimeProvider;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.OutboxEventSender;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
class AuthContextWriteE2EConfiguration {

    @Bean
    public DateTimeProvider authTestDateTimeProvider() {
        return new DeterministicDateTimeProvider();
    }
//    @Primary
//    @Bean
//    public OutboxEventSender testOutboxEventSender() {
//        return new LoggingOutboxEventSender();
//    }
}
