
package com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.CommandBus;
import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.EventBus;
import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.QueryBus;
import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.eventDispatcher.OutboxEventDispatcher;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.providers.outboxEventPublisher.OutboxDomainEventPublisher;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.repositories.jpa.JpaOutboxEventRepsitory;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.repositories.jpa.SpringOutboxEventRepository;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DateTimeProvider;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEventPublisher;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.gateways.OutboxEventSender;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.time.Instant;


@Configuration
@EntityScan(basePackages = "com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.repositories.jpa.entities")
@EnableJpaRepositories("com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.repositories.jpa")
@ComponentScan(basePackages = {
        "com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot",
        "com.nm.fragmentsclean.sharedKernel.adapters.secondary"
})
public class SharedKernelDependenciesConfiguration {

    @Bean
    public DomainEventPublisher domainEventPublisher(SpringOutboxEventRepository outboxRepo,
                                                     ObjectMapper objectMapper,
                                                     DateTimeProvider dateTimeProvider) {
        return new OutboxDomainEventPublisher(outboxRepo, objectMapper, dateTimeProvider);
    }

    @Bean
    public JpaOutboxEventRepsitory jpaOutboxEventRepository(SpringOutboxEventRepository springOutboxEventRepository) {
        return new JpaOutboxEventRepsitory(springOutboxEventRepository);
    }

    @Bean
    public OutboxEventDispatcher outboxEventDispatcher(SpringOutboxEventRepository outboxRepo,
                                                       OutboxEventSender sender) {
        return new OutboxEventDispatcher(outboxRepo, sender);
    }

    @Bean
    public CommandBus commandBus() {
        return new CommandBus();
    }
    @Bean
    public QueryBus queryBus()  {
        return new QueryBus();
    }
    @Bean
    public EventBus eventBus()  {return new EventBus();}

    @Bean
    public DateTimeProvider dateTimeProvider() {
        return Instant::now; // tu pourras mettre ta version déterministe en test
    }
    @Bean
    public ObjectMapper objectMapper() {
        // findAndRegisterModules() pour JavaTime, etc.
        return new ObjectMapper().findAndRegisterModules();
    }
}
