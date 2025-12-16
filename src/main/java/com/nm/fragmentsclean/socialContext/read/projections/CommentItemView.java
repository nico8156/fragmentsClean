package com.nm.fragmentsclean.socialContext.read.projections;

import java.time.Instant;
import java.util.UUID;

public record CommentItemView(
        UUID id,
        UUID targetId,
        UUID parentId,
        UUID authorId,
        String authorName,
        String avatarUrl,
        String body,
        Instant createdAt,
        Instant editedAt,
        long likeCount,
        long replyCount,
        long version
) {}
