package com.nm.fragmentsclean.aticleContext.write.adapters.secondary.gateways.repositories;

import com.nm.fragmentsclean.aticleContext.write.businesslogic.gateways.repositories.ArticleGenerationShellRepository;
import com.nm.fragmentsclean.aticleContext.write.businesslogic.models.ArticleAggregate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.sql.Timestamp;

@Repository
public class JdbcArticleGenerationShellRepository implements ArticleGenerationShellRepository {
    private final JdbcTemplate jdbc;
    public JdbcArticleGenerationShellRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override public void save(ArticleAggregate article) {
        if (!article.awaitsGeneratedRevision()) throw new IllegalArgumentException("Expected an article awaiting generation");
        jdbc.update("""
                INSERT INTO articles (article_id,slug,locale,author_id,author_name,title,intro,blocks_json,
                    conclusion,tags_json,reading_time_min,coffee_ids_json,created_at,updated_at,status,version)
                VALUES (?,?,?,?,?,'Génération en cours','Génération en cours','[]','Génération en cours','[]',1,'[]',?,?, 'DRAFT',0)
                """, article.id(), article.slug(), article.locale(), article.authorId(), article.authorName(),
                Timestamp.from(article.createdAt()), Timestamp.from(article.createdAt()));
    }
}
