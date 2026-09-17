
package com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.CommandBus;
import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.EventBus;
import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.QueryBus;
import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.eventDispatcher.OutboxEventDispatcher;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.providers.outboxEventPublisher.OutboxDomainEventPublisher;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.repositories.jdbc.CommandStatusRepository;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.repositories.jdbc.JdbcOutboxDeliveryStore;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.repositories.jpa.SpringOutboxEventRepository;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DateTimeProvider;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEventPublisher;
import com.nm.fragmentsclean.sharedKernel.businesslogic.eventing.OutboxEventMetadataResolver;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.gateways.OutboxEventSender;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.gateways.OutboxDeliveryStore;
import com.nm.fragmentsclean.sharedKernel.businesslogic.eventing.OutboxRetryPolicy;
import com.nm.fragmentsclean.sharedKernel.businesslogic.commandStatus.CommandFingerprint;
import com.nm.fragmentsclean.sharedKernel.businesslogic.commandStatus.CommandReceiptStore;
import com.nm.fragmentsclean.sharedKernel.businesslogic.commandStatus.CommandTransaction;
import com.nm.fragmentsclean.sharedKernel.businesslogic.commandStatus.DurableCommandExecutor;
import com.nm.fragmentsclean.sharedKernel.businesslogic.privacy.AccountErasureBarrier;
import com.nm.fragmentsclean.sharedKernel.businesslogic.privacy.PersonalDataResidueStore;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.repositories.jdbc.JdbcPersonalDataResidueStore;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.repositories.jdbc.JdbcAccountErasureBarrier;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.beans.factory.annotation.Value;

import java.time.Duration;
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
    public PersonalDataResidueStore personalDataResidueStore(org.springframework.jdbc.core.JdbcTemplate jdbc) {
        return new JdbcPersonalDataResidueStore(jdbc);
    }

    @Bean
    public DomainEventPublisher domainEventPublisher(SpringOutboxEventRepository outboxRepo,
                                                     ObjectMapper objectMapper,
                                                     DateTimeProvider dateTimeProvider,
                                                     OutboxEventMetadataResolver metadataResolver) {
        return new OutboxDomainEventPublisher(outboxRepo, objectMapper, dateTimeProvider, metadataResolver);
    }

    @Bean
    public OutboxDeliveryStore outboxDeliveryStore(JdbcTemplate jdbcTemplate,
                                                   CommandStatusRepository commandStatusRepository,
                                                   PlatformTransactionManager transactionManager) {
        return new JdbcOutboxDeliveryStore(jdbcTemplate, commandStatusRepository, transactionManager);
    }

    @Bean
    public OutboxEventDispatcher outboxEventDispatcher(
            OutboxDeliveryStore deliveryStore,
            OutboxEventSender sender,
            DateTimeProvider dateTimeProvider,
            @Value("${app.outbox.dispatcher.max-failures:10}") int maxFailures,
            @Value("${app.outbox.dispatcher.base-delay-ms:1000}") long baseDelayMs,
            @Value("${app.outbox.dispatcher.max-delay-ms:300000}") long maxDelayMs,
            @Value("${app.outbox.dispatcher.lease-ms:120000}") long leaseMs,
            @Value("${app.outbox.dispatcher.batch-size:10}") int batchSize,
            @Value("${HOSTNAME:${random.uuid}}") String workerId) {
        return new OutboxEventDispatcher(deliveryStore, sender, dateTimeProvider,
                new OutboxRetryPolicy(maxFailures, Duration.ofMillis(baseDelayMs), Duration.ofMillis(maxDelayMs)),
                Duration.ofMillis(leaseMs), batchSize, workerId);
    }

    @Bean
    public DurableCommandExecutor durableCommandExecutor(
            CommandReceiptStore receipts,
            CommandFingerprint fingerprint,
            CommandTransaction transactions,
            DateTimeProvider dateTimeProvider,
            AccountErasureBarrier erasureBarrier) {
        return new DurableCommandExecutor(receipts, fingerprint, transactions, dateTimeProvider, erasureBarrier);
    }

    @Bean
    public AccountErasureBarrier accountErasureBarrier(
            JdbcTemplate jdbcTemplate, PlatformTransactionManager transactionManager) {
        return new JdbcAccountErasureBarrier(jdbcTemplate, transactionManager);
    }

    @Bean
    public CommandBus commandBus(DurableCommandExecutor durableCommandExecutor) {
        return new CommandBus(durableCommandExecutor);
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
        return new ObjectMapper()
                .findAndRegisterModules()
                .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }
}
