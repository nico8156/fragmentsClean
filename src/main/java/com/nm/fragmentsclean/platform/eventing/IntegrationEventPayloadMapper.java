package com.nm.fragmentsclean.platform.eventing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nm.fragmentsclean.platform.eventing.contracts.AppUserCreatedIntegrationEvent;
import com.nm.fragmentsclean.platform.eventing.contracts.AppUserProfileUpdatedIntegrationEvent;
import com.nm.fragmentsclean.platform.eventing.contracts.AuthUserCreatedIntegrationEvent;
import com.nm.fragmentsclean.platform.eventing.contracts.CoffeeCreatedIntegrationEvent;
import com.nm.fragmentsclean.platform.eventing.contracts.CoffeeLifecycleIntegrationEvent;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.repositories.jpa.entities.OutboxEventJpaEntity;

import java.time.Instant;
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
                case "coffee.created" -> coffeeCreated(node, event);
                case "coffee.archived", "coffee.deleted" -> coffeeLifecycle(node, event);
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

    private UUID uuid(JsonNode node, String fieldName) {
        return UUID.fromString(text(node, fieldName));
    }

    private UUID uuidOrFallback(JsonNode node, String fieldName, String fallback) {
        String value = text(node, fieldName);
        return UUID.fromString(value == null ? fallback : value);
    }

    private UUID uuidFromValueObjectOrFallback(JsonNode node, String fieldName, String fallback) {
        String value = valueObjectText(node, fieldName);
        return UUID.fromString(value == null ? fallback : value);
    }

    private String text(JsonNode node, String fieldName) {
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
