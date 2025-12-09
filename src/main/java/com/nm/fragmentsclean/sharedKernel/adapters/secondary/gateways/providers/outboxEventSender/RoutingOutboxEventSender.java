package com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.providers.outboxEventSender;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.repositories.jpa.entities.OutboxEventJpaEntity;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEvent;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.EventRouting;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.gateways.DomainEventRouter;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.gateways.OutboxEventSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
public class RoutingOutboxEventSender implements OutboxEventSender {

    private static final Logger log = LoggerFactory.getLogger(RoutingOutboxEventSender.class);

    private final DomainEventRouter router;
    private final EventBusOutboxEventSender eventBusSender;
    private final KafkaOutboxEventSender kafkaSender;
    private final LoggingOutboxEventSender loggingSender;
    private final ObjectMapper objectMapper;
    private final WebSocketOutboxEventSender webSocketSender;


    public RoutingOutboxEventSender(
            DomainEventRouter router,
            EventBusOutboxEventSender eventBusSender,
            KafkaOutboxEventSender kafkaSender,
            LoggingOutboxEventSender loggingSender,
            WebSocketOutboxEventSender webSocketSender,
            ObjectMapper objectMapper
    ) {
        this.router = router;
        this.eventBusSender = eventBusSender;
        this.kafkaSender = kafkaSender;
        this.loggingSender = loggingSender;
        this.objectMapper = objectMapper;
        this.webSocketSender = webSocketSender;
    }

    @Override
    public void send(OutboxEventJpaEntity jpaEvent) throws Exception {
        DomainEvent domainEvent;

        try {
            // ⚠️ adapte ces getters aux vrais noms de champs
            String eventType = jpaEvent.getEventType();
            String payload = jpaEvent.getPayloadJson();

            Class<?> clazz = Class.forName(eventType);
            domainEvent = (DomainEvent) objectMapper.readValue(payload, clazz);
        } catch (Exception e) {
            log.error("Failed to deserialize outbox event id={} for routing, sending nowhere.",
                    jpaEvent.getId(), e);
            return;
        }

        EventRouting routing = router.routingFor(domainEvent);

        // logging : toujours
        if (loggingSender != null) {
            loggingSender.send(jpaEvent);
        }

        if (routing.sendToEventBus() && eventBusSender != null) {
            eventBusSender.send(jpaEvent);
        }

        if (routing.sendToKafka() && kafkaSender != null) {
            kafkaSender.send(jpaEvent);
        }
        if (routing.sendToWebSocket() && webSocketSender != null) {
            webSocketSender.send(jpaEvent);
        }

    }
}
