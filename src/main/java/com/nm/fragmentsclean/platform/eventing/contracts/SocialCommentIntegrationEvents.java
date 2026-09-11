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

    public record Reported(UUID eventId, UUID commandId, UUID reportId, UUID commentId, UUID targetId,
                           UUID authorId, UUID reporterId, String reason, String details, String status,
                           long version, Instant occurredAt, Instant clientAt) { }

    public record Moderated(UUID eventId, UUID commandId, UUID actionId, UUID reportId, UUID commentId,
                            UUID targetId, UUID authorId, UUID operatorId, String moderation,
                            String reportStatus, String reason, long version,
                            Instant occurredAt, Instant clientAt) { }

    public record UserBlockChanged(UUID eventId, UUID commandId, UUID blockId, UUID blockerId,
                                   UUID blockedUserId, boolean active, long version,
                                   Instant occurredAt, Instant clientAt) { }
}
