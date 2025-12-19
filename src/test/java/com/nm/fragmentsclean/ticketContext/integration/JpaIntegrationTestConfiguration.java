package com.nm.fragmentsclean.ticketContext.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nm.fragmentsclean.ticketContext.write.adapters.secondary.gateways.repositories.jpa.JpaTicketRepository;
import com.nm.fragmentsclean.ticketContext.write.adapters.secondary.gateways.repositories.jpa.SpringTicketRepository;
import com.nm.fragmentsclean.ticketContext.write.businesslogic.gateways.TicketRepository;
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
