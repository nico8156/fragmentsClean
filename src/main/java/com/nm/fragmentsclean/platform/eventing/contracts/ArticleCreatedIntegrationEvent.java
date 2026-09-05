package com.nm.fragmentsclean.platform.eventing.contracts;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ArticleCreatedIntegrationEvent(
        UUID eventId, UUID commandId, UUID articleId, String slug, String locale,
        UUID authorId, String authorName, String title, String intro, String blocksJson,
        String conclusion, String coverUrl, Integer coverWidth, Integer coverHeight,
        String coverAlt, List<String> tags, Integer readingTimeMin, List<UUID> coffeeIds,
        String status, long version, Instant occurredAt, Instant clientAt) {
    public ArticleCreatedIntegrationEvent {
        tags = tags == null ? List.of() : List.copyOf(tags);
        coffeeIds = coffeeIds == null ? List.of() : List.copyOf(coffeeIds);
    }
}
