package com.nm.fragmentsclean.socialContextTest.unit;

import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.providers.outboxEventSender.DefaultDomainEventRouter;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.EventRouting;
import com.nm.fragmentsclean.socialContext.write.businesslogic.models.CommentCreatedEvent;
import com.nm.fragmentsclean.socialContext.write.businesslogic.models.CommentDeletedEvent;
import com.nm.fragmentsclean.socialContext.write.businesslogic.models.CommentUpdatedEvent;
import com.nm.fragmentsclean.socialContext.write.businesslogic.models.LikeSetEvent;
import com.nm.fragmentsclean.socialContext.write.businesslogic.models.ModerationStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SocialEventRoutingTest {

    private final DefaultDomainEventRouter router = new DefaultDomainEventRouter();

    @Test
    void comment_events_are_routed_to_eventbus_kafka_and_websocket() {
        var eventId   = UUID.randomUUID();
        var commandId = UUID.randomUUID();
        var commentId = UUID.randomUUID();
        var targetId  = UUID.randomUUID();
        var parentId  = (UUID) null;
        var authorId  = UUID.randomUUID();

        var occurredAt = Instant.parse("2024-01-01T10:00:00Z");
        var clientAt   = Instant.parse("2024-01-01T09:00:00Z");

        // CommentCreatedEvent
        var created = new CommentCreatedEvent(
                eventId,
                commandId,
                commentId,
                targetId,
                parentId,
                authorId,
                "Hello world",
                ModerationStatus.PUBLISHED,
                0L,
                occurredAt,
                clientAt
        );

        // CommentUpdatedEvent → j’imagine une signature proche de CommentCreatedEvent
        // 👉 à adapter à ta vraie signature si elle diffère
        var updated = new CommentUpdatedEvent(
                eventId,
                commandId,
                commentId,
                targetId,
                authorId,
                "Hello world (edited)",
                ModerationStatus.PUBLISHED,
                1L,
                occurredAt,
                clientAt
        );

        // CommentDeletedEvent (avec ta vraie signature)
        var deleted = new CommentDeletedEvent(
                eventId,
                commandId,
                commentId,
                targetId,
                authorId,
                ModerationStatus.PUBLISHED,
                occurredAt,  // deletedAt
                2L,          // version
                occurredAt,
                clientAt
        );

        assertRoutingAll(router.routingFor(created));
        assertRoutingAll(router.routingFor(updated));
        assertRoutingAll(router.routingFor(deleted));
    }

    @Test
    void like_events_are_routed_to_eventbus_kafka_and_websocket() {
        var likeEvent = new LikeSetEvent(
                UUID.randomUUID(), // eventId
                UUID.randomUUID(), // commandId
                UUID.randomUUID(), // likeId
                UUID.randomUUID(), // userId
                UUID.randomUUID(), // targetId
                true,              // active
                1,                 // count
                1,                 // version
                Instant.parse("2024-01-01T10:00:00Z"),
                Instant.parse("2024-01-01T09:00:00Z")
        );

        assertRoutingAll(router.routingFor(likeEvent));
    }

    private void assertRoutingAll(EventRouting routing) {
        assertThat(routing.sendToEventBus()).isTrue();
        assertThat(routing.sendToKafka()).isTrue();
        assertThat(routing.sendToWebSocket()).isTrue();
    }
}
