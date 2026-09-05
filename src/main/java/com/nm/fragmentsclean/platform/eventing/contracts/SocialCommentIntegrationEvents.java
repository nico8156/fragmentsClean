package com.nm.fragmentsclean.platform.eventing.contracts;

import java.time.Instant;
import java.util.UUID;

public final class SocialCommentIntegrationEvents {
    private SocialCommentIntegrationEvents() { }

    public record Created(UUID eventId, UUID commandId, UUID commentId, UUID targetId, UUID parentId,
                          UUID authorId, String body, String moderation, long version,
                          Instant occurredAt, Instant clientAt) { }

    public record Updated(UUID eventId, UUID commandId, UUID commentId, UUID targetId,
                          UUID authorId, String body, String moderation, long version,
                          Instant occurredAt, Instant clientAt) { }

    public record Deleted(UUID eventId, UUID commandId, UUID commentId, UUID targetId,
                          UUID authorId, String moderation, Instant deletedAt, long version,
                          Instant occurredAt, Instant clientAt) { }
}
