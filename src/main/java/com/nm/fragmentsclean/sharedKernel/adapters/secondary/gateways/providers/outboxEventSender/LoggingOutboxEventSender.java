package com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.providers.outboxEventSender;

import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.repositories.jpa.entities.OutboxEventJpaEntity;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.OutboxEventSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;


@Component
public class LoggingOutboxEventSender implements OutboxEventSender {
    private static final Logger log = LoggerFactory.getLogger(LoggingOutboxEventSender.class);

    @Override
    public void send(OutboxEventJpaEntity event) throws Exception {
        log.info("Sending outbox event id={} type={} streamKey={} payload={}",
                event.getId(),
                event.getEventType(),
                event.getStreamKey(),
                event.getPayloadJson());
    }
}
