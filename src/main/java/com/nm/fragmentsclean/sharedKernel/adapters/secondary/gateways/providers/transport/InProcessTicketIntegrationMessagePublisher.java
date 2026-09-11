package com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.providers.transport;

import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.sqs.SqsIntegrationEventRouting;
import com.nm.fragmentsclean.sharedKernel.businesslogic.eventing.IntegrationEventEnvelope;
import com.nm.fragmentsclean.sharedKernel.businesslogic.eventing.IntegrationMessagePublisher;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Transitional in-process transport for the Ticket stream, whose consumers now
 * use the same stable envelopes and inbox semantics locally and through SQS.
 */
@Component
@Order(0)
@ConditionalOnProperty(name = "app.messaging.local-event-bus.enabled", havingValue = "true", matchIfMissing = true)
public class InProcessTicketIntegrationMessagePublisher implements IntegrationMessagePublisher {
    private final SqsIntegrationEventRouting routing;

    public InProcessTicketIntegrationMessagePublisher(SqsIntegrationEventRouting routing) {
        this.routing = routing;
    }

    @Override
    public void publish(IntegrationEventEnvelope envelope) {
        if ("Ticket".equals(envelope.aggregateType())) routing.route(envelope);
    }
}
