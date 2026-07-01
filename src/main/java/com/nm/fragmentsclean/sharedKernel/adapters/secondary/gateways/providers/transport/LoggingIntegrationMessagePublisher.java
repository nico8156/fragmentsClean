package com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.providers.transport;

import com.nm.fragmentsclean.sharedKernel.businesslogic.eventing.IntegrationEventEnvelope;
import com.nm.fragmentsclean.sharedKernel.businesslogic.eventing.IntegrationMessagePublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.messaging.transport.logging.enabled", havingValue = "true", matchIfMissing = true)
public class LoggingIntegrationMessagePublisher implements IntegrationMessagePublisher {

    private static final Logger log = LoggerFactory.getLogger(LoggingIntegrationMessagePublisher.class);

    @Override
    public void publish(IntegrationEventEnvelope envelope) {
        log.info("[messaging] eventId={} type={} destination={} aggregateType={} aggregateId={}",
                envelope.eventId(),
                envelope.eventType(),
                envelope.destination(),
                envelope.aggregateType(),
                envelope.aggregateId());
    }
}
