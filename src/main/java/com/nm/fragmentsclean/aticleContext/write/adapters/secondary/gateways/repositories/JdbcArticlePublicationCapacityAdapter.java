package com.nm.fragmentsclean.aticleContext.write.adapters.secondary.gateways.repositories;

import com.nm.fragmentsclean.aticleContext.write.businesslogic.gateways.repositories.ArticlePublicationCapacityPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public final class JdbcArticlePublicationCapacityAdapter implements ArticlePublicationCapacityPort {
    private final JdbcTemplate jdbc;

    public JdbcArticlePublicationCapacityAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public int countPublishedExcluding(UUID articleId) {
        // Lock each candidate row, not the aggregate COUNT(*) result.
        return jdbc.query(
                "SELECT article_id FROM articles WHERE status = 'PUBLISHED' AND article_id <> ? FOR UPDATE",
                (rs, rowNum) -> rs.getObject("article_id", UUID.class), articleId).size();
    }
}
