package com.nm.fragmentsclean.socialContextTest.unit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.providers.outboxEventSender.WebSocketOutboxEventSender;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.repositories.jpa.entities.OutboxEventJpaEntity;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.OutboxStatus;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class WebSocketOutboxEventSenderTest {

    @Test
    void sends_to_topic_with_streamKey() throws Exception {
        // GIVEN
        SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
        WebSocketOutboxEventSender sender =
                new WebSocketOutboxEventSender(messagingTemplate, new ObjectMapper());

        OutboxEventJpaEntity outboxEvent = new OutboxEventJpaEntity(
                "event-id-123",
                "com.nm.fragmentsclean.socialContext.write.businesslogic.models.LikeSetEvent",
                "Like",
                "f47b3b3b-3b3b-3b3b-3b3b-03b3b3b3b3b3",           // aggregateId
                "social:e67b3b3b-3b3b-3b3b-3b3b-03b3b3b3b3b3",      // streamKey
                "{\"some\":\"payload\"}",                           // ✅ string simple, pas de text block
                Instant.parse("2024-01-01T10:00:00Z"),
                Instant.parse("2024-01-01T10:00:00Z"),
                OutboxStatus.PENDING,
                0
        );

        // WHEN
        sender.send(outboxEvent);

        // THEN
        verify(messagingTemplate).convertAndSend(
                eq("/topic/social:e67b3b3b-3b3b-3b3b-3b3b-03b3b3b3b3b3"),
                anyString() // ou eq(outboxEvent.getPayloadJson()) si tu veux être strict
        );
    }
}
