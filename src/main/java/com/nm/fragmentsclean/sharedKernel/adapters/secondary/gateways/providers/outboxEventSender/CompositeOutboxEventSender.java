package com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.providers.outboxEventSender;

import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.repositories.jpa.entities.OutboxEventJpaEntity;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.OutboxEventSender;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Primary
public class CompositeOutboxEventSender implements OutboxEventSender {

    private final List<OutboxEventSender> delegates;

    public CompositeOutboxEventSender(
            EventBusOutboxEventSender eventBusSender,
            LoggingOutboxEventSender loggingOutboxEventSender
            // tu peux en rajouter un troisième ex: LoggingOutboxEventSender si tu veux
    ) {
        this.delegates = List.of(
                eventBusSender,
                loggingOutboxEventSender
        );
    }

    @Override
    public void send(OutboxEventJpaEntity event) throws Exception {
        for (OutboxEventSender delegate : delegates) {
            delegate.send(event);
        }
    }
}
