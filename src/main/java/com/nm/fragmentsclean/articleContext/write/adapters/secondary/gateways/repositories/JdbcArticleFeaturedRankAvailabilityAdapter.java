package com.nm.fragmentsclean.articleContext.write.adapters.secondary.gateways.repositories;

import com.nm.fragmentsclean.articleContext.write.businesslogic.gateways.repositories.ArticleFeaturedRankAvailabilityPort;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcArticleFeaturedRankAvailabilityAdapter implements ArticleFeaturedRankAvailabilityPort {
    private final JdbcTemplate jdbc;
    public JdbcArticleFeaturedRankAvailabilityAdapter(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    @Override public boolean occupiedByAnother(UUID articleId, int rank) {
        // One transaction may claim a rank at a time, including when the slot is empty.
        // The domain handler remains responsible for deciding whether to reject.
        jdbc.execute((org.springframework.jdbc.core.ConnectionCallback<Void>) connection -> {
            if (connection.getAutoCommit()) throw new IllegalStateException("Rank allocation requires a transaction");
            try (var statement = connection.prepareStatement("SELECT pg_advisory_xact_lock(714302, ?)")) {
                statement.setInt(1, rank);
                statement.execute();
            }
            return null;
        });
        Integer count = jdbc.queryForObject("""
                SELECT COUNT(*) FROM articles
                WHERE status = 'PUBLISHED' AND featured_rank = ? AND article_id <> ?
                """, Integer.class, rank, articleId);
        return count != null && count > 0;
    }
}
