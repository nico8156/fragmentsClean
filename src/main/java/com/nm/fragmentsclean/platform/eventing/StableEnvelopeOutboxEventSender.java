package com.nm.fragmentsclean.platform.eventing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.repositories.jpa.entities.OutboxEventJpaEntity;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.providers.outboxEventSender.EventBusOutboxEventSender;
import com.nm.fragmentsclean.sharedKernel.businesslogic.eventing.IntegrationMessagePublisher;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.gateways.OutboxEventSender;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Primary
public class StableEnvelopeOutboxEventSender implements OutboxEventSender {

    private final EventBusOutboxEventSender eventBusSender;
    private final List<IntegrationMessagePublisher> publishers;
    private final IntegrationEventDestinationResolver destinationResolver;
    private final IntegrationEventEnvelopeFactory envelopeFactory;
    private final boolean localEventBusEnabled;

    public StableEnvelopeOutboxEventSender(
            EventBusOutboxEventSender eventBusSender,
            List<IntegrationMessagePublisher> publishers,
            ObjectMapper objectMapper,
            @Value("${app.messaging.local-event-bus.enabled:true}") boolean localEventBusEnabled
    ) {
        this.eventBusSender = eventBusSender;
        this.publishers = publishers;
        this.destinationResolver = new IntegrationEventDestinationResolver();
        this.envelopeFactory = new IntegrationEventEnvelopeFactory(objectMapper);
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
    }
}
