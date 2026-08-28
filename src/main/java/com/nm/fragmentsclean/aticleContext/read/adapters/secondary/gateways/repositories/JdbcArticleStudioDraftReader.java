package com.nm.fragmentsclean.aticleContext.read.adapters.secondary.gateways.repositories;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nm.fragmentsclean.aticleContext.read.ArticleImageUriResolver;
import com.nm.fragmentsclean.aticleContext.read.ArticleStudioDraftReader;
import com.nm.fragmentsclean.aticleContext.read.ArticleStudioDraftView;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JdbcArticleStudioDraftReader implements ArticleStudioDraftReader {
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;
    private final ArticleImageUriResolver images;

    public JdbcArticleStudioDraftReader(JdbcTemplate jdbc, ObjectMapper mapper, ArticleImageUriResolver images) {
        this.jdbc = jdbc;
        this.mapper = mapper;
        this.images = images;
    }

    @Override
    public List<ArticleStudioDraftView> list() {
        return jdbc.query(baseQuery() + " ORDER BY a.updated_at DESC", this::map);
    }

    @Override
    public Optional<ArticleStudioDraftView> byId(UUID articleId) {
        return jdbc.query(baseQuery() + " AND a.article_id = ?", this::map, articleId).stream().findFirst();
    }

    private String baseQuery() {
        return """
                SELECT a.article_id, a.working_revision_id, a.status, a.slug, a.locale,
                       a.author_id, a.author_name, a.coffee_ids_json, a.created_at,
                       a.updated_at, a.published_at, r.title, r.introduction, r.conclusion,
                       r.cover_reference, r.cover_width, r.cover_height, r.cover_alt,
                       r.reading_time_min
                FROM articles a
                JOIN article_revisions r ON r.revision_id = a.working_revision_id
                WHERE a.working_revision_id IS NOT NULL
                """;
    }

    private ArticleStudioDraftView map(ResultSet rs, int row) throws SQLException {
        UUID revisionId = rs.getObject("working_revision_id", UUID.class);
        String coverReference = rs.getString("cover_reference");
        var cover = coverReference == null ? null : new ArticleStudioDraftView.Image(
                coverReference, images.resolve(coverReference), rs.getInt("cover_width"),
                rs.getInt("cover_height"), rs.getString("cover_alt"));
        return new ArticleStudioDraftView(rs.getObject("article_id", UUID.class), revisionId,
                rs.getString("status"), rs.getString("slug"), rs.getString("locale"),
                rs.getObject("author_id", UUID.class), rs.getString("author_name"),
                rs.getString("title"), rs.getString("introduction"), sections(revisionId),
                rs.getString("conclusion"), cover, tags(revisionId), rs.getInt("reading_time_min"),
                uuids(rs.getString("coffee_ids_json")), rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant(),
                rs.getTimestamp("published_at") == null ? null : rs.getTimestamp("published_at").toInstant());
    }

    private List<ArticleStudioDraftView.Section> sections(UUID revisionId) {
        return jdbc.query("""
                SELECT s.section_id, s.heading,
                       COALESCE(string_agg(p.body, E'\\n\\n' ORDER BY p.position), '') paragraph,
                       i.storage_reference, i.width, i.height, i.alt
                FROM article_revision_sections s
                LEFT JOIN article_revision_paragraphs p ON p.section_id = s.section_id
                LEFT JOIN article_revision_images i ON i.section_id = s.section_id AND i.position = 0
                WHERE s.revision_id = ?
                GROUP BY s.section_id, s.position, s.heading,
                         i.storage_reference, i.width, i.height, i.alt
                ORDER BY s.position
                """, (rs, row) -> {
            String reference = rs.getString("storage_reference");
            var image = reference == null ? null : new ArticleStudioDraftView.Image(reference,
                    images.resolve(reference), rs.getInt("width"), rs.getInt("height"), rs.getString("alt"));
            return new ArticleStudioDraftView.Section(
                    rs.getString("heading"), rs.getString("paragraph"), image);
        }, revisionId);
    }

    private List<String> tags(UUID revisionId) {
        return jdbc.query("SELECT tag FROM article_revision_tags WHERE revision_id = ? ORDER BY position",
                (rs, row) -> rs.getString("tag"), revisionId);
    }

    private List<UUID> uuids(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return mapper.readValue(json, new TypeReference<List<String>>() { }).stream()
                    .map(UUID::fromString).toList();
        } catch (IOException error) {
            throw new IllegalStateException("Invalid article coffee references", error);
        }
    }
}
