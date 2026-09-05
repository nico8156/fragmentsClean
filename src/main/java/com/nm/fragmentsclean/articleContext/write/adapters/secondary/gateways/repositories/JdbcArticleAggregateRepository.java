package com.nm.fragmentsclean.articleContext.write.adapters.secondary.gateways.repositories;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nm.fragmentsclean.articleContext.write.businesslogic.gateways.repositories.ArticleAggregateRepository;
import com.nm.fragmentsclean.articleContext.write.businesslogic.models.*;
import com.nm.fragmentsclean.articleContext.write.businesslogic.models.generation.ArticleEditorialTag;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JdbcArticleAggregateRepository implements ArticleAggregateRepository {
    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    public JdbcArticleAggregateRepository(JdbcTemplate jdbc, ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
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
        Integer existing = jdbc.queryForObject(
                "SELECT count(*) FROM articles WHERE article_id = ?", Integer.class, article.id());
        if (existing == null || existing == 0) {
            insertArticle(article);
            persistRevisions(article);
            return;
        }
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
        persistRevisions(article);
    }

    private List<ArticleRevision> revisions(UUID articleId) {
        return jdbc.query("""
                SELECT revision_id, title, introduction, conclusion, cover_reference,
                       cover_width, cover_height, cover_alt, status,
                       created_at, updated_at, published_at
                FROM article_revisions WHERE article_id = ? ORDER BY revision_number
                """, (rs, row) -> ArticleRevision.reconstitute(
                rs.getObject("revision_id", UUID.class),
                ArticleRevisionDraft.editable(ArticleContent.draft(ArticleTitle.from(rs.getString("title")),
                        ArticleIntroduction.from(rs.getString("introduction")),
                        sections(rs.getObject("revision_id", UUID.class)),
                        ArticleParagraph.from(rs.getString("conclusion"))),
                        image(rs.getString("cover_reference"), (Integer) rs.getObject("cover_width"),
                                (Integer) rs.getObject("cover_height"), rs.getString("cover_alt")),
                        tags(rs.getObject("revision_id", UUID.class))),
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

    private void insertArticle(ArticleAggregate article) {
        var revision = article.workingRevision();
        var draft = revision.draft();
        var cover = draft.cover();
        jdbc.update("""
                INSERT INTO articles(article_id, slug, locale, author_id, author_name, title, intro,
                    blocks_json, conclusion, cover_url, cover_width, cover_height, cover_alt,
                    tags_json, reading_time_min, coffee_ids_json, created_at, updated_at,
                    status, version, working_revision_id, published_revision_id, published_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, '[]', ?, ?, ?, ?, ?, ?, ?)
                """, article.id(), article.slug(), article.locale(), article.authorId(), article.authorName(),
                draft.content().title().value(), draft.content().introduction().value(), blocksJson(draft),
                draft.content().conclusion().value(), cover == null ? null : cover.storageReference(),
                cover == null ? null : cover.width(), cover == null ? null : cover.height(),
                cover == null ? null : cover.alt(), tagsJson(draft.tags()), readingTime(draft.content()),
                Timestamp.from(article.createdAt()), Timestamp.from(revision.updatedAt()),
                article.lifecycle().name(), article.version(), article.workingRevisionId(),
                article.publishedRevisionId(), timestamp(revision.publishedAt()));
    }

    private void persistRevisions(ArticleAggregate article) {
        int number = 1;
        for (var revision : article.revisions()) {
            var draft = revision.draft();
            var cover = draft.cover();
            jdbc.update("""
                    INSERT INTO article_revisions(revision_id, article_id, revision_number, title,
                        introduction, conclusion, cover_reference, cover_width, cover_height, cover_alt,
                        reading_time_min, status, created_at, updated_at, published_at, version)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    ON CONFLICT (revision_id) DO UPDATE SET title = EXCLUDED.title,
                        introduction = EXCLUDED.introduction, conclusion = EXCLUDED.conclusion,
                        cover_reference = EXCLUDED.cover_reference, cover_width = EXCLUDED.cover_width,
                        cover_height = EXCLUDED.cover_height, cover_alt = EXCLUDED.cover_alt,
                        reading_time_min = EXCLUDED.reading_time_min, status = EXCLUDED.status,
                        updated_at = EXCLUDED.updated_at, published_at = EXCLUDED.published_at,
                        version = EXCLUDED.version
                    """, revision.revisionId(), article.id(), number++, draft.content().title().value(),
                    draft.content().introduction().value(), draft.content().conclusion().value(),
                    cover == null ? null : cover.storageReference(), cover == null ? null : cover.width(),
                    cover == null ? null : cover.height(), cover == null ? null : cover.alt(),
                    readingTime(draft.content()), revision.status().name(), Timestamp.from(revision.createdAt()),
                    Timestamp.from(revision.updatedAt()), timestamp(revision.publishedAt()), article.version());
            jdbc.update("DELETE FROM article_revision_sections WHERE revision_id = ?", revision.revisionId());
            jdbc.update("DELETE FROM article_revision_tags WHERE revision_id = ?", revision.revisionId());
            insertSections(revision.revisionId(), draft.content());
            int tagPosition = 0;
            for (var tag : draft.tags()) {
                jdbc.update("INSERT INTO article_revision_tags(revision_id, position, tag) VALUES (?, ?, ?)",
                        revision.revisionId(), tagPosition++, tag.label());
            }
        }
    }

    private void insertSections(UUID revisionId, ArticleContent content) {
        int sectionPosition = 0;
        for (var section : content.sections()) {
            UUID sectionId = UUID.randomUUID();
            jdbc.update("INSERT INTO article_revision_sections(section_id, revision_id, position, heading) VALUES (?, ?, ?, ?)",
                    sectionId, revisionId, sectionPosition++, section.heading());
            int paragraphPosition = 0;
            for (var paragraph : section.paragraphs()) {
                jdbc.update("INSERT INTO article_revision_paragraphs(paragraph_id, section_id, position, body) VALUES (?, ?, ?, ?)",
                        UUID.randomUUID(), sectionId, paragraphPosition++, paragraph.value());
            }
            int imagePosition = 0;
            for (var image : section.images()) {
                jdbc.update("""
                        INSERT INTO article_revision_images(image_id, revision_id, section_id, position,
                            storage_reference, width, height, alt, source)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'STUDIO')
                        """, UUID.randomUUID(), revisionId, sectionId, imagePosition++,
                        image.storageReference(), image.width(), image.height(), image.alt());
            }
        }
    }

    private List<ArticleEditorialTag> tags(UUID revisionId) {
        return jdbc.query("SELECT tag FROM article_revision_tags WHERE revision_id = ? ORDER BY position",
                (rs, row) -> ArticleEditorialTag.fromProvider(rs.getString("tag")), revisionId);
    }

    private static ArticleImageRef image(String reference, Integer width, Integer height, String alt) {
        return reference == null ? null : ArticleImageRef.from(reference, width, height, alt);
    }

    private String blocksJson(ArticleRevisionDraft draft) {
        return toJson(draft.content().sections().stream().map(section -> new CompatibilityBlock(
                section.heading(), section.paragraphs().stream().map(ArticleParagraph::value)
                .reduce((left, right) -> left + "\n\n" + right).orElse(""),
                section.images().isEmpty() ? null : CompatibilityImage.from(section.images().getFirst()))).toList());
    }

    private String tagsJson(List<ArticleEditorialTag> tags) {
        return toJson(tags.stream().map(ArticleEditorialTag::label).toList());
    }

    private String toJson(Object value) {
        try { return objectMapper.writeValueAsString(value); }
        catch (JsonProcessingException error) { throw new IllegalStateException("Cannot serialize article compatibility fields", error); }
    }

    private static int readingTime(ArticleContent content) {
        String text = content.introduction().value() + " " + content.conclusion().value() + " "
                + content.sections().stream().flatMap(section -> section.paragraphs().stream())
                .map(ArticleParagraph::value).reduce("", (left, right) -> left + " " + right);
        return Math.max(1, (text.trim().split("\\s+").length + 199) / 200);
    }

    private record CompatibilityBlock(String heading, String paragraph, CompatibilityImage photo) { }
    private record CompatibilityImage(String url, int width, int height, String alt) {
        static CompatibilityImage from(ArticleImageRef image) {
            return new CompatibilityImage(image.storageReference(), image.width(), image.height(), image.alt());
        }
    }

    private static Timestamp timestamp(Instant value) { return value == null ? null : Timestamp.from(value); }
    private static Instant instant(Timestamp value) { return value == null ? null : value.toInstant(); }
}
