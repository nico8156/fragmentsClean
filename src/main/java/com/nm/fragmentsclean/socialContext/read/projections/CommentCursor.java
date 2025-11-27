package com.nm.fragmentsclean.socialContext.read.projections;

import java.time.Instant;
import java.util.UUID;

public record CommentCursor(Instant createdAt, UUID id) {

    public static CommentCursor parse(String cursor) {
        if (cursor == null || cursor.isBlank()) return null;
        var parts = cursor.split(":", 2);
        long epochMillis = Long.parseLong(parts[0]);
        UUID id = UUID.fromString(parts[1]);
        return new CommentCursor(Instant.ofEpochMilli(epochMillis), id);
    }

    public String encode() {
        return createdAt.toEpochMilli() + ":" + id;
    }
}
