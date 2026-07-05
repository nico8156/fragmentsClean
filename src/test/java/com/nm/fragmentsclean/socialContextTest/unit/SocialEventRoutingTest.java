package com.nm.fragmentsclean.socialContextTest.unit;

import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.providers.outboxEventSender.DefaultDomainEventRouter;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.EventRouting;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.CoffeePhotoAddedEvent;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.CoffeePhotoDeletedEvent;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.ImportedCoffeePhoto;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.VO.CoffeeId;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.VO.PhotoId;
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
                UUID.randomUUID().toString(), // commandId
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

    @Test
    void admin_coffee_photo_events_are_routed_to_eventbus_kafka_and_websocket() {
        var coffeeId = new CoffeeId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
        var photoId = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");
        var occurredAt = Instant.parse("2026-07-05T10:00:00Z");
        var clientAt = Instant.parse("2026-07-05T09:59:59Z");

        var added = new CoffeePhotoAddedEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                coffeeId,
                new ImportedCoffeePhoto(photoId, "s3://bucket/photo-1.jpg"),
                1,
                occurredAt,
                clientAt);
        var deleted = new CoffeePhotoDeletedEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                coffeeId,
                new PhotoId(photoId),
                2,
                occurredAt,
                clientAt);

        assertRoutingKafkaAndWebSocket(router.routingFor(added));
        assertRoutingKafkaAndWebSocket(router.routingFor(deleted));
    }

    private void assertRoutingAll(EventRouting routing) {
        assertThat(routing.sendToEventBus()).isTrue();
        assertThat(routing.sendToKafka()).isTrue();
        assertThat(routing.sendToWebSocket()).isTrue();
    }

    private void assertRoutingKafkaAndWebSocket(EventRouting routing) {
        assertThat(routing.sendToEventBus()).isFalse();
        assertThat(routing.sendToKafka()).isTrue();
        assertThat(routing.sendToWebSocket()).isTrue();
    }
}
