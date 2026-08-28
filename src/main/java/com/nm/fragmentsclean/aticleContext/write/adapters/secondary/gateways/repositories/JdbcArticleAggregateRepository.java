package com.nm.fragmentsclean.aticleContext.write.adapters.secondary.gateways.repositories;

import com.nm.fragmentsclean.aticleContext.write.businesslogic.gateways.repositories.ArticleAggregateRepository;
import com.nm.fragmentsclean.aticleContext.write.businesslogic.models.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public final class JdbcArticleAggregateRepository implements ArticleAggregateRepository {
    private final JdbcTemplate jdbc;

    public JdbcArticleAggregateRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<ArticleAggregate> byId(UUID articleId) {
        return jdbc.query("""
                SELECT article_id, slug, locale, author_id, author_name, created_at,
                       working_revision_id, published_revision_id, status, version
                FROM articles WHERE article_id = ?
                """, (rs, row) -> ArticleAggregate.reconstitute(
                rs.getObject("article_id", UUID.class), rs.getString("slug"), rs.getString("locale"),
                rs.getObject("author_id", UUID.class), rs.getString("author_name"),
                rs.getTimestamp("created_at").toInstant(), revisions(articleId),
                rs.getObject("working_revision_id", UUID.class),
                rs.getObject("published_revision_id", UUID.class),
                ArticleLifecycle.valueOf(rs.getString("status")), rs.getLong("version")), articleId)
                .stream().findFirst();
    }

    @Override
    public void save(ArticleAggregate article) {
        Instant updatedAt = article.workingRevision().updatedAt();
        int updated = jdbc.update("""
                UPDATE articles
                SET status = ?, working_revision_id = ?, published_revision_id = ?,
                    published_at = ?, updated_at = ?, version = ?
                WHERE article_id = ?
                """, article.lifecycle().name(), article.workingRevisionId(), article.publishedRevisionId(),
                timestamp(article.publishedRevisionId() == null ? null : article.publishedRevision().publishedAt()),
                Timestamp.from(updatedAt), article.version(), article.id());
        if (updated != 1) throw new IllegalStateException("Article aggregate no longer exists: " + article.id());
        for (var revision : article.revisions()) {
            jdbc.update("""
                    UPDATE article_revisions
                    SET status = ?, updated_at = ?, published_at = ?
                    WHERE article_id = ? AND revision_id = ?
                    """, revision.status().name(), Timestamp.from(revision.updatedAt()),
                    timestamp(revision.publishedAt()), article.id(), revision.revisionId());
        }
    }

    private List<ArticleRevision> revisions(UUID articleId) {
        return jdbc.query("""
                SELECT revision_id, title, introduction, conclusion, status,
                       created_at, updated_at, published_at
                FROM article_revisions WHERE article_id = ? ORDER BY revision_number
                """, (rs, row) -> ArticleRevision.reconstitute(
                rs.getObject("revision_id", UUID.class),
                ArticleContent.draft(ArticleTitle.from(rs.getString("title")),
                        ArticleIntroduction.from(rs.getString("introduction")),
                        sections(rs.getObject("revision_id", UUID.class)),
                        ArticleParagraph.from(rs.getString("conclusion"))),
                ArticleRevisionStatus.valueOf(rs.getString("status")),
                rs.getTimestamp("created_at").toInstant(), rs.getTimestamp("updated_at").toInstant(),
                instant(rs.getTimestamp("published_at"))), articleId);
    }

    private List<ArticleSection> sections(UUID revisionId) {
        return jdbc.query("""
                SELECT section_id, heading FROM article_revision_sections
                WHERE revision_id = ? ORDER BY position
                """, (rs, row) -> {
            UUID sectionId = rs.getObject("section_id", UUID.class);
            ArticleSection section = ArticleSection.draft(rs.getString("heading"));
            for (String paragraph : jdbc.query("""
                    SELECT body FROM article_revision_paragraphs
                    WHERE section_id = ? ORDER BY position
                    """, (paragraphRs, paragraphRow) -> paragraphRs.getString("body"), sectionId)) {
                section = section.withParagraph(ArticleParagraph.from(paragraph));
            }
            for (ArticleImageRef image : jdbc.query("""
                    SELECT storage_reference, width, height, alt FROM article_revision_images
                    WHERE section_id = ? ORDER BY position
                    """, (imageRs, imageRow) -> ArticleImageRef.from(imageRs.getString("storage_reference"),
                    imageRs.getInt("width"), imageRs.getInt("height"), imageRs.getString("alt")), sectionId)) {
                section = section.withImage(image);
            }
            return section;
        }, revisionId);
    }

    private static Timestamp timestamp(Instant value) { return value == null ? null : Timestamp.from(value); }
    private static Instant instant(Timestamp value) { return value == null ? null : value.toInstant(); }
}
