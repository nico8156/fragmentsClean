package com.nm.fragmentsclean.articleContextTest.integration.adapters.secondary.repositories;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nm.fragmentsclean.articleContext.read.*;
import com.nm.fragmentsclean.articleContextTest.integration.AbstractJpaIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.ArrayList;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;

class ArticleLocaleReadCompatibilityIT extends AbstractJpaIntegrationTest {
    @Autowired JdbcTemplate jdbc;

    @Test
    void five_featured_articles_with_legacy_and_canonical_french_locales_are_readable_and_paginated() throws Exception {
        // Isolated transaction: fixture writes are rolled back by the test harness.
        jdbc.update("DELETE FROM articles_projection");
        for (int rank = 1; rank <= 5; rank++) seed("featured-" + rank, rank == 3 ? "fr-FR" : "fr", "published", rank);
        seed("ordinary", "fr", "published", null);
        seed("draft", "fr", "draft", null);
        seed("archived", "fr-FR", "archived", null);
        seed("english", "en", "published", null);
        var detail = new GetArticleBySlugQueryHandler(jdbc, new ObjectMapper(), reference -> reference);
        var list = new ListArticlesQueryHandler(jdbc, detail);
        var items = new ArrayList<com.nm.fragmentsclean.articleContext.read.projections.ArticleView>();
        String cursor = null;
        do {
            var page = list.handle(new ListArticlesQuery("fr-FR", 2, cursor));
            items.addAll(page.items());
            cursor = page.nextCursor();
        } while (cursor != null);
        assertThat(items).hasSize(6).allMatch(item -> item.locale().equals("fr-FR"));
        assertThat(items.stream().filter(item -> item.featuredRank() != null).map(item -> item.featuredRank()))
                .containsExactlyInAnyOrder(1, 2, 3, 4, 5);
        assertThat(items.stream().map(item -> item.id()).distinct()).hasSize(6);
        assertThat(detail.handle(new GetArticleBySlugQuery("featured-1", "fr-FR")).featuredRank()).isEqualTo(1);
        assertThat(detail.handle(new GetArticleBySlugQuery("featured-3", "fr")).locale()).isEqualTo("fr-FR");
        assertThat(detail.handle(new GetArticleBySlugQuery("ordinary", "fr-FR"))).isNotNull();
        assertThat(detail.handle(new GetArticleBySlugQuery("draft", "fr-FR"))).isNull();
        assertThat(detail.handle(new GetArticleBySlugQuery("archived", "fr-FR"))).isNull();
        assertThat(detail.handle(new GetArticleBySlugQuery("english", "fr-FR"))).isNull();
        assertThat(detail.handle(new GetArticleBySlugQuery("english", "en-US")).locale()).isEqualTo("en-US");
        var bus = new com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.QueryBus();
        bus.registerQueryHandlers(java.util.List.of(detail, list));
        var web = org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup(
                new com.nm.fragmentsclean.articleContext.read.adapters.primary.springboot.controllers.ReadArticleController(bus))
                .setMessageConverters(new org.springframework.http.converter.json.MappingJackson2HttpMessageConverter(
                        new ObjectMapper().findAndRegisterModules())).build();
        web.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/articles").param("locale", "fr-FR"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.items.length()").value(6));
        web.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/articles/featured-1").param("locale", "fr-FR"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.locale").value("fr-FR"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.featuredRank").value(1));
    }

    @Test
    void legacy_studio_draft_remains_editable_with_the_canonical_locale_then_publishes_to_mobile() {
        var mapper = new ObjectMapper();
        var repository = new com.nm.fragmentsclean.articleContext.write.adapters.secondary.gateways.repositories.JdbcArticleAggregateRepository(jdbc, mapper);
        var now = java.time.Instant.parse("2026-09-16T12:00:00Z");
        var article = com.nm.fragmentsclean.articleContext.write.businesslogic.models.ArticleAggregate.draft(
                UUID.randomUUID(), "legacy-edit", "fr", UUID.randomUUID(), "Studio",
                com.nm.fragmentsclean.articleContext.write.businesslogic.models.ArticleRevision.draft(
                        UUID.randomUUID(), JdbcArticleAggregateRepositoryIT.publishableDraft(), now), now);
        repository.save(article);
        assertThat(jdbc.queryForObject("SELECT locale FROM articles WHERE article_id = ?", String.class, article.id())).isEqualTo("fr-FR");
        // Model a historical row without mutating production data.
        jdbc.update("UPDATE articles SET locale = 'fr' WHERE article_id = ?", article.id());
        var studio = new com.nm.fragmentsclean.articleContext.read.adapters.secondary.gateways.repositories.JdbcArticleStudioDraftReader(jdbc, mapper, reference -> reference);
        assertThat(studio.byId(article.id()).orElseThrow().locale()).isEqualTo("fr-FR");
        var handler = new com.nm.fragmentsclean.articleContext.write.businesslogic.usecases.article.UpsertArticleDraftCommandHandler(
                repository, event -> {}, () -> now,
                (commandId, type, aggregateId, eventType, at) -> {});
        for (String alias : java.util.List.of("fr", "fr-FR")) {
            handler.execute(new com.nm.fragmentsclean.articleContext.write.businesslogic.usecases.article.UpsertArticleDraftCommand(
                    UUID.randomUUID(), now, article.id(), article.workingRevisionId(), article.slug(), alias,
                    article.authorId(), article.authorName(), JdbcArticleAggregateRepositoryIT.publishableDraft()));
        }
        var edited = repository.byId(article.id()).orElseThrow();
        edited.submitForReview(now);
        edited.publishWorkingRevision(now);
        repository.save(edited);
        var projection = new com.nm.fragmentsclean.articleContext.read.adapters.secondary.gateways.repositories.JdbcArticleProjectionRepository(jdbc, mapper);
        projection.apply(new com.nm.fragmentsclean.platform.eventing.contracts.ArticleRevisionPublishedIntegrationEvent(
                UUID.randomUUID(), UUID.randomUUID(), edited.id(), edited.publishedRevisionId(), edited.version(), now, now));
        var publicArticle = new GetArticleBySlugQueryHandler(jdbc, mapper, reference -> reference)
                .handle(new GetArticleBySlugQuery("legacy-edit", "fr-FR"));
        assertThat(publicArticle.locale()).isEqualTo("fr-FR");
        assertThat(publicArticle.status()).isEqualTo("published");
    }

    private void seed(String slug, String locale, String status, Integer rank) {
        jdbc.update("""
            INSERT INTO articles_projection(id,slug,locale,title,intro,blocks_json,conclusion,tags_json,
                author_id,author_name,reading_time_min,published_at,updated_at,version,status,coffee_ids_json,featured_rank)
            VALUES (?,?,?,'Article','Intro','[]','Conclusion','[]',?,'Studio',1,
                '2026-09-16T12:00:00Z','2026-09-16T12:00:00Z',1,?,'[]',?)
            """, UUID.randomUUID(), slug, locale, UUID.randomUUID(), status, rank);
    }
}
