package com.nm.fragmentsclean.articleContextTest.unit;

import com.nm.fragmentsclean.articleContext.write.businesslogic.models.ArticleAggregate;
import com.nm.fragmentsclean.articleContext.write.businesslogic.models.ArticleContent;
import com.nm.fragmentsclean.articleContext.write.businesslogic.models.ArticleDomainException;
import com.nm.fragmentsclean.articleContext.write.businesslogic.models.ArticleIntroduction;
import com.nm.fragmentsclean.articleContext.write.businesslogic.models.ArticleParagraph;
import com.nm.fragmentsclean.articleContext.write.businesslogic.models.ArticleRevision;
import com.nm.fragmentsclean.articleContext.write.businesslogic.models.ArticleRevisionStatus;
import com.nm.fragmentsclean.articleContext.write.businesslogic.models.ArticleSection;
import com.nm.fragmentsclean.articleContext.write.businesslogic.models.ArticleTitle;
import com.nm.fragmentsclean.articleContext.write.businesslogic.models.ArticleLifecycle;
import com.nm.fragmentsclean.articleContext.write.businesslogic.models.ArticleImageRef;
import com.nm.fragmentsclean.articleContext.write.businesslogic.models.ArticleRevisionDraft;
import com.nm.fragmentsclean.articleContext.write.businesslogic.models.generation.ArticleEditorialTag;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RichArticleDomainTest {

    private static final Instant NOW = Instant.parse("2026-08-27T10:00:00Z");
    private static final UUID ARTICLE_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID REVISION_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID NEXT_REVISION_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final UUID AUTHOR_ID = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");

    @Test
    void draft_is_built_from_behavioral_objects_and_published_explicitly() {
        var article = draftArticle();

        article.submitForReview(NOW.plusSeconds(60));
        article.publishWorkingRevision(NOW.plusSeconds(120));

        assertThat(article.lifecycle()).isEqualTo(ArticleLifecycle.PUBLISHED);
        assertThat(article.publishedRevisionId()).isEqualTo(REVISION_ID);
        assertThat(article.publishedRevision().status()).isEqualTo(ArticleRevisionStatus.PUBLISHED);
        assertThat(article.version()).isEqualTo(1L);
    }

    @Test
    void published_revision_is_immutable_and_editing_creates_a_new_working_revision() {
        var article = draftArticle();
        article.submitForReview(NOW.plusSeconds(60));
        article.publishWorkingRevision(NOW.plusSeconds(120));

        article.startWorkingRevision(NEXT_REVISION_ID, draft(content("Version corrigée")), NOW.plusSeconds(180));

        assertThat(article.lifecycle()).isEqualTo(ArticleLifecycle.DRAFT);
        assertThat(article.workingRevisionId()).isEqualTo(NEXT_REVISION_ID);
        assertThat(article.publishedRevisionId()).isEqualTo(REVISION_ID);
        assertThat(article.publishedRevision().content().title().value()).isEqualTo("Version initiale");
        assertThatThrownBy(() -> article.publishedRevision().replaceDraft(draft(content("Interdit")), NOW.plusSeconds(240)))
                .isInstanceOf(ArticleDomainException.class)
                .hasMessage("Une révision non brouillon est immuable.");
    }

    @Test
    void review_rejects_a_section_without_paragraph() {
        var emptySection = ArticleSection.draft("Une section vide");
        var content = ArticleContent.draft(
                ArticleTitle.from("Titre"),
                ArticleIntroduction.from("Introduction"),
                List.of(emptySection,
                        sectionWithContent("Deuxième"), sectionWithContent("Troisième")),
                ArticleParagraph.from("Conclusion"));
        var article = ArticleAggregate.draft(
                ARTICLE_ID,
                "article-test",
                "fr-FR",
                AUTHOR_ID,
                "Fragments Studio",
                ArticleRevision.draft(REVISION_ID, draft(content), NOW),
                NOW);

        assertThatThrownBy(() -> article.submitForReview(NOW.plusSeconds(60)))
                .isInstanceOf(ArticleDomainException.class)
                .hasMessage("Chaque section doit contenir au moins un paragraphe.");
        assertThat(article.lifecycle()).isEqualTo(ArticleLifecycle.DRAFT);
    }

    @Test
    void review_rejects_a_revision_without_cover_or_editorial_tag() {
        var revisionDraft = ArticleRevisionDraft.editable(content("Titre incomplet"), null, List.of());
        var article = ArticleAggregate.draft(ARTICLE_ID, "article-incomplet", "fr-FR", AUTHOR_ID,
                "Fragments Studio", ArticleRevision.draft(REVISION_ID, revisionDraft, NOW), NOW);

        assertThatThrownBy(() -> article.submitForReview(NOW.plusSeconds(60)))
                .isInstanceOf(ArticleDomainException.class)
                .hasMessage("La couverture est obligatoire avant revue.");
    }

    @Test
    void value_objects_reject_invalid_values_and_collections_are_protected() {
        assertThatThrownBy(() -> ArticleTitle.from(" "))
                .isInstanceOf(ArticleDomainException.class);
        assertThatThrownBy(() -> ArticleParagraph.from(" "))
                .isInstanceOf(ArticleDomainException.class);

        var section = ArticleSection.draft("Goûts").withParagraph(ArticleParagraph.from("Texte"));
        var content = ArticleContent.draft(
                ArticleTitle.from("Titre"),
                ArticleIntroduction.from("Intro"),
                List.of(section),
                ArticleParagraph.from("Fin"));
        assertThatThrownBy(() -> content.sections().clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }

    private ArticleAggregate draftArticle() {
        return ArticleAggregate.draft(
                ARTICLE_ID,
                "article-test",
                "fr-FR",
                AUTHOR_ID,
                "Fragments Studio",
                ArticleRevision.draft(REVISION_ID, draft(content("Version initiale")), NOW),
                NOW);
    }

    private ArticleContent content(String title) {
        var section = sectionWithContent("Comprendre");
        var second = sectionWithContent("Explorer");
        var third = sectionWithContent("Partager");
        return ArticleContent.draft(
                ArticleTitle.from(title),
                ArticleIntroduction.from("Une introduction."),
                List.of(section, second, third),
                ArticleParagraph.from("Une conclusion."));
    }

    private ArticleSection sectionWithContent(String heading) {
        return ArticleSection.draft(heading)
                .withParagraph(ArticleParagraph.from("Un paragraphe."))
                .withImage(ArticleImageRef.from("s3://articles/" + heading.toLowerCase() + ".jpg", 1200, 800, heading));
    }

    private ArticleRevisionDraft draft(ArticleContent content) {
        return ArticleRevisionDraft.editable(content,
                ArticleImageRef.from("s3://articles/cover.jpg", 1200, 800, "Couverture"),
                List.of(ArticleEditorialTag.DECOUVERTE));
    }
}
