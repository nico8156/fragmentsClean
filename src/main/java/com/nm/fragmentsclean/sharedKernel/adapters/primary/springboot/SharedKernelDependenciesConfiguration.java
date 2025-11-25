
package com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.providers.OutboxDomainEventPublisher;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.repositories.SpringOutboxEventRepository;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DateTimeProvider;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEventPublisher;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.OutboxEventSender;
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
                                                     ObjectMapper objectMapper) {
        return new OutboxDomainEventPublisher(outboxRepo, objectMapper);
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
    public DateTimeProvider dateTimeProvider() {
        return Instant::now; // tu pourras mettre ta version déterministe en test
    }
}
