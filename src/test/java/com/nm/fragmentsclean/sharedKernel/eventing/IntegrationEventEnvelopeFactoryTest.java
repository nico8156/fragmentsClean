package com.nm.fragmentsclean.sharedKernel.eventing;

import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.repositories.jpa.entities.OutboxEventJpaEntity;
import com.nm.fragmentsclean.platform.eventing.IntegrationEventDestinationResolver;
import com.nm.fragmentsclean.platform.eventing.IntegrationEventEnvelopeFactory;
import com.nm.fragmentsclean.platform.eventing.IntegrationEventDestinations;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.OutboxStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nm.fragmentsclean.platform.eventing.contracts.ArticleCreatedIntegrationEvent;
import com.nm.fragmentsclean.platform.eventing.contracts.SocialCommentIntegrationEvents;
import com.nm.fragmentsclean.platform.eventing.contracts.TicketIntegrationEvents;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class IntegrationEventEnvelopeFactoryTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

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
    void mapsAuthUserDomainPayloadToPublicIntegrationPayload() {
        var outbox = new OutboxEventJpaEntity(
                "11111111-1111-1111-1111-111111111111",
                "com.nm.fragmentsclean.authenticationContext.write.businesslogic.models.AuthUserCreatedEvent",
                "AuthUser",
                "22222222-2222-2222-2222-222222222222",
                "authUser:22222222-2222-2222-2222-222222222222",
                """
                        {
                          "eventId":"11111111-1111-1111-1111-111111111111",
                          "authUserId":"22222222-2222-2222-2222-222222222222",
                          "provider":"GOOGLE",
                          "providerUserId":"google-user",
                          "email":"user@example.test",
                          "emailVerified":true,
                          "displayName":"Test User",
                          "avatarUrl":"https://example.test/avatar.png",
                          "occurredAt":"2026-07-06T08:00:00Z"
                        }
                        """,
                Instant.parse("2026-07-06T08:00:00Z"),
                Instant.parse("2026-07-06T08:00:01Z"),
                OutboxStatus.PENDING,
                0);

        var envelope = new IntegrationEventEnvelopeFactory()
                .from(outbox, IntegrationEventDestinations.AUTH_USERS_EVENTS);

        assertThat(envelope.eventType()).isEqualTo("auth.user.created");
        assertThat(envelope.eventVersion()).isEqualTo(1);
        assertThat(envelope.payloadJson())
                .contains("\"authUserId\":\"22222222-2222-2222-2222-222222222222\"")
                .contains("\"provider\":\"GOOGLE\"")
                .doesNotContain("AuthUserCreatedEvent");
    }

    @Test
    void mapsDoubleEncodedAuthUserDomainPayloadToPublicIntegrationPayload() {
        var outbox = new OutboxEventJpaEntity(
                "11111111-1111-1111-1111-111111111111",
                "com.nm.fragmentsclean.authenticationContext.write.businesslogic.models.AuthUserCreatedEvent",
                "AuthUser",
                "22222222-2222-2222-2222-222222222222",
                "authUser:22222222-2222-2222-2222-222222222222",
                """
                        "{\\"eventId\\":\\"11111111-1111-1111-1111-111111111111\\",\\"authUserId\\":\\"22222222-2222-2222-2222-222222222222\\",\\"provider\\":\\"GOOGLE\\",\\"providerUserId\\":\\"google-user\\",\\"email\\":\\"user@example.test\\",\\"emailVerified\\":true,\\"displayName\\":\\"Test User\\",\\"avatarUrl\\":\\"https://example.test/avatar.png\\",\\"occurredAt\\":\\"2026-07-06T08:00:00Z\\"}"
                        """,
                Instant.parse("2026-07-06T08:00:00Z"),
                Instant.parse("2026-07-06T08:00:01Z"),
                OutboxStatus.PENDING,
                0);

        var envelope = new IntegrationEventEnvelopeFactory()
                .from(outbox, IntegrationEventDestinations.AUTH_USERS_EVENTS);

        assertThat(envelope.payloadJson())
                .contains("\"displayName\":\"Test User\"")
                .contains("\"email\":\"user@example.test\"");
    }

    @Test
    void mapsAppUserDomainPayloadToPublicIntegrationPayload() {
        var outbox = new OutboxEventJpaEntity(
                "33333333-3333-3333-3333-333333333333",
                "com.nm.fragmentsclean.userApplicationContext.write.businesslogic.models.AppUserCreatedEvent",
                "AppUser",
                "44444444-4444-4444-4444-444444444444",
                "appUser:44444444-4444-4444-4444-444444444444",
                """
                        {
                          "eventId":"33333333-3333-3333-3333-333333333333",
                          "userId":"44444444-4444-4444-4444-444444444444",
                          "authUserId":"55555555-5555-5555-5555-555555555555",
                          "displayName":"App User",
                          "avatarUrl":null,
                          "version":2,
                          "occurredAt":"2026-07-06T08:00:00Z"
                        }
                        """,
                Instant.parse("2026-07-06T08:00:00Z"),
                Instant.parse("2026-07-06T08:00:01Z"),
                OutboxStatus.PENDING,
                0);

        var envelope = new IntegrationEventEnvelopeFactory()
                .from(outbox, IntegrationEventDestinations.APP_USERS_EVENTS);

        assertThat(envelope.eventType()).isEqualTo("app.user.created");
        assertThat(envelope.eventVersion()).isEqualTo(1);
        assertThat(envelope.payloadJson())
                .contains("\"userId\":\"44444444-4444-4444-4444-444444444444\"")
                .contains("\"authUserId\":\"55555555-5555-5555-5555-555555555555\"")
                .contains("\"version\":2")
                .doesNotContain("AppUserCreatedEvent");
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
    void routesCoffeeCreatedToCoffeeEventsAndSavedCoffeeProjectionEventsWithStableTypes() {
        var outbox = new OutboxEventJpaEntity(
                "evt-created",
                "com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.CoffeeCreatedEvent",
                "Coffee",
                "11111111-1111-1111-1111-111111111111",
                "coffee:11111111-1111-1111-1111-111111111111",
                """
                        {
                          "eventId":"22222222-2222-2222-2222-222222222222",
                          "commandId":"33333333-3333-3333-3333-333333333333",
                          "coffeeId":{"value":"11111111-1111-1111-1111-111111111111"},
                          "name":{"value":"Fragments Cafe"},
                          "address":{"city":"Paris"},
                          "version":3,
                          "occurredAt":"2026-07-04T10:00:00Z"
                        }
                        """,
                Instant.parse("2026-07-04T10:00:00Z"),
                Instant.parse("2026-07-04T10:00:01Z"),
                OutboxStatus.PENDING,
                0);

        assertThat(new IntegrationEventDestinationResolver().destinationsFor(outbox))
                .containsExactly("coffees-events", "app-users-events");

        var coffeeEnvelope = new IntegrationEventEnvelopeFactory()
                .from(outbox, IntegrationEventDestinations.COFFEES_EVENTS);

        assertThat(coffeeEnvelope.eventType()).isEqualTo("coffee.created");
        assertThat(coffeeEnvelope.eventVersion()).isEqualTo(1);
        assertThat(coffeeEnvelope.destination()).isEqualTo("coffees-events");
        assertThat(coffeeEnvelope.payloadJson())
                .contains("\"coffeeId\":\"11111111-1111-1111-1111-111111111111\"")
                .contains("\"name\":\"Fragments Cafe\"");

        var userProjectionEnvelope = new IntegrationEventEnvelopeFactory()
                .from(outbox, IntegrationEventDestinations.APP_USERS_EVENTS);

        assertThat(userProjectionEnvelope.eventType()).isEqualTo("coffee.saved_coffee_projection.created");
        assertThat(userProjectionEnvelope.eventVersion()).isEqualTo(1);
        assertThat(userProjectionEnvelope.destination()).isEqualTo("app-users-events");
        assertThat(userProjectionEnvelope.payloadJson())
                .contains("\"coffeeId\":\"11111111-1111-1111-1111-111111111111\"")
                .contains("\"name\":\"Fragments Cafe\"");
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
                .containsExactly("coffees-events", "app-users-events");

        var envelope = new IntegrationEventEnvelopeFactory()
                .from(outbox, IntegrationEventDestinations.COFFEES_EVENTS);

        assertThat(envelope.eventType()).isEqualTo("coffee.archived");
        assertThat(envelope.eventVersion()).isEqualTo(1);
        assertThat(envelope.destination()).isEqualTo("coffees-events");

        var userProjectionEnvelope = new IntegrationEventEnvelopeFactory()
                .from(outbox, IntegrationEventDestinations.APP_USERS_EVENTS);

        assertThat(userProjectionEnvelope.eventType()).isEqualTo("coffee.saved_coffee_projection.archived");
        assertThat(userProjectionEnvelope.eventVersion()).isEqualTo(1);
        assertThat(userProjectionEnvelope.destination()).isEqualTo("app-users-events");
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
                .containsExactly("coffees-events", "app-users-events");

        var envelope = new IntegrationEventEnvelopeFactory()
                .from(outbox, IntegrationEventDestinations.COFFEES_EVENTS);

        assertThat(envelope.eventType()).isEqualTo("coffee.deleted");
        assertThat(envelope.eventVersion()).isEqualTo(1);
        assertThat(envelope.destination()).isEqualTo("coffees-events");

        var userProjectionEnvelope = new IntegrationEventEnvelopeFactory()
                .from(outbox, IntegrationEventDestinations.APP_USERS_EVENTS);

        assertThat(userProjectionEnvelope.eventType()).isEqualTo("coffee.saved_coffee_projection.deleted");
        assertThat(userProjectionEnvelope.eventVersion()).isEqualTo(1);
        assertThat(userProjectionEnvelope.destination()).isEqualTo("app-users-events");
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

    @Test
    void maps_photo_added_domain_value_objects_to_primitive_sqs_payload() {
        var outbox = new OutboxEventJpaEntity(
                "99999999-9999-9999-9999-999999999999",
                "com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.CoffeePhotoAddedEvent",
                "Coffee", "11111111-1111-1111-1111-111111111111", "coffee:11111111-1111-1111-1111-111111111111",
                """
                        {"eventId":"99999999-9999-9999-9999-999999999999","commandId":"88888888-8888-8888-8888-888888888888",
                         "coffeeId":{"value":"11111111-1111-1111-1111-111111111111"},
                         "photo":{"photoId":{"value":"22222222-2222-2222-2222-222222222222"},"photoUri":"s3://bucket/photo.jpg"},
                         "version":4,"occurredAt":"2026-08-27T09:40:00Z","clientAt":"2026-08-27T09:39:00Z"}
                        """,
                Instant.parse("2026-08-27T09:40:00Z"), Instant.parse("2026-08-27T09:40:01Z"), OutboxStatus.PENDING, 0);

        var payload = new IntegrationEventEnvelopeFactory()
                .from(outbox, IntegrationEventDestinations.COFFEES_EVENTS).payloadJson();

        assertThat(payload).contains("\"coffeeId\":\"11111111-1111-1111-1111-111111111111\"")
                .contains("\"photoId\":\"22222222-2222-2222-2222-222222222222\"")
                .contains("s3://bucket/photo.jpg");
    }

    @Test
    void article_created_payload_round_trips_through_the_public_contract() throws Exception {
        var outbox = outbox("ArticleCreatedEvent", "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa", """
                {"eventId":"11111111-1111-1111-1111-111111111111","commandId":"22222222-2222-2222-2222-222222222222",
                 "articleId":"aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa","slug":"coffee-culture","locale":"fr-FR",
                 "authorId":"33333333-3333-3333-3333-333333333333","authorName":"Fragments","title":"Culture café",
                 "intro":"Intro","blocksJson":"[]","conclusion":"Conclusion","tags":["culture-cafe"],
                 "coffeeIds":[],"status":"DRAFT","version":1,"occurredAt":"2026-09-05T08:00:00Z"}
                """);

        var envelope = new IntegrationEventEnvelopeFactory().from(outbox, IntegrationEventDestinations.ARTICLES_EVENTS);
        var contract = objectMapper.readValue(envelope.payloadJson(), ArticleCreatedIntegrationEvent.class);

        assertThat(contract.articleId()).isEqualTo(java.util.UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"));
        assertThat(contract.status()).isEqualTo("DRAFT");
    }

    @Test
    void social_payload_round_trips_without_domain_enum_types() throws Exception {
        var outbox = outbox("CommentCreatedEvent", "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa", """
                {"eventId":"11111111-1111-1111-1111-111111111111","commandId":"22222222-2222-2222-2222-222222222222",
                 "commentId":"aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa","targetId":"bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb",
                 "authorId":"cccccccc-cccc-cccc-cccc-cccccccccccc","body":"Très bon café","moderation":"VISIBLE",
                 "version":2,"occurredAt":"2026-09-05T08:00:00Z"}
                """);

        var envelope = new IntegrationEventEnvelopeFactory().from(outbox, IntegrationEventDestinations.DOMAIN_EVENTS);
        var contract = objectMapper.readValue(envelope.payloadJson(), SocialCommentIntegrationEvents.Created.class);

        assertThat(contract.moderation()).isEqualTo("VISIBLE");
        assertThat(contract.body()).isEqualTo("Très bon café");
    }

    @Test
    void ticket_completed_payload_round_trips_with_primitive_line_items() throws Exception {
        var outbox = outbox("TicketVerificationCompletedEvent", "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa", """
                {"eventId":"11111111-1111-1111-1111-111111111111","commandId":"22222222-2222-2222-2222-222222222222",
                 "ticketId":"aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa","userId":"bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb",
                 "outcome":"APPROVED","version":3,"occurredAt":"2026-09-05T08:00:00Z",
                 "approved":{"amountCents":450,"currency":"EUR","ticketDate":"2026-09-05T07:00:00Z",
                 "merchantName":"Fragments","lineItems":[{"label":"Espresso","quantity":1,"amountCents":450}]}}
                """);

        var envelope = new IntegrationEventEnvelopeFactory().from(outbox, IntegrationEventDestinations.TICKET_EVENTS);
        var contract = objectMapper.readValue(envelope.payloadJson(), TicketIntegrationEvents.VerificationCompleted.class);

        assertThat(contract.outcome()).isEqualTo("APPROVED");
        assertThat(contract.approved().lineItems()).containsExactly(new TicketIntegrationEvents.LineItem("Espresso", 1, 450));
    }

    private OutboxEventJpaEntity outbox(String simpleEventType, String aggregateId, String payload) {
        return new OutboxEventJpaEntity("11111111-1111-1111-1111-111111111111",
                "com.nm.fragmentsclean.context.write.businesslogic.models." + simpleEventType,
                "Aggregate", aggregateId, "stream:" + aggregateId, payload,
                Instant.parse("2026-09-05T08:00:00Z"), Instant.parse("2026-09-05T08:00:01Z"),
                OutboxStatus.PENDING, 0);
    }
}
