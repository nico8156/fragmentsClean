package com.nm.fragmentsclean.socialContext.read.projectors;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
public class UsersPublicProjectionProjector {

    private final JdbcTemplate jdbcTemplate;

    public UsersPublicProjectionProjector(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void upsert(UUID userId,
                       String displayName,
                       String avatarUrl,
                       String locale,
                       long version,
                       Instant occurredAt) {

        Instant now = occurredAt != null ? occurredAt : Instant.now();

        String sql = """
            INSERT INTO users (user_id, created_at, updated_at, display_name, avatar_url, bio, locale, version)
            VALUES (?, ?, ?, ?, ?, NULL, ?, ?)
            ON CONFLICT (user_id) DO UPDATE
            SET display_name = EXCLUDED.display_name,
                avatar_url   = EXCLUDED.avatar_url,
                updated_at   = EXCLUDED.updated_at,
                locale       = EXCLUDED.locale,
                version      = EXCLUDED.version
            WHERE users.version <= EXCLUDED.version
            """;

        jdbcTemplate.update(sql,
                userId,
                now,          // created_at (si insert)
                now,          // updated_at
                safeDisplayName(displayName),
                avatarUrl,
                safeLocale(locale),
                version
        );
    }

    private String safeLocale(String locale) {
        return (locale == null || locale.isBlank()) ? "fr-FR" : locale;
    }

    private String safeDisplayName(String name) {
        return (name == null || name.isBlank()) ? "Unknown" : name;
    }
}
