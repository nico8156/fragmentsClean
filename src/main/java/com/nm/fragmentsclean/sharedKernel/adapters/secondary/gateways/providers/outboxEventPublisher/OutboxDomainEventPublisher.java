package com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.providers.outboxEventPublisher;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.nm.fragmentsclean.aticleContext.write.businesslogic.models.ArticleCreatedEvent;
import com.nm.fragmentsclean.authContext.write.businesslogic.models.events.UserAuthenticatedEvent;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.repositories.jpa.SpringOutboxEventRepository;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.repositories.jpa.entities.OutboxEventJpaEntity;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DateTimeProvider;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEvent;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEventPublisher;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.OutboxStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Primary
@Component
public class OutboxDomainEventPublisher implements DomainEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxDomainEventPublisher.class);

    private final SpringOutboxEventRepository outboxRepository;
    private final ObjectMapper objectMapper;
    private final DateTimeProvider dateTimeProvider;

    public OutboxDomainEventPublisher(
            SpringOutboxEventRepository outboxRepository,
            ObjectMapper objectMapper,
            DateTimeProvider dateTimeProvider
    ) {
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
        this.dateTimeProvider = dateTimeProvider;
    }

    @Override
    public void publish(DomainEvent event) {
        log.info(">>> OutboxDomainEventPublisher.publish called with {}", event.getClass().getName());
        try {
            // 1. Payload JSON
            String payloadJson = objectMapper.writeValueAsString(event);

            // 2. Timestamps
            Instant occurredAt = event.occurredAt();
            Instant createdAt = dateTimeProvider.now();

            // 3. Type logique de l'event (FQCN)
            String eventType = event.getClass().getName();

            // 4. Routing / clé de stream
            String aggregateType;
            String aggregateId;
            String streamKey;

            if (event instanceof UserAuthenticatedEvent authEvent) {
                aggregateType = "User";
                aggregateId = authEvent.userId().toString();
                streamKey = "user:" + aggregateId;
            } else if (event instanceof ArticleCreatedEvent articleEvent) {
                aggregateType = "Article";
                aggregateId = articleEvent.articleId().toString();
                streamKey = "article:" + aggregateId;
            } else {
                aggregateType = "Unknown";
                aggregateId = "unknown";
                streamKey = "global";

                log.warn("Persisting domain event of unknown type in outbox: {}",
                        event.getClass().getName());
            }

            OutboxEventJpaEntity entity = new OutboxEventJpaEntity(
                    event.eventId().toString(),
                    eventType,
                    aggregateType,
                    aggregateId,
                    streamKey,
                    payloadJson,
                    occurredAt,
                    createdAt,
                    OutboxStatus.PENDING,
                    0 // retryCount initial
            );

            outboxRepository.save(entity);

            log.info(">>> OutboxDomainEventPublisher persisted eventId={} aggregateId={}",
                    event.eventId(), aggregateId);

            log.debug("Persisted outbox event: eventType={} aggregateType={} aggregateId={}",
                    eventType, aggregateType, aggregateId);

        } catch (JsonProcessingException e) {
            log.error("Failed to serialize domain event {} for outbox", event, e);
            throw new RuntimeException("Failed to serialize domain event for outbox", e);
        }
    }
}
