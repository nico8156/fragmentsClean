package com.nm.fragmentsclean.articleContextTest.integration.adapters.secondary.repositories;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nm.fragmentsclean.articleContextTest.integration.AbstractJpaIntegrationTest;
import com.nm.fragmentsclean.articleContext.write.adapters.secondary.gateways.repositories.JdbcArticleAggregateRepository;
import com.nm.fragmentsclean.articleContext.write.businesslogic.models.*;
import com.nm.fragmentsclean.articleContext.write.businesslogic.models.generation.ArticleEditorialTag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JdbcArticleAggregateRepositoryIT extends AbstractJpaIntegrationTest {

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void creates_reloads_and_edits_a_complete_structured_manual_draft() {
        var repository = new JdbcArticleAggregateRepository(jdbc, new ObjectMapper());
        UUID articleId = UUID.randomUUID();
        UUID revisionId = UUID.randomUUID();
        Instant now = Instant.parse("2026-08-28T10:00:00Z");
        var article = ArticleAggregate.draft(articleId, "manuel-cafe", "fr-FR", UUID.randomUUID(),
                "Fragments Studio", ArticleRevision.draft(revisionId, draft("Premier titre"), now), now);

        repository.save(article);
        var reloaded = repository.byId(articleId).orElseThrow();

        assertThat(reloaded.workingRevision().content().title().value()).isEqualTo("Premier titre");
        assertThat(reloaded.workingRevision().draft().cover().storageReference())
                .isEqualTo("s3://articles/cover.jpg");
        assertThat(reloaded.workingRevision().draft().tags())
                .containsExactly(ArticleEditorialTag.DECOUVERTE);

        reloaded.replaceWorkingDraft(draft("Titre corrigé"), now.plusSeconds(60));
        repository.save(reloaded);

        var edited = repository.byId(articleId).orElseThrow();
        assertThat(edited.workingRevision().content().title().value()).isEqualTo("Titre corrigé");
        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM article_revision_sections WHERE revision_id = ?",
                Integer.class, revisionId)).isEqualTo(1);
    }

    @Test
    void persists_featured_rank_and_a_withdrawn_editable_revision_without_losing_publication_history() {
        var repository = new JdbcArticleAggregateRepository(jdbc, new ObjectMapper());
        var articleId = UUID.randomUUID();
        var publishedRevisionId = UUID.randomUUID();
        var draftRevisionId = UUID.randomUUID();
        var now = Instant.parse("2026-09-15T10:00:00Z");
        var article = ArticleAggregate.draft(articleId, "manuel-featured", "fr-FR", UUID.randomUUID(),
                "Fragments Studio", ArticleRevision.draft(publishedRevisionId, publishableDraft(), now), now);
        article.submitForReview(now.plusSeconds(30));
        article.publishWorkingRevision(now.plusSeconds(60));
        repository.save(article);

        var published = repository.byId(articleId).orElseThrow();
        published.setFeaturedRank(2, now.plusSeconds(90));
        repository.save(published);
        assertThat(repository.byId(articleId).orElseThrow().featuredRank()).isEqualTo(2);

        var featured = repository.byId(articleId).orElseThrow();
        featured.withdrawToDraft(draftRevisionId, now.plusSeconds(120));
        repository.save(featured);
        var withdrawn = repository.byId(articleId).orElseThrow();
        assertThat(withdrawn.lifecycle()).isEqualTo(ArticleLifecycle.DRAFT);
        assertThat(withdrawn.featuredRank()).isNull();
        assertThat(withdrawn.workingRevisionId()).isEqualTo(draftRevisionId);
        assertThat(withdrawn.publishedRevisionId()).isEqualTo(publishedRevisionId);
        assertThat(withdrawn.publishedRevision().status()).isEqualTo(ArticleRevisionStatus.PUBLISHED);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM article_revisions WHERE article_id = ?",
                Integer.class, articleId)).isEqualTo(2);
    }

    private static ArticleRevisionDraft publishableDraft() {
        var content = ArticleContent.draft(ArticleTitle.from("Article à la une"),
                ArticleIntroduction.from("Une introduction."),
                List.of(
                        ArticleSection.draft("Comprendre").withParagraph(ArticleParagraph.from("Un texte."))
                                .withImage(ArticleImageRef.from("s3://articles/comprendre.jpg", 1000, 700, "Comprendre")),
                        ArticleSection.draft("Explorer").withParagraph(ArticleParagraph.from("Un autre texte."))
                                .withImage(ArticleImageRef.from("s3://articles/explorer.jpg", 1000, 700, "Explorer")),
                        ArticleSection.draft("Partager").withParagraph(ArticleParagraph.from("Encore un texte."))
                                .withImage(ArticleImageRef.from("s3://articles/partager.jpg", 1000, 700, "Partager"))),
                ArticleParagraph.from("Une conclusion."));
        return ArticleRevisionDraft.editable(content,
                ArticleImageRef.from("s3://articles/cover.jpg", 1200, 800, "Couverture"),
                List.of(ArticleEditorialTag.DECOUVERTE));
    }

    private static ArticleRevisionDraft draft(String title) {
        var section = ArticleSection.draft("Comprendre")
                .withParagraph(ArticleParagraph.from("Un contenu éditorial manuel."))
                .withImage(ArticleImageRef.from("s3://articles/section.jpg", 1000, 700, "Méthode café"));
        var content = ArticleContent.draft(ArticleTitle.from(title),
                ArticleIntroduction.from("Une introduction."), List.of(section),
                ArticleParagraph.from("Une conclusion."));
        return ArticleRevisionDraft.editable(content,
                ArticleImageRef.from("s3://articles/cover.jpg", 1200, 800, "Couverture"),
                List.of(ArticleEditorialTag.DECOUVERTE));
    }
}
