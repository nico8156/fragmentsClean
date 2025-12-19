package com.nm.fragmentsclean.ticketContext.read.configuration;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.nm.fragmentsclean.ticketContext.write.adapters.secondary.gateways.repositories.jpa.JpaTicketRepository;
import com.nm.fragmentsclean.ticketContext.write.adapters.secondary.gateways.repositories.jpa.SpringTicketRepository;
import com.nm.fragmentsclean.ticketContext.write.businesslogic.gateways.TicketRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TicketContextDependenciesConfiguration {

    @Bean
    public TicketRepository ticketRepository(SpringTicketRepository springTicketRepository, ObjectMapper objectMapper){
        return new JpaTicketRepository(springTicketRepository, objectMapper);
    }
}
