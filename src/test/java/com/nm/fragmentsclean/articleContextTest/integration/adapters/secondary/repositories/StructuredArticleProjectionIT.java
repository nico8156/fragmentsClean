package com.nm.fragmentsclean.articleContextTest.integration.adapters.secondary.repositories;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nm.fragmentsclean.articleContextTest.integration.AbstractJpaIntegrationTest;
import com.nm.fragmentsclean.articleContext.read.adapters.secondary.gateways.repositories.JdbcArticleProjectionRepository;
import com.nm.fragmentsclean.platform.eventing.contracts.ArticleRevisionPublishedIntegrationEvent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class StructuredArticleProjectionIT extends AbstractJpaIntegrationTest {

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void published_structured_revision_creates_the_public_mobile_projection() throws Exception {
        UUID articleId = UUID.randomUUID();
        UUID revisionId = UUID.randomUUID();
        UUID sectionId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        Instant now = Instant.parse("2026-08-28T08:00:00Z");

        jdbcTemplate.update("""
                INSERT INTO articles(article_id, slug, locale, author_id, author_name, title, intro,
                    blocks_json, conclusion, tags_json, reading_time_min, coffee_ids_json,
                    created_at, updated_at, status, version, working_revision_id)
                VALUES (?, 'choisir-son-cafe', 'fr-FR', ?, 'Jules Moreau', 'Shell', 'Shell',
                    '[]', 'Shell', '[]', 1, '[]', ?, ?, 'DRAFT', 1, ?)
                """, articleId, authorId, now, now, revisionId);
        jdbcTemplate.update("""
                INSERT INTO article_revisions(revision_id, article_id, revision_number, title,
                    introduction, conclusion, cover_reference, cover_width, cover_height, cover_alt,
                    reading_time_min, status, created_at, updated_at, version)
                VALUES (?, ?, 1, 'Découvrir et choisir son café', 'Une introduction structurée.',
                    'Une conclusion structurée.', 's3://articles/cover.jpg', 1200, 800, 'Tasses de café',
                    5, 'PUBLISHED', ?, ?, 3)
                """, revisionId, articleId, now, now);
        jdbcTemplate.update("""
                INSERT INTO article_revision_sections(section_id, revision_id, position, heading)
                VALUES (?, ?, 0, 'Comprendre ses goûts')
                """, sectionId, revisionId);
        jdbcTemplate.update("""
                INSERT INTO article_revision_paragraphs(paragraph_id, section_id, position, body)
                VALUES (?, ?, 0, 'Premier paragraphe.'), (?, ?, 1, 'Second paragraphe.')
                """, UUID.randomUUID(), sectionId, UUID.randomUUID(), sectionId);
        jdbcTemplate.update("""
                INSERT INTO article_revision_images(image_id, revision_id, section_id, position,
                    storage_reference, width, height, alt, source)
                VALUES (?, ?, ?, 0, 's3://articles/section.jpg', 1000, 700, 'Méthodes café', 'generated')
                """, UUID.randomUUID(), revisionId, sectionId);
        jdbcTemplate.update("""
                INSERT INTO article_revision_tags(revision_id, position, tag)
                VALUES (?, 0, 'Découverte'), (?, 1, 'Culture café')
                """, revisionId, revisionId);

        var repository = new JdbcArticleProjectionRepository(jdbcTemplate, new ObjectMapper());
        repository.apply(new ArticleRevisionPublishedIntegrationEvent(
                UUID.randomUUID(), UUID.randomUUID(), articleId, revisionId, 3, now, now));

        var projection = jdbcTemplate.queryForMap(
                "SELECT * FROM articles_projection WHERE id = ?", articleId);
        assertThat(projection.get("title")).isEqualTo("Découvrir et choisir son café");
        assertThat(projection.get("intro")).isEqualTo("Une introduction structurée.");
        assertThat(projection.get("status")).isEqualTo("published");
        assertThat(((Number) projection.get("version")).longValue()).isEqualTo(3L);

        ObjectMapper mapper = new ObjectMapper();
        assertThat(mapper.readTree((String) projection.get("blocks_json")))
                .hasSize(1)
                .first()
                .extracting(node -> node.get("heading").asText())
                .isEqualTo("Comprendre ses goûts");
        assertThat(mapper.readTree((String) projection.get("blocks_json")).get(0).get("paragraph").asText())
                .isEqualTo("Premier paragraphe.\n\nSecond paragraphe.");
        var tags = mapper.readTree((String) projection.get("tags_json"));
        assertThat(List.of(tags.get(0).asText(), tags.get(1).asText()))
                .containsExactly("Découverte", "Culture café");
    }
}
