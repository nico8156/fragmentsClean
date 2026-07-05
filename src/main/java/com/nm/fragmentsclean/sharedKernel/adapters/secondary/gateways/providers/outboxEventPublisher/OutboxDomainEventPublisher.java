package com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.providers.outboxEventPublisher;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.repositories.jpa.SpringOutboxEventRepository;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.repositories.jpa.entities.OutboxEventJpaEntity;
import com.nm.fragmentsclean.sharedKernel.businesslogic.eventing.OutboxEventMetadata;
import com.nm.fragmentsclean.sharedKernel.businesslogic.eventing.OutboxEventMetadataResolver;
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
    private final OutboxEventMetadataResolver metadataResolver;

    public OutboxDomainEventPublisher(
            SpringOutboxEventRepository outboxRepository,
            ObjectMapper objectMapper,
            DateTimeProvider dateTimeProvider,
            OutboxEventMetadataResolver metadataResolver
    ) {
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
        this.dateTimeProvider = dateTimeProvider;
        this.metadataResolver = metadataResolver;
    }

    @Override
    public void publish(DomainEvent event) {
        try {
            ObjectMapper om = objectMapper.copy()
                    .registerModule(new JavaTimeModule())
                    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                    .disable(SerializationFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS);

            String payloadJson = om.writeValueAsString(event);

            Instant occurredAt = event.occurredAt();
            Instant createdAt = dateTimeProvider.now();

            String eventType = event.getClass().getName();
            OutboxEventMetadata metadata = metadataResolver.resolve(event);

            OutboxEventJpaEntity entity = new OutboxEventJpaEntity(
                    event.eventId().toString(),
                    eventType,
                    metadata.aggregateType(),
                    metadata.aggregateId(),
                    metadata.streamKey(),
                    payloadJson,
                    occurredAt,
                    createdAt,
                    OutboxStatus.PENDING,
                    0 // retryCount initial
            );

            outboxRepository.save(entity);

            log.info(">>> OutboxDomainEventPublisher persisted eventId={} aggregateId={}",
                    event.eventId(), metadata.aggregateId());

            log.debug("Persisted outbox event: eventType={} aggregateType={} aggregateId={}",
                    eventType, metadata.aggregateType(), metadata.aggregateId());

        } catch (JsonProcessingException e) {
            log.error("Failed to serialize domain event type={} for outbox", event.getClass().getName(), e);
            throw new RuntimeException("Failed to serialize domain event for outbox", e);
        }
    }
}
