package com.nm.fragmentsclean.ticketContext.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.providers.DeterministicDateTimeProvider;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.providers.outboxEventPublisher.OutboxDomainEventPublisher;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.repositories.jpa.SpringOutboxEventRepository;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DateTimeProvider;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEventPublisher;
import com.nm.fragmentsclean.ticketContext.write.adapters.secondary.gateways.repositories.jpa.JpaTicketRepository;
import com.nm.fragmentsclean.ticketContext.write.adapters.secondary.gateways.repositories.jpa.SpringTicketRepository;
import com.nm.fragmentsclean.ticketContext.write.businesslogic.gateways.TicketRepository;
import com.nm.fragmentsclean.ticketContext.write.businesslogic.usecases.VerifyTicketCommandHandler;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;


@TestConfiguration
public class TicketOutboxJpaIntegrationTestConfiguration {

    @Bean
    public ObjectMapper objectMapperForTest() {
        return new ObjectMapper();
    }

    @Bean
    public DateTimeProvider dateTimeProviderForTest() {
        var p = new DeterministicDateTimeProvider();
        p.instantOfNow = java.time.Instant.parse("2024-01-01T10:00:00Z");
        return p;
    }

    @Bean
    public TicketRepository ticketRepository(SpringTicketRepository springTicketRepository, ObjectMapper objectMapper) {
        return new JpaTicketRepository(springTicketRepository, objectMapper);
    }

    @Bean
    public DomainEventPublisher testOutboxDomainEventPublisher(SpringOutboxEventRepository outboxRepository,
                                                     ObjectMapper objectMapper,
                                                     DateTimeProvider dateTimeProvider) {
        return new OutboxDomainEventPublisher(outboxRepository, objectMapper, dateTimeProvider);
    }

    @Bean
    public VerifyTicketCommandHandler verifyTicketCommandHandler(TicketRepository ticketRepository,
                                                                 DomainEventPublisher domainEventPublisher,
                                                                 DateTimeProvider dateTimeProvider) {
        return new VerifyTicketCommandHandler(ticketRepository, domainEventPublisher, dateTimeProvider);
    }
}
