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
        // même principe que pour le social : horloge stable
        return new DeterministicDateTimeProvider();
        // ou, si tu préfères :
        // return () -> Instant.parse("2024-01-01T10:00:00Z");
    }
    @Primary
    @Bean
    public OutboxEventSender testOutboxEventSender() {
        // ⚠️ adapte la signature au vrai contrat !
        // Exemple si ton interface ressemble à : void send(OutboxEvent event)
        return new LoggingOutboxEventSender();
    }

    // ⚠️ NE PAS redéclarer ici IdentityRepository / UserRepository / RefreshSessionCommandHandler
    // Ils viennent de AuthContextDependenciesConfiguration (main)
    // qui est déjà prise dans le component scan de FragmentsCleanApplication.
}
