package com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.providers.outboxEventSender;

import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.repositories.jpa.entities.OutboxEventJpaEntity;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.gateways.OutboxEventSender;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;


public class CompositeOutboxEventSender implements OutboxEventSender {

    private final List<OutboxEventSender> delegates;

    public CompositeOutboxEventSender(
            EventBusOutboxEventSender eventBusSender,
            LoggingOutboxEventSender loggingOutboxEventSender,
            KafkaOutboxEventSender kafkaOutboxEventSender // optionnel
            // tu peux en rajouter un troisième ex: LoggingOutboxEventSender si tu veux
    ) {
        var list = new java.util.ArrayList<OutboxEventSender>();
        list.add(eventBusSender);
        list.add(loggingOutboxEventSender);
        if (kafkaOutboxEventSender != null) {
            list.add(kafkaOutboxEventSender);
        }
        this.delegates = java.util.List.copyOf(list);
    }

    @Override
    public void send(OutboxEventJpaEntity event) throws Exception {
        for (OutboxEventSender delegate : delegates) {
            delegate.send(event);
        }
    }
}
