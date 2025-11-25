package com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.providers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEvent;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEventPublisher;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.OutboxStatus;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.repositories.SpringOutboxEventRepository;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.repositories.jpa.entities.OutboxEventJpaEntity;
import com.nm.fragmentsclean.socialContext.write.businesslogic.models.LikeSetEvent;


import java.time.Instant;

public class OutboxDomainEventPublisher implements DomainEventPublisher {

    private final SpringOutboxEventRepository outboxRepo;
    private final ObjectMapper objectMapper;

    public OutboxDomainEventPublisher(SpringOutboxEventRepository outboxRepo,
                                      ObjectMapper objectMapper) {
        this.outboxRepo = outboxRepo;
        this.objectMapper = objectMapper;
    }

    @Override
    public void publish(DomainEvent event) {
        try {
            var payload = objectMapper.writeValueAsString(event);

            var outbox = new OutboxEventJpaEntity();
            outbox.setEventId(event.eventId().toString());
            outbox.setEventType(event.getClass().getSimpleName());
            outbox.setAggregateType(resolveAggregateType(event)); // ex "Like"
            outbox.setAggregateId(resolveAggregateId(event));     // ex likeId.toString()
            outbox.setStreamKey(resolveStreamKey(event));         // ex "user:{userId}"
            outbox.setPayloadJson(payload);
            outbox.setOccurredAt(event.occurredAt());
            outbox.setCreatedAt(Instant.now());
            outbox.setStatus(OutboxStatus.PENDING);
            outbox.setRetryCount(0);

            outboxRepo.save(outbox);
        } catch (JsonProcessingException e) {
            // tu peux logger / remonter l'erreur
            throw new RuntimeException(e);
        }
    }

    private String resolveAggregateType(DomainEvent event) {
        if (event instanceof LikeSetEvent) return "Like";
        // autres mappings
        return "Unknown";
    }

    private String resolveAggregateId(DomainEvent event) {
        if (event instanceof LikeSetEvent e) return e.likeId().toString();
        return "UNKNOWN";
    }

    private String resolveStreamKey(DomainEvent event) {
        if (event instanceof LikeSetEvent e) {
            // on peut choisir différentes clés de stream :
            return "target:" + e.targetId();  // ex: stream par café
            // ou "user:" + e.userId();
        }
        return "global";
    }
}
