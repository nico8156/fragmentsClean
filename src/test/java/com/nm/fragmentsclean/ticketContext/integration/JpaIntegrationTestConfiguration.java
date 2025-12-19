package com.nm.fragmentsclean.ticketContext.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
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
public class JpaIntegrationTestConfiguration {
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    public TicketRepository ticketRepository(SpringTicketRepository springTicketRepository, ObjectMapper objectMapper) {
        return new JpaTicketRepository(springTicketRepository, objectMapper);
    }

}
