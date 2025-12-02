package com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.providers;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.nm.fragmentsclean.aticleContext.write.businesslogic.models.ArticleCreatedEvent;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.repositories.jpa.entities.OutboxEventJpaEntity;
import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.EventBus;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEvent;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.OutboxEventSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class EventBusOutboxEventSender implements OutboxEventSender {

    private static final Logger log = LoggerFactory.getLogger(EventBusOutboxEventSender.class);

    private final ObjectMapper objectMapper;
    private final EventBus eventBus;

    public EventBusOutboxEventSender(ObjectMapper objectMapper,
                                     EventBus eventBus) {
        this.objectMapper = objectMapper;
        this.eventBus = eventBus;
    }

    @Override
    public void send(OutboxEventJpaEntity entity) {
        String type = entity.getEventType();
        String payload = entity.getPayloadJson();

        DomainEvent domainEvent = mapToDomainEvent(type, payload);

        if (domainEvent == null) {
            log.warn("Unhandled outbox event type={} id={}", type, entity.getId());
            return;
        }

        eventBus.publish(domainEvent);
    }

    private DomainEvent mapToDomainEvent(String type, String payload) {
        try {
            return switch (type) {
                case "ArticleCreatedEvent" -> read(payload, ArticleCreatedEvent.class);
                // case "CommentCreatedEvent" -> read(payload, CommentCreatedEvent.class);
                default -> null;
            };
        } catch (Exception e) {
            throw new IllegalStateException("Failed to deserialize outbox event of type " + type, e);
        }
    }

    private <T extends DomainEvent> T read(String payload, Class<T> clazz) throws Exception {
        return objectMapper.readValue(payload, clazz);
    }
}
