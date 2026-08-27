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
                case "auth.user.created" -> authUserCreated(node, event);
                case "app.user.created" -> appUserCreated(node, event);
                case "app.user.profile_updated" -> appUserProfileUpdated(node, event);
                case "coffee.created", "coffee.saved_coffee_projection.created" -> coffeeCreated(node, event);
                case "coffee.archived", "coffee.deleted",
                        "coffee.saved_coffee_projection.archived",
                        "coffee.saved_coffee_projection.deleted" -> coffeeLifecycle(node, event);
                case "coffee.photo_added" -> coffeePhotoAdded(node, event);
                case "coffee.photos_imported" -> coffeePhotosImported(node, event);
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

    private UUID uuid(JsonNode node, String fieldName) {
        return UUID.fromString(text(node, fieldName));
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
