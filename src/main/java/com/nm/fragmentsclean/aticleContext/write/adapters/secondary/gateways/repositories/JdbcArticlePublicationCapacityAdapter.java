package com.nm.fragmentsclean.aticleContext.write.adapters.secondary.gateways.repositories;

import com.nm.fragmentsclean.aticleContext.write.businesslogic.gateways.repositories.ArticlePublicationCapacityPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public class JdbcArticlePublicationCapacityAdapter implements ArticlePublicationCapacityPort {
    private final JdbcTemplate jdbc;

    public JdbcArticlePublicationCapacityAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public int countPublishedExcluding(UUID articleId) {
        // A transaction-scoped advisory lock also protects the empty-catalogue case.
        jdbc.query("SELECT pg_advisory_xact_lock(191, 30)", resultSet -> null);
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM articles WHERE status = 'PUBLISHED' AND article_id <> ?",
                Integer.class, articleId);
        return count == null ? 0 : count;
    }
}
