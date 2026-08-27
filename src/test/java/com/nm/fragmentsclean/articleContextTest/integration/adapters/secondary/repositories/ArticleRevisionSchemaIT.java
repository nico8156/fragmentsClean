package com.nm.fragmentsclean.articleContextTest.integration.adapters.secondary.repositories;

import com.nm.fragmentsclean.articleContextTest.integration.AbstractJpaIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

class ArticleRevisionSchemaIT extends AbstractJpaIntegrationTest {

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void structured_revision_schema_is_available_alongside_legacy_articles() {
        assertThat(tableExists("articles")).isTrue();
        assertThat(tableExists("article_revisions")).isTrue();
        assertThat(tableExists("article_revision_sections")).isTrue();
        assertThat(tableExists("article_revision_paragraphs")).isTrue();
        assertThat(tableExists("article_revision_images")).isTrue();
        assertThat(tableExists("article_revision_tags")).isTrue();

        assertThat(columnExists("articles", "working_revision_id")).isTrue();
        assertThat(columnExists("articles", "published_revision_id")).isTrue();
    }

    private boolean tableExists(String table) {
        return Boolean.TRUE.equals(jdbcTemplate.queryForObject(
                "SELECT to_regclass(?) IS NOT NULL",
                Boolean.class,
                table));
    }

    private boolean columnExists(String table, String column) {
        return jdbcTemplate.queryForObject("""
                SELECT EXISTS (
                    SELECT 1
                    FROM information_schema.columns
                    WHERE table_schema = current_schema()
                      AND table_name = ?
                      AND column_name = ?
                )
                """, Boolean.class, table, column);
    }
}
