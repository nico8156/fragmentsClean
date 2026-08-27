package com.nm.fragmentsclean.aticleContext.write.adapters.secondary.gateways.repositories;

import com.nm.fragmentsclean.aticleContext.write.businesslogic.gateways.ArticleGenerationScheduleGuard;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;

@Repository
public final class JdbcArticleGenerationScheduleGuard implements ArticleGenerationScheduleGuard {
    private final JdbcTemplate jdbc;

    public JdbcArticleGenerationScheduleGuard(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public boolean mayRequest(String subject, Instant now, int maxPending, int deduplicationHours) {
        if (maxPending < 1 || deduplicationHours < 0) return false;
        var pending = jdbc.queryForObject("""
                SELECT COUNT(*) FROM article_authoring_sagas
                WHERE state NOT IN ('PUBLISHED','REJECTED','FAILED','EXPIRED','CANCELLED')
                """, Integer.class);
        if (pending != null && pending >= maxPending) return false;
        var since = now.minusSeconds(deduplicationHours * 3600L);
        var duplicate = jdbc.queryForObject("""
                SELECT COUNT(*) FROM article_authoring_sagas
                WHERE LOWER(TRIM(theme)) = LOWER(TRIM(?)) AND updated_at >= ?
                """, Integer.class, subject, Timestamp.from(since));
        return duplicate == null || duplicate == 0;
    }
}
