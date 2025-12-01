package com.nm.fragmentsclean.socialContextTest.endtoend.adapters.primary.springboot.controllers;

import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.security.FakeCurrentUserProvider;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.providers.DeterministicDateTimeProvider;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.providers.LoggingOutboxEventSender;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.CurrentUserProvider;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DateTimeProvider;

import com.nm.fragmentsclean.sharedKernel.businesslogic.models.OutboxEventSender;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import org.springframework.context.annotation.Primary;

import java.util.UUID;

@TestConfiguration
public class SocialContextWriteE2EConfiguration {
    @Primary
    @Bean
    public DateTimeProvider deterministicClockProvider() {
        return new DeterministicDateTimeProvider();
    }
    @Primary
    @Bean
    public OutboxEventSender testOutboxEventSender() {
        // ⚠️ adapte la signature au vrai contrat !
        // Exemple si ton interface ressemble à : void send(OutboxEvent event)
        return new LoggingOutboxEventSender();
    }
    @Primary
    @Bean
    public CurrentUserProvider testCurrentUserProvider() {
        // l'utilisateur "me" utilisé dans tes tests E2E
        return new FakeCurrentUserProvider();
    }

}
