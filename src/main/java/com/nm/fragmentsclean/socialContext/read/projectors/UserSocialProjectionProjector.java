package com.nm.fragmentsclean.socialContext.read.projectors;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

@Component
public class UserSocialProjectionProjector {

    private final JdbcTemplate jdbcTemplate;

    public UserSocialProjectionProjector(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }


    public void upsert(UUID userId,
                       String displayName,
                       String avatarUrl,
                       long version,
                       Instant occurredAt) {

        Instant instant = (occurredAt != null) ? occurredAt : Instant.now();
        Timestamp ts = Timestamp.from(instant);

        String safeName = (displayName == null || displayName.isBlank()) ? "Unknown" : displayName;

        int updated = jdbcTemplate.update("""
        UPDATE user_social_projection
        SET display_name = ?,
            avatar_url   = ?,
            updated_at   = ?,
            version      = ?
        WHERE user_id = ?
          AND version <= ?
        """,
                safeName, avatarUrl, ts, version,
                userId, version
        );

        if (updated > 0) return;

        try {
            jdbcTemplate.update("""
            INSERT INTO user_social_projection (user_id, display_name, avatar_url, created_at, updated_at, version)
            VALUES (?, ?, ?, ?, ?, ?)
            """,
                    userId, safeName, avatarUrl, ts, ts, version
            );
        } catch (Exception e) {
            jdbcTemplate.update("""
            UPDATE user_social_projection
            SET display_name = ?,
                avatar_url   = ?,
                updated_at   = ?,
                version      = ?
            WHERE user_id = ?
              AND version <= ?
            """,
                    safeName, avatarUrl, ts, version,
                    userId, version
            );
        }
    }


}
