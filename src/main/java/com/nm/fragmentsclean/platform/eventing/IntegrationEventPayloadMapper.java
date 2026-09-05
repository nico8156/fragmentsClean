package com.nm.fragmentsclean.platform.eventing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nm.fragmentsclean.platform.eventing.contracts.AppUserCreatedIntegrationEvent;
import com.nm.fragmentsclean.platform.eventing.contracts.AppUserProfileUpdatedIntegrationEvent;
import com.nm.fragmentsclean.platform.eventing.contracts.AuthUserCreatedIntegrationEvent;
import com.nm.fragmentsclean.platform.eventing.contracts.CoffeeCreatedIntegrationEvent;
import com.nm.fragmentsclean.platform.eventing.contracts.CoffeeLifecycleIntegrationEvent;
import com.nm.fragmentsclean.platform.eventing.contracts.CoffeePhotoAddedIntegrationEvent;
import com.nm.fragmentsclean.platform.eventing.contracts.CoffeePhotosImportedIntegrationEvent;
import com.nm.fragmentsclean.platform.eventing.contracts.CoffeePublishedIntegrationEvent;
import com.nm.fragmentsclean.platform.eventing.contracts.ArticleRevisionPublishedIntegrationEvent;
import com.nm.fragmentsclean.platform.eventing.contracts.ArticleArchivedIntegrationEvent;
import com.nm.fragmentsclean.platform.eventing.contracts.ArticleGenerationRequestedIntegrationEvent;
import com.nm.fragmentsclean.platform.eventing.contracts.ArticleGenerationCompletedIntegrationEvent;
import com.nm.fragmentsclean.platform.eventing.contracts.ArticleCreatedIntegrationEvent;
import com.nm.fragmentsclean.platform.eventing.contracts.CoffeeOpeningHoursImportedIntegrationEvent;
import com.nm.fragmentsclean.platform.eventing.contracts.CoffeePhotoDeletedIntegrationEvent;
import com.nm.fragmentsclean.platform.eventing.contracts.SavedCoffeeSetIntegrationEvent;
import com.nm.fragmentsclean.platform.eventing.contracts.SocialCommentIntegrationEvents;
import com.nm.fragmentsclean.platform.eventing.contracts.SocialLikeSetIntegrationEvent;
import com.nm.fragmentsclean.platform.eventing.contracts.TicketIntegrationEvents;
import com.nm.fragmentsclean.platform.eventing.contracts.ArticleWorkflowIntegrationEvent;
import com.nm.fragmentsclean.platform.eventing.contracts.AuthUserLoggedInIntegrationEvent;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.repositories.jpa.entities.OutboxEventJpaEntity;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class IntegrationEventPayloadMapper {

    private final ObjectMapper objectMapper;

    public IntegrationEventPayloadMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String toPublicPayloadJson(String stableEventType, OutboxEventJpaEntity event) {
        try {
            JsonNode node = readPayloadTree(event.getPayloadJson());
            Object publicPayload = switch (stableEventType) {
                case "article.created" -> new ArticleCreatedIntegrationEvent(
                        uuidOrFallback(node, "eventId", event.getEventId()), uuidOrFallback(node, "commandId", event.getEventId()),
                        uuidOrFallback(node, "articleId", event.getAggregateId()), text(node, "slug"), text(node, "locale"),
                        uuid(node, "authorId"), text(node, "authorName"), text(node, "title"), text(node, "intro"),
                        text(node, "blocksJson"), text(node, "conclusion"), text(node, "coverUrl"), nullableInt(node, "coverWidth"),
                        nullableInt(node, "coverHeight"), text(node, "coverAlt"), strings(node, "tags"), nullableInt(node, "readingTimeMin"),
                        uuids(node, "coffeeIds"), text(node, "status"), longValue(node, "version"),
                        instantOrFallback(node, "occurredAt", event.getOccurredAt()), nullableInstant(node, "clientAt"));
                case "article.draft.created", "article.draft.edited", "article.revision.submitted",
                        "article.generated_revision.edited" -> new ArticleWorkflowIntegrationEvent(
                        uuidOrFallback(node, "eventId", event.getEventId()), uuidOrFallback(node, "commandId", event.getEventId()),
                        nullableUuid(node, "sagaId"), uuidOrFallback(node, "articleId", event.getAggregateId()),
                        uuidOrFallback(node, "revisionId", event.getAggregateId()), text(node, "slug"), text(node, "locale"),
                        instantOrFallback(node, "occurredAt", event.getOccurredAt()), nullableInstant(node, "clientAt"));
                case "auth.user.created" -> authUserCreated(node, event);
                case "auth.user.logged_in" -> new AuthUserLoggedInIntegrationEvent(
                        uuidOrFallback(node, "eventId", event.getEventId()), uuidOrFallback(node, "authUserId", event.getAggregateId()),
                        text(node, "provider"), text(node, "providerUserId"),
                        instantOrFallback(node, "occurredAt", event.getOccurredAt()));
                case "app.user.created" -> appUserCreated(node, event);
                case "app.user.profile_updated" -> appUserProfileUpdated(node, event);
                case "coffee.created", "coffee.saved_coffee_projection.created" -> coffeeCreated(node, event);
                case "coffee.archived", "coffee.deleted",
                        "coffee.saved_coffee_projection.archived",
                        "coffee.saved_coffee_projection.deleted" -> coffeeLifecycle(node, event);
                case "coffee.photo_added" -> coffeePhotoAdded(node, event);
                case "coffee.photos_imported" -> coffeePhotosImported(node, event);
                case "coffee.opening_hours_imported" -> new CoffeeOpeningHoursImportedIntegrationEvent(
                        uuidOrFallback(node, "eventId", event.getEventId()), uuidOrFallback(node, "commandId", event.getEventId()),
                        uuidFromValueObjectOrFallback(node, "coffeeId", event.getAggregateId()), valueObjectText(node, "googlePlaceId"),
                        strings(node, "weekdayDescriptions"), longValue(node, "version"),
                        instantOrFallback(node, "occurredAt", event.getOccurredAt()), nullableInstant(node, "clientAt"));
                case "coffee.photo_deleted" -> new CoffeePhotoDeletedIntegrationEvent(
                        uuidOrFallback(node, "eventId", event.getEventId()), uuidOrFallback(node, "commandId", event.getEventId()),
                        uuidFromValueObjectOrFallback(node, "coffeeId", event.getAggregateId()),
                        uuidFromValueObjectOrFallback(node, "photoId", event.getEventId()), intValue(node, "version"),
                        instantOrFallback(node, "occurredAt", event.getOccurredAt()), nullableInstant(node, "clientAt"));
                case "user.saved_coffee.set" -> new SavedCoffeeSetIntegrationEvent(
                        uuidOrFallback(node, "eventId", event.getEventId()), uuidOrFallback(node, "commandId", event.getEventId()),
                        uuidOrFallback(node, "savedCoffeeId", event.getAggregateId()), uuid(node, "userId"), uuid(node, "coffeeId"),
                        bool(node, "active"), longValue(node, "version"), instantOrFallback(node, "occurredAt", event.getOccurredAt()),
                        nullableInstant(node, "clientAt"));
                case "social.comment.created" -> new SocialCommentIntegrationEvents.Created(
                        uuidOrFallback(node, "eventId", event.getEventId()), uuidOrFallback(node, "commandId", event.getEventId()),
                        uuidOrFallback(node, "commentId", event.getAggregateId()), uuid(node, "targetId"), nullableUuid(node, "parentId"),
                        uuid(node, "authorId"), text(node, "body"), text(node, "moderation"), longValue(node, "version"),
                        instantOrFallback(node, "occurredAt", event.getOccurredAt()), nullableInstant(node, "clientAt"));
                case "social.comment.updated" -> new SocialCommentIntegrationEvents.Updated(
                        uuidOrFallback(node, "eventId", event.getEventId()), uuidOrFallback(node, "commandId", event.getEventId()),
                        uuidOrFallback(node, "commentId", event.getAggregateId()), uuid(node, "targetId"), uuid(node, "authorId"),
                        text(node, "body"), text(node, "moderation"), longValue(node, "version"),
                        instantOrFallback(node, "occurredAt", event.getOccurredAt()), nullableInstant(node, "clientAt"));
                case "social.comment.deleted" -> new SocialCommentIntegrationEvents.Deleted(
                        uuidOrFallback(node, "eventId", event.getEventId()), uuidOrFallback(node, "commandId", event.getEventId()),
                        uuidOrFallback(node, "commentId", event.getAggregateId()), uuid(node, "targetId"), uuid(node, "authorId"),
                        text(node, "moderation"), nullableInstant(node, "deletedAt"), longValue(node, "version"),
                        instantOrFallback(node, "occurredAt", event.getOccurredAt()), nullableInstant(node, "clientAt"));
                case "social.like.set" -> new SocialLikeSetIntegrationEvent(
                        uuidOrFallback(node, "eventId", event.getEventId()), text(node, "commandId"),
                        uuidOrFallback(node, "likeId", event.getAggregateId()), uuid(node, "userId"), uuid(node, "targetId"),
                        bool(node, "active"), longValue(node, "count"), longValue(node, "version"),
                        instantOrFallback(node, "occurredAt", event.getOccurredAt()), nullableInstant(node, "clientAt"));
                case "ticket.verify.accepted" -> new TicketIntegrationEvents.VerifyAccepted(
                        uuidOrFallback(node, "eventId", event.getEventId()), uuidOrFallback(node, "commandId", event.getEventId()),
                        uuidOrFallback(node, "ticketId", event.getAggregateId()), uuid(node, "userId"), text(node, "ocrText"),
                        text(node, "imageRef"), text(node, "status"), longValue(node, "version"),
                        instantOrFallback(node, "occurredAt", event.getOccurredAt()), nullableInstant(node, "clientAt"));
                case "ticket.verification.completed" -> ticketVerificationCompleted(node, event);
                case "ticket.admin.updated" -> new TicketIntegrationEvents.AdminUpdated(
                        uuidOrFallback(node, "eventId", event.getEventId()), uuidOrFallback(node, "commandId", event.getEventId()),
                        uuidOrFallback(node, "ticketId", event.getAggregateId()), uuid(node, "userId"), text(node, "status"),
                        text(node, "ocrText"), text(node, "imageRef"), nullableInt(node, "amountCents"), text(node, "currency"),
                        nullableInstant(node, "ticketDate"), text(node, "merchantName"), text(node, "merchantAddress"),
                        text(node, "paymentMethod"), text(node, "rejectionReason"), longValue(node, "version"),
                        uuid(node, "actorUserId"), instantOrFallback(node, "occurredAt", event.getOccurredAt()));
                case "ticket.admin.deleted" -> new TicketIntegrationEvents.AdminDeleted(
                        uuidOrFallback(node, "eventId", event.getEventId()), uuidOrFallback(node, "commandId", event.getEventId()),
                        uuidOrFallback(node, "ticketId", event.getAggregateId()), uuid(node, "userId"), uuid(node, "actorUserId"),
                        longValue(node, "version"), instantOrFallback(node, "occurredAt", event.getOccurredAt()));
                case "coffee.published" -> new CoffeePublishedIntegrationEvent(
                        uuidOrFallback(node, "eventId", event.getEventId()),
                        uuidOrFallback(node, "commandId", event.getEventId()),
                        uuidFromValueObjectOrFallback(node, "coffeeId", event.getAggregateId()),
                        intValue(node, "version"), instantOrFallback(node, "occurredAt", event.getOccurredAt()));
                case "article.revision.published" -> new ArticleRevisionPublishedIntegrationEvent(
                        uuidOrFallback(node, "eventId", event.getEventId()),
                        uuidOrFallback(node, "commandId", event.getEventId()),
                        uuidOrFallback(node, "articleId", event.getAggregateId()),
                        uuidOrFallback(node, "revisionId", event.getAggregateId()),
                        longValue(node, "version"),
                        instantOrFallback(node, "occurredAt", event.getOccurredAt()),
                        instantOrFallback(node, "clientAt", event.getOccurredAt()));
                case "article.archived" -> new ArticleArchivedIntegrationEvent(
                        uuidOrFallback(node, "eventId", event.getEventId()),
                        uuidOrFallback(node, "commandId", event.getEventId()),
                        uuidOrFallback(node, "articleId", event.getAggregateId()),
                        uuidOrFallback(node, "revisionId", event.getAggregateId()),
                        longValue(node, "version"), instantOrFallback(node, "occurredAt", event.getOccurredAt()),
                        instantOrFallback(node, "clientAt", event.getOccurredAt()));
                case "article.generation.requested" -> new ArticleGenerationRequestedIntegrationEvent(
                        uuidOrFallback(node, "eventId", event.getEventId()),
                        uuidOrFallback(node, "commandId", event.getEventId()),
                        uuidOrFallback(node, "sagaId", event.getAggregateId()), uuidOrFallback(node, "articleId", event.getAggregateId()),
                        uuidOrFallback(node, "revisionId", event.getAggregateId()), text(node, "theme"), text(node, "locale"),
                        text(node, "trigger"), longValue(node, "version"), instantOrFallback(node, "occurredAt", event.getOccurredAt()),
                        instantOrFallback(node, "clientAt", event.getOccurredAt()));
                case "article.generation.completed" -> new ArticleGenerationCompletedIntegrationEvent(
                        uuidOrFallback(node, "eventId", event.getEventId()),
                        uuidOrFallback(node, "sagaId", event.getAggregateId()), uuidOrFallback(node, "articleId", event.getAggregateId()),
                        uuidOrFallback(node, "revisionId", event.getAggregateId()), uuidOrFallback(node, "runId", event.getEventId()),
                        text(node, "provider"), text(node, "providerResponseId"), text(node, "model"), text(node, "schemaVersion"),
                        longValue(node, "sagaVersion"), instantOrFallback(node, "occurredAt", event.getOccurredAt()));
                default -> null;
            };

            if (publicPayload == null) {
                return event.getPayloadJson();
            }
            return objectMapper.writeValueAsString(publicPayload);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to map integration payload for " + stableEventType, exception);
        }
    }

    private JsonNode readPayloadTree(String payloadJson) throws java.io.IOException {
        JsonNode node = objectMapper.readTree(payloadJson);
        if (node != null && node.isTextual()) {
            return objectMapper.readTree(node.asText());
        }
        return node;
    }

    private AuthUserCreatedIntegrationEvent authUserCreated(JsonNode node, OutboxEventJpaEntity event) {
        return new AuthUserCreatedIntegrationEvent(
                uuidOrFallback(node, "eventId", event.getEventId()),
                uuidOrFallback(node, "authUserId", event.getAggregateId()),
                text(node, "provider"),
                text(node, "providerUserId"),
                text(node, "email"),
                bool(node, "emailVerified"),
                text(node, "displayName"),
                text(node, "avatarUrl"),
                instantOrFallback(node, "occurredAt", event.getOccurredAt())
        );
    }

    private AppUserCreatedIntegrationEvent appUserCreated(JsonNode node, OutboxEventJpaEntity event) {
        return new AppUserCreatedIntegrationEvent(
                uuidOrFallback(node, "eventId", event.getEventId()),
                uuidOrFallback(node, "userId", event.getAggregateId()),
                uuid(node, "authUserId"),
                text(node, "displayName"),
                text(node, "avatarUrl"),
                longValue(node, "version"),
                instantOrFallback(node, "occurredAt", event.getOccurredAt())
        );
    }

    private AppUserProfileUpdatedIntegrationEvent appUserProfileUpdated(JsonNode node, OutboxEventJpaEntity event) {
        return new AppUserProfileUpdatedIntegrationEvent(
                uuidOrFallback(node, "eventId", event.getEventId()),
                uuidOrFallback(node, "userId", event.getAggregateId()),
                text(node, "displayName"),
                text(node, "avatarUrl"),
                longValue(node, "version"),
                instantOrFallback(node, "occurredAt", event.getOccurredAt())
        );
    }

    private CoffeeCreatedIntegrationEvent coffeeCreated(JsonNode node, OutboxEventJpaEntity event) {
        JsonNode address = node.get("address");
        return new CoffeeCreatedIntegrationEvent(
                uuidOrFallback(node, "eventId", event.getEventId()),
                uuidOrFallback(node, "commandId", event.getEventId()),
                uuidFromValueObjectOrFallback(node, "coffeeId", event.getAggregateId()),
                valueObjectText(node, "googlePlaceId"),
                valueObjectText(node, "name"),
                addressText(address, "line1"),
                addressText(address, "city"),
                addressText(address, "postalCode"),
                addressText(address, "country"),
                text(node, "publicationStatus"),
                intValue(node, "version"),
                instantOrFallback(node, "occurredAt", event.getOccurredAt())
        );
    }

    private CoffeeLifecycleIntegrationEvent coffeeLifecycle(JsonNode node, OutboxEventJpaEntity event) {
        return new CoffeeLifecycleIntegrationEvent(
                uuidOrFallback(node, "eventId", event.getEventId()),
                uuidOrFallback(node, "commandId", event.getEventId()),
                uuidFromValueObjectOrFallback(node, "coffeeId", event.getAggregateId()),
                intValue(node, "version"),
                instantOrFallback(node, "occurredAt", event.getOccurredAt())
        );
    }

    private CoffeePhotoAddedIntegrationEvent coffeePhotoAdded(JsonNode node, OutboxEventJpaEntity event) {
        JsonNode photo = node.get("photo");
        return new CoffeePhotoAddedIntegrationEvent(
                uuidOrFallback(node, "eventId", event.getEventId()),
                uuidOrFallback(node, "commandId", event.getEventId()),
                uuidFromValueObjectOrFallback(node, "coffeeId", event.getAggregateId()),
                uuidFromValueObjectOrFallback(photo, "photoId", event.getEventId()),
                text(photo, "photoUri"),
                intValue(node, "version"),
                instantOrFallback(node, "occurredAt", event.getOccurredAt()),
                instantOrFallback(node, "clientAt", event.getOccurredAt()));
    }

    private CoffeePhotosImportedIntegrationEvent coffeePhotosImported(JsonNode node, OutboxEventJpaEntity event) {
        List<CoffeePhotosImportedIntegrationEvent.PhotoReference> photos = new java.util.ArrayList<>();
        JsonNode photoNodes = node.get("photos");
        if (photoNodes != null && photoNodes.isArray()) {
            for (JsonNode photo : photoNodes) {
                photos.add(new CoffeePhotosImportedIntegrationEvent.PhotoReference(
                        uuidFromValueObjectOrFallback(photo, "photoId", event.getEventId()),
                        text(photo, "photoUri")));
            }
        }
        return new CoffeePhotosImportedIntegrationEvent(
                uuidOrFallback(node, "eventId", event.getEventId()),
                uuidOrFallback(node, "commandId", event.getEventId()),
                uuidFromValueObjectOrFallback(node, "coffeeId", event.getAggregateId()),
                photos,
                longValue(node, "version"),
                instantOrFallback(node, "occurredAt", event.getOccurredAt()),
                instantOrFallback(node, "clientAt", event.getOccurredAt()));
    }

    private TicketIntegrationEvents.VerificationCompleted ticketVerificationCompleted(
            JsonNode node, OutboxEventJpaEntity event) {
        JsonNode approved = node.get("approved");
        TicketIntegrationEvents.Approved approvedContract = null;
        if (approved != null && !approved.isNull()) {
            List<TicketIntegrationEvents.LineItem> items = new java.util.ArrayList<>();
            JsonNode lineItems = approved.get("lineItems");
            if (lineItems != null && lineItems.isArray()) {
                lineItems.forEach(item -> items.add(new TicketIntegrationEvents.LineItem(
                        text(item, "label"), nullableInt(item, "quantity"), nullableInt(item, "amountCents"))));
            }
            approvedContract = new TicketIntegrationEvents.Approved(intValue(approved, "amountCents"),
                    text(approved, "currency"), nullableInstant(approved, "ticketDate"), text(approved, "merchantName"),
                    text(approved, "merchantAddress"), text(approved, "paymentMethod"), items);
        }
        JsonNode rejected = node.get("rejected");
        TicketIntegrationEvents.Rejected rejectedContract = rejected == null || rejected.isNull() ? null
                : new TicketIntegrationEvents.Rejected(text(rejected, "reasonCode"), text(rejected, "message"));
        return new TicketIntegrationEvents.VerificationCompleted(
                uuidOrFallback(node, "eventId", event.getEventId()), uuidOrFallback(node, "commandId", event.getEventId()),
                uuidOrFallback(node, "ticketId", event.getAggregateId()), nullableUuid(node, "userId"), text(node, "outcome"),
                longValue(node, "version"), instantOrFallback(node, "occurredAt", event.getOccurredAt()),
                nullableInstant(node, "clientAt"), approvedContract, rejectedContract,
                text(node, "provider"), text(node, "providerTraceId"));
    }

    private UUID uuid(JsonNode node, String fieldName) {
        return UUID.fromString(text(node, fieldName));
    }

    private UUID nullableUuid(JsonNode node, String fieldName) {
        String value = text(node, fieldName);
        return value == null ? null : UUID.fromString(value);
    }

    private Integer nullableInt(JsonNode node, String fieldName) {
        JsonNode value = node == null ? null : node.get(fieldName);
        return value == null || value.isNull() ? null : value.asInt();
    }

    private Instant nullableInstant(JsonNode node, String fieldName) {
        String value = text(node, fieldName);
        return value == null ? null : Instant.parse(value);
    }

    private List<String> strings(JsonNode node, String fieldName) {
        JsonNode values = node == null ? null : node.get(fieldName);
        if (values == null || !values.isArray()) return List.of();
        java.util.ArrayList<String> result = new java.util.ArrayList<>();
        values.forEach(value -> result.add(value.asText()));
        return List.copyOf(result);
    }

    private List<UUID> uuids(JsonNode node, String fieldName) {
        return strings(node, fieldName).stream().map(UUID::fromString).toList();
    }

    private UUID uuidOrFallback(JsonNode node, String fieldName, String fallback) {
        String value = text(node, fieldName);
        return parseUuidOrDeterministicFallback(value == null ? fallback : value);
    }

    private UUID uuidFromValueObjectOrFallback(JsonNode node, String fieldName, String fallback) {
        String value = valueObjectText(node, fieldName);
        return parseUuidOrDeterministicFallback(value == null ? fallback : value);
    }

    private UUID parseUuidOrDeterministicFallback(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException exception) {
            return UUID.nameUUIDFromBytes(value.getBytes(StandardCharsets.UTF_8));
        }
    }

    private String text(JsonNode node, String fieldName) {
        if (node == null || node.isNull()) {
            return null;
        }
        JsonNode value = node.get(fieldName);
        if (value == null) {
            value = node.get(camelToSnake(fieldName));
        }
        return value == null || value.isNull() ? null : value.asText();
    }

    private String camelToSnake(String value) {
        return value.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
    }

    private boolean bool(JsonNode node, String fieldName) {
        JsonNode value = node.get(fieldName);
        return value != null && value.asBoolean();
    }

    private int intValue(JsonNode node, String fieldName) {
        JsonNode value = node.get(fieldName);
        return value == null ? 0 : value.asInt();
    }

    private long longValue(JsonNode node, String fieldName) {
        JsonNode value = node.get(fieldName);
        return value == null ? 0L : value.asLong();
    }

    private String valueObjectText(JsonNode node, String fieldName) {
        if (node == null || node.isNull()) {
            return null;
        }
        JsonNode value = node.get(fieldName);
        if (value == null) {
            value = node.get(camelToSnake(fieldName));
        }
        if (value == null || value.isNull()) {
            return null;
        }
        if (value.isTextual()) {
            return value.asText();
        }
        JsonNode nestedValue = value.get("value");
        return nestedValue == null || nestedValue.isNull() ? value.asText(null) : nestedValue.asText();
    }

    private String addressText(JsonNode address, String fieldName) {
        if (address == null || address.isNull()) {
            return null;
        }
        JsonNode value = address.get(fieldName);
        if (value == null) {
            value = address.get(camelToSnake(fieldName));
        }
        return value == null || value.isNull() ? null : value.asText();
    }

    private Instant instant(JsonNode node, String fieldName) {
        return Instant.parse(text(node, fieldName));
    }

    private Instant instantOrFallback(JsonNode node, String fieldName, Instant fallback) {
        String value = text(node, fieldName);
        return value == null ? fallback : Instant.parse(value);
    }
}
