package com.nm.fragmentsclean.sharedKernel.eventing;

import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.repositories.jpa.entities.OutboxEventJpaEntity;
import com.nm.fragmentsclean.sharedKernel.businesslogic.eventing.IntegrationEventDestinationResolver;
import com.nm.fragmentsclean.sharedKernel.businesslogic.eventing.IntegrationEventEnvelopeFactory;
import com.nm.fragmentsclean.sharedKernel.businesslogic.eventing.IntegrationEventDestinations;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.OutboxStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class IntegrationEventEnvelopeFactoryTest {

    @Test
    void buildsStableEnvelopeWithoutLeakingJavaClassAsEventType() {
        var outbox = new OutboxEventJpaEntity(
                "evt-1",
                "com.nm.fragmentsclean.ticketContext.write.businesslogic.models.TicketVerificationCompletedEvent",
                "Ticket",
                "ticket-1",
                "user:user-1",
                "{\"commandId\":\"00000000-0000-0000-0000-000000000001\"}",
                Instant.parse("2026-01-01T10:00:00Z"),
                Instant.parse("2026-01-01T10:00:01Z"),
                OutboxStatus.PENDING,
                0);

        var envelope = new IntegrationEventEnvelopeFactory()
                .from(outbox, IntegrationEventDestinations.TICKET_EVENTS);

        assertThat(envelope.eventType()).isEqualTo("ticket.verification.completed");
        assertThat(envelope.eventVersion()).isEqualTo(1);
        assertThat(envelope.sourceEventClassName()).endsWith("TicketVerificationCompletedEvent");
        assertThat(envelope.destination()).isEqualTo("ticket-events");
        assertThat(envelope.payloadJson()).contains("commandId");
    }

    @Test
    void routesTicketAcceptedToReadProjectionAndWorkerDestinations() {
        var outbox = new OutboxEventJpaEntity(
                "evt-2",
                "com.nm.fragmentsclean.ticketContext.write.businesslogic.models.TicketVerifyAcceptedEvent",
                "Ticket",
                "ticket-1",
                "user:user-1",
                "{}",
                Instant.now(),
                Instant.now(),
                OutboxStatus.PENDING,
                0);

        assertThat(new IntegrationEventDestinationResolver().destinationsFor(outbox))
                .containsExactly("ticket-events", "ticket-verification-requested");
    }

    @Test
    void routesCoffeeOpeningHoursImportedToCoffeeEventsWithStableType() {
        var outbox = new OutboxEventJpaEntity(
                "evt-3",
                "com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.CoffeeOpeningHoursImportedEvent",
                "Coffee",
                "coffee-1",
                "coffee:coffee-1",
                "{\"weekdayDescriptions\":[]}",
                Instant.parse("2026-07-04T10:00:00Z"),
                Instant.parse("2026-07-04T10:00:01Z"),
                OutboxStatus.PENDING,
                0);

        assertThat(new IntegrationEventDestinationResolver().destinationsFor(outbox))
                .containsExactly("coffees-events");

        var envelope = new IntegrationEventEnvelopeFactory()
                .from(outbox, IntegrationEventDestinations.COFFEES_EVENTS);

        assertThat(envelope.eventType()).isEqualTo("coffee.opening_hours_imported");
        assertThat(envelope.eventVersion()).isEqualTo(1);
        assertThat(envelope.destination()).isEqualTo("coffees-events");
    }

    @Test
    void routesCoffeePhotosImportedToCoffeeEventsWithStableType() {
        var outbox = new OutboxEventJpaEntity(
                "evt-4",
                "com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.CoffeePhotosImportedEvent",
                "Coffee",
                "coffee-1",
                "coffee:coffee-1",
                "{\"photos\":[]}",
                Instant.parse("2026-07-04T10:00:00Z"),
                Instant.parse("2026-07-04T10:00:01Z"),
                OutboxStatus.PENDING,
                0);

        assertThat(new IntegrationEventDestinationResolver().destinationsFor(outbox))
                .containsExactly("coffees-events");

        var envelope = new IntegrationEventEnvelopeFactory()
                .from(outbox, IntegrationEventDestinations.COFFEES_EVENTS);

        assertThat(envelope.eventType()).isEqualTo("coffee.photos_imported");
        assertThat(envelope.eventVersion()).isEqualTo(1);
        assertThat(envelope.destination()).isEqualTo("coffees-events");
    }

    @Test
    void routesCoffeeArchivedToCoffeeEventsWithStableType() {
        var outbox = new OutboxEventJpaEntity(
                "evt-archived",
                "com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.CoffeeArchivedEvent",
                "Coffee",
                "coffee-1",
                "coffee:coffee-1",
                "{\"coffeeId\":{\"value\":\"11111111-1111-1111-1111-111111111111\"}}",
                Instant.parse("2026-07-04T10:00:00Z"),
                Instant.parse("2026-07-04T10:00:01Z"),
                OutboxStatus.PENDING,
                0);

        assertThat(new IntegrationEventDestinationResolver().destinationsFor(outbox))
                .containsExactly("coffees-events");

        var envelope = new IntegrationEventEnvelopeFactory()
                .from(outbox, IntegrationEventDestinations.COFFEES_EVENTS);

        assertThat(envelope.eventType()).isEqualTo("coffee.archived");
        assertThat(envelope.eventVersion()).isEqualTo(1);
        assertThat(envelope.destination()).isEqualTo("coffees-events");
    }

    @Test
    void routesCoffeeDeletedToCoffeeEventsWithStableType() {
        var outbox = new OutboxEventJpaEntity(
                "evt-5",
                "com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.CoffeeDeletedEvent",
                "Coffee",
                "coffee-1",
                "coffee:coffee-1",
                "{\"coffeeId\":{\"value\":\"11111111-1111-1111-1111-111111111111\"}}",
                Instant.parse("2026-07-04T10:00:00Z"),
                Instant.parse("2026-07-04T10:00:01Z"),
                OutboxStatus.PENDING,
                0);

        assertThat(new IntegrationEventDestinationResolver().destinationsFor(outbox))
                .containsExactly("coffees-events");

        var envelope = new IntegrationEventEnvelopeFactory()
                .from(outbox, IntegrationEventDestinations.COFFEES_EVENTS);

        assertThat(envelope.eventType()).isEqualTo("coffee.deleted");
        assertThat(envelope.eventVersion()).isEqualTo(1);
        assertThat(envelope.destination()).isEqualTo("coffees-events");
    }

    @Test
    void routesCoffeePhotoManagementEventsToCoffeeEventsWithStableTypes() {
        var added = new OutboxEventJpaEntity(
                "evt-6",
                "com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.CoffeePhotoAddedEvent",
                "Coffee",
                "coffee-1",
                "coffee:coffee-1",
                "{\"photo\":{}}",
                Instant.parse("2026-07-05T10:00:00Z"),
                Instant.parse("2026-07-05T10:00:01Z"),
                OutboxStatus.PENDING,
                0);
        var deleted = new OutboxEventJpaEntity(
                "evt-7",
                "com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.CoffeePhotoDeletedEvent",
                "Coffee",
                "coffee-1",
                "coffee:coffee-1",
                "{\"photoId\":{\"value\":\"22222222-2222-2222-2222-222222222222\"}}",
                Instant.parse("2026-07-05T10:00:00Z"),
                Instant.parse("2026-07-05T10:00:01Z"),
                OutboxStatus.PENDING,
                0);

        assertThat(new IntegrationEventDestinationResolver().destinationsFor(added))
                .containsExactly("coffees-events");
        assertThat(new IntegrationEventDestinationResolver().destinationsFor(deleted))
                .containsExactly("coffees-events");

        var factory = new IntegrationEventEnvelopeFactory();
        assertThat(factory.from(added, IntegrationEventDestinations.COFFEES_EVENTS).eventType())
                .isEqualTo("coffee.photo_added");
        assertThat(factory.from(deleted, IntegrationEventDestinations.COFFEES_EVENTS).eventType())
                .isEqualTo("coffee.photo_deleted");
    }
}
