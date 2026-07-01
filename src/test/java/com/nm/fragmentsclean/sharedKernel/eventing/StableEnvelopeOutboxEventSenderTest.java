package com.nm.fragmentsclean.sharedKernel.eventing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.EventBus;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.providers.outboxEventSender.EventBusOutboxEventSender;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.providers.outboxEventSender.StableEnvelopeOutboxEventSender;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.providers.outboxEventSender.WebSocketOutboxEventSender;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.repositories.jpa.entities.OutboxEventJpaEntity;
import com.nm.fragmentsclean.sharedKernel.businesslogic.eventing.IntegrationEventEnvelope;
import com.nm.fragmentsclean.sharedKernel.businesslogic.eventing.IntegrationMessagePublisher;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.OutboxStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class StableEnvelopeOutboxEventSenderTest {

    @Test
    void websocketFailureDoesNotFailTransportPublication() throws Exception {
        EventBusOutboxEventSender eventBus = new EventBusOutboxEventSender(new ObjectMapper(), new EventBus());
        WebSocketOutboxEventSender webSocket = new FailingWebSocketOutboxEventSender();

        var published = new ArrayList<IntegrationEventEnvelope>();
        IntegrationMessagePublisher publisher = published::add;

        var sender = new StableEnvelopeOutboxEventSender(
                eventBus,
                webSocket,
                List.of(publisher),
                false);

        sender.send(new OutboxEventJpaEntity(
                "evt-1",
                "com.nm.fragmentsclean.socialContext.write.businesslogic.models.CommentCreatedEvent",
                "Comment",
                "comment-1",
                "user:user-1",
                "{}",
                Instant.now(),
                Instant.now(),
                OutboxStatus.PENDING,
                0));

        assertThat(published).hasSize(1);
        assertThat(published.getFirst().eventType()).isEqualTo("social.comment.created");
    }

    private static class FailingWebSocketOutboxEventSender extends WebSocketOutboxEventSender {
        FailingWebSocketOutboxEventSender() {
            super(null, new ObjectMapper());
        }

        @Override
        public void send(OutboxEventJpaEntity outboxEvent) {
            throw new IllegalStateException("ws down");
        }
    }
}
