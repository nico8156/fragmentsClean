package com.nm.fragmentsclean.articleContext.write.adapters.secondary.gateways.repositories;

import com.nm.fragmentsclean.articleContext.write.businesslogic.gateways.repositories.ArticleFeaturedRankAvailabilityPort;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public final class JdbcArticleFeaturedRankAvailabilityAdapter implements ArticleFeaturedRankAvailabilityPort {
    private final JdbcTemplate jdbc;
    public JdbcArticleFeaturedRankAvailabilityAdapter(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    @Override public boolean occupiedByAnother(UUID articleId, int rank) {
        Integer count = jdbc.queryForObject("""
                SELECT COUNT(*) FROM articles
                WHERE status = 'PUBLISHED' AND featured_rank = ? AND article_id <> ?
                """, Integer.class, rank, articleId);
        return count != null && count > 0;
    }
}
