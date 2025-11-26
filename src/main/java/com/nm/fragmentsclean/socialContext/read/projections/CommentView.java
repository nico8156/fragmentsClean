package com.nm.fragmentsclean.socialContext.read.projections;

import java.time.Instant;
import java.util.UUID;

public record CommentView(
        UUID id,
        UUID targetId,
        UUID parentId,
        UUID authorId,
        String body,
        Instant createdAt,
        Instant editedAt,
        Instant deletedAt,
        String moderation,
        long likeCount,
        long replyCount,
        long version
) {
}
