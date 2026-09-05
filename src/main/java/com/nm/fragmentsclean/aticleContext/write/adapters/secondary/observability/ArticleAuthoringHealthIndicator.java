package com.nm.fragmentsclean.aticleContext.write.adapters.secondary.observability;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
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
            MeterRegistry meters,
            @Value("${fragments.article.authoring.health.stale-after-minutes:15}") int staleAfterMinutes) {
        this.jdbc = jdbc;
        this.staleAfterMinutes = staleAfterMinutes;
        Gauge.builder("fragments.article.sagas.failed", this, ignored -> countFailed())
                .description("Failed article authoring sagas").register(meters);
        Gauge.builder("fragments.article.sagas.stale", this, ignored -> countStale())
                .description("Stale active article authoring sagas").register(meters);
    }

    @Override
    public Health health() {
        var stale = countStale();
        var failed = countFailed();
        var builder = stale > 0 ? Health.status("DEGRADED") : Health.up();
        return builder.withDetail("staleActiveSagas", stale)
                .withDetail("failedSagas", failed)
                .withDetail("staleAfterMinutes", staleAfterMinutes).build();
    }

    private int countStale() {
        var staleBefore = Instant.now().minusSeconds(Math.max(1, staleAfterMinutes) * 60L);
        Integer stale = jdbc.queryForObject("""
                SELECT COUNT(*) FROM article_authoring_sagas
                WHERE state IN ('GENERATION_PENDING','GENERATING','VALIDATING','NOTIFICATION_PENDING','PUBLICATION_REQUESTED')
                  AND updated_at < ?
                """, Integer.class, Timestamp.from(staleBefore));
        return stale == null ? 0 : stale;
    }

    private int countFailed() {
        Integer failed = jdbc.queryForObject(
                "SELECT COUNT(*) FROM article_authoring_sagas WHERE state = 'FAILED'", Integer.class);
        return failed == null ? 0 : failed;
    }
}
