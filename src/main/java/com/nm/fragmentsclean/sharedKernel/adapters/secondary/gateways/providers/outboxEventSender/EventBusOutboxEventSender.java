package com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.providers.outboxEventSender;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.EventBus;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.repositories.jpa.entities.OutboxEventJpaEntity;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEvent;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.gateways.OutboxEventSender;
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
    public void send(OutboxEventJpaEntity entity) throws Exception {
        String type = entity.getEventType();
        String payload = entity.getPayloadJson();

        DomainEvent domainEvent = mapToDomainEvent(type, payload);

        if (domainEvent == null) {
            log.warn("Unhandled outbox event type={} id={}", type, entity.getId());
            return;
        }
        System.out.println("domainEvent = " + domainEvent);
        // 👉👉👉 C’EST ICI QUE L’EVENTBUS EST UTILISÉ 👈👈👈
        eventBus.publish(domainEvent);
    }

    private DomainEvent mapToDomainEvent(String type, String payload) {
        try {
            Class<?> clazz = Class.forName(type);

            if (!DomainEvent.class.isAssignableFrom(clazz)) {
                throw new IllegalStateException("Type " + type + " is not a DomainEvent");
            }

            @SuppressWarnings("unchecked")
            Class<? extends DomainEvent> eventClass =
                    (Class<? extends DomainEvent>) clazz;

            return objectMapper.readValue(payload, eventClass);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to deserialize outbox event of type " + type, e);
        }
    }
}
