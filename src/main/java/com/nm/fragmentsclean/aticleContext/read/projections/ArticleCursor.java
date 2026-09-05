package com.nm.fragmentsclean.aticleContext.read.projections;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

public record ArticleCursor(Direction direction, Instant publishedAt, UUID articleId) {
    private static final String VERSION = "v1";

    public ArticleCursor {
        if (direction == null || publishedAt == null || articleId == null) {
            throw new IllegalArgumentException("An article cursor requires a direction, timestamp and article id.");
        }
    }

    public static ArticleCursor next(Instant publishedAt, UUID articleId) {
        return new ArticleCursor(Direction.NEXT, publishedAt, articleId);
    }

    public static ArticleCursor previous(Instant publishedAt, UUID articleId) {
        return new ArticleCursor(Direction.PREVIOUS, publishedAt, articleId);
    }

    public String encode() {
        String value = String.join("|", VERSION, direction.name(), publishedAt.toString(), articleId.toString());
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    public static Optional<ArticleCursor> decode(String raw) {
        if (raw == null || raw.isBlank()) return Optional.empty();
        try {
            String decoded = new String(Base64.getUrlDecoder().decode(raw), StandardCharsets.UTF_8);
            String[] parts = decoded.split("\\|", -1);
            if (parts.length != 4 || !VERSION.equals(parts[0])) return Optional.empty();
            return Optional.of(new ArticleCursor(
                    Direction.valueOf(parts[1]), Instant.parse(parts[2]), UUID.fromString(parts[3])));
        } catch (RuntimeException ignored) {
            return Optional.empty();
        }
    }

    public enum Direction { NEXT, PREVIOUS }
}
