package com.nm.fragmentsclean.aticleContext.write.adapters.secondary.observability;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Instant;

@Component("articleAuthoringHealth")
public final class ArticleAuthoringHealthIndicator implements HealthIndicator {
    private final JdbcTemplate jdbc;
    private final int staleAfterMinutes;

    public ArticleAuthoringHealthIndicator(
            JdbcTemplate jdbc,
            @Value("${fragments.article.authoring.health.stale-after-minutes:15}") int staleAfterMinutes) {
        this.jdbc = jdbc;
        this.staleAfterMinutes = staleAfterMinutes;
    }

    @Override
    public Health health() {
        var staleBefore = Instant.now().minusSeconds(Math.max(1, staleAfterMinutes) * 60L);
        var stale = jdbc.queryForObject("""
                SELECT COUNT(*) FROM article_authoring_sagas
                WHERE state IN ('GENERATION_PENDING','GENERATING','VALIDATING','NOTIFICATION_PENDING','PUBLICATION_REQUESTED')
                  AND updated_at < ?
                """, Integer.class, Timestamp.from(staleBefore));
        var failed = jdbc.queryForObject("SELECT COUNT(*) FROM article_authoring_sagas WHERE state = 'FAILED'", Integer.class);
        var builder = stale != null && stale > 0 ? Health.status("DEGRADED") : Health.up();
        return builder.withDetail("staleActiveSagas", stale == null ? 0 : stale)
                .withDetail("failedSagas", failed == null ? 0 : failed)
                .withDetail("staleAfterMinutes", staleAfterMinutes).build();
    }
}
