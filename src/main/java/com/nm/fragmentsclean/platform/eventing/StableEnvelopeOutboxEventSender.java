package com.nm.fragmentsclean.platform.eventing;

import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.repositories.jpa.entities.OutboxEventJpaEntity;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.providers.outboxEventSender.EventBusOutboxEventSender;
import com.nm.fragmentsclean.sharedKernel.businesslogic.eventing.IntegrationMessagePublisher;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.gateways.ClientAckOutboxEventSender;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.gateways.OutboxEventSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Primary
public class StableEnvelopeOutboxEventSender implements OutboxEventSender {

    private static final Logger log = LoggerFactory.getLogger(StableEnvelopeOutboxEventSender.class);

    private final EventBusOutboxEventSender eventBusSender;
    private final ClientAckOutboxEventSender webSocketSender;
    private final List<IntegrationMessagePublisher> publishers;
    private final IntegrationEventDestinationResolver destinationResolver;
    private final IntegrationEventEnvelopeFactory envelopeFactory;
    private final boolean localEventBusEnabled;

    public StableEnvelopeOutboxEventSender(
            EventBusOutboxEventSender eventBusSender,
            ClientAckOutboxEventSender webSocketSender,
            List<IntegrationMessagePublisher> publishers,
            @Value("${app.messaging.local-event-bus.enabled:true}") boolean localEventBusEnabled
    ) {
        this.eventBusSender = eventBusSender;
        this.webSocketSender = webSocketSender;
        this.publishers = publishers;
        this.destinationResolver = new IntegrationEventDestinationResolver();
        this.envelopeFactory = new IntegrationEventEnvelopeFactory();
        this.localEventBusEnabled = localEventBusEnabled;
    }

    @Override
    public void send(OutboxEventJpaEntity event) throws Exception {
        if (localEventBusEnabled) {
            eventBusSender.send(event);
        }

        for (String destination : destinationResolver.destinationsFor(event)) {
            var envelope = envelopeFactory.from(event, destination);
            for (IntegrationMessagePublisher publisher : publishers) {
                publisher.publish(envelope);
            }
        }

        sendWebSocketBestEffort(event);
    }

    private void sendWebSocketBestEffort(OutboxEventJpaEntity event) {
        try {
            webSocketSender.send(event);
        } catch (Exception e) {
            log.warn("[ws] opportunistic ack delivery failed for outboxId={} eventType={} error={}",
                    event.getId(), event.getEventType(), e.getMessage());
        }
    }
}
