package com.nm.fragmentsclean.adminImportContextTest.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nm.fragmentsclean.adminImportContext.businessLogic.models.StudioArticleBlock;
import com.nm.fragmentsclean.adminImportContext.businessLogic.models.StudioArticleCommand;
import com.nm.fragmentsclean.adminImportContext.businessLogic.models.StudioArticleImageRef;
import com.nm.fragmentsclean.adminImportContext.businessLogic.models.StudioArticleSubmission;
import com.nm.fragmentsclean.adminImportContext.businessLogic.models.VersionedArticleSeed;
import com.nm.fragmentsclean.adminImportContext.businessLogic.ports.ArticleAuthoringPort;
import com.nm.fragmentsclean.adminImportContext.businessLogic.usecases.ImportVersionedArticleSeeds;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ImportVersionedArticleSeedsTest {
  private static final Instant NOW = Instant.parse("2026-09-18T10:00:00Z");

  @Test
  void imports_a_published_seed_through_the_complete_article_command_lifecycle() {
    var authoring = new RecordingAuthoringPort();
    var importer = new ImportVersionedArticleSeeds(authoring, () -> NOW);

    var result = importer.execute("catalog-v1", List.of(seed("article-001", "published")));

    assertThat(result.imported()).isEqualTo(1);
    assertThat(result.published()).isEqualTo(1);
    assertThat(authoring.saved).singleElement().satisfies(command -> {
      assertThat(command.articleId()).isEqualTo(UUID.fromString("a0e2aaf5-16f0-3596-a6ef-82594ec08b12"));
      assertThat(command.clientAt()).isEqualTo(NOW);
      assertThat(command.slug()).isEqualTo("seed-article");
    });
    assertThat(authoring.reviewed).singleElement().satisfies(call ->
        assertThat(call.articleId()).isEqualTo(authoring.saved.getFirst().articleId()));
    assertThat(authoring.published).singleElement().satisfies(call -> {
      assertThat(call.articleId()).isEqualTo(authoring.saved.getFirst().articleId());
      assertThat(call.revisionId()).isEqualTo(authoring.saved.getFirst().revisionId());
    });
  }

  @Test
  void uses_stable_command_and_entity_ids_so_an_interrupted_import_can_be_replayed() {
    var first = new RecordingAuthoringPort();
    var second = new RecordingAuthoringPort();

    new ImportVersionedArticleSeeds(first, () -> NOW)
        .execute("catalog-v1", List.of(seed("article-001", "published")));
    new ImportVersionedArticleSeeds(second, () -> NOW.plusSeconds(60))
        .execute("catalog-v1", List.of(seed("article-001", "published")));

    assertThat(second.saved.getFirst().commandId()).isEqualTo(first.saved.getFirst().commandId());
    assertThat(second.saved.getFirst().articleId()).isEqualTo(first.saved.getFirst().articleId());
    assertThat(second.saved.getFirst().revisionId()).isEqualTo(first.saved.getFirst().revisionId());
    assertThat(second.reviewed.getFirst().commandId()).isEqualTo(first.reviewed.getFirst().commandId());
    assertThat(second.published.getFirst().commandId()).isEqualTo(first.published.getFirst().commandId());
  }

  @Test
  void validates_the_whole_manifest_before_dispatching_any_command() {
    var authoring = new RecordingAuthoringPort();
    var importer = new ImportVersionedArticleSeeds(authoring, () -> NOW);

    assertThatThrownBy(() -> importer.execute(
            "catalog-v1", List.of(seed("duplicate", "draft"), seed("duplicate", "published"))))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Duplicate");
    assertThat(authoring.saved).isEmpty();
  }

  private static VersionedArticleSeed seed(String key, String lifecycle) {
    var image = new StudioArticleImageRef("https://example.test/image.jpg", null, 800, 600, "Alt");
    var submission = new StudioArticleSubmission(
        null,
        null,
        "seed-article",
        "fr-FR",
        UUID.fromString("346abea5-0160-372f-9130-5bc9c2bfc51a"),
        "Fragments",
        "Titre suffisamment explicite",
        "Une introduction suffisamment complète pour être relue.",
        List.of(
            new StudioArticleBlock("Section une", "Un paragraphe suffisamment détaillé.", image),
            new StudioArticleBlock("Section deux", "Un autre paragraphe suffisamment détaillé.", image),
            new StudioArticleBlock("Section trois", "Un dernier paragraphe suffisamment détaillé.", image)),
        "Une conclusion suffisamment complète pour terminer cet article.",
        image,
        List.of("culture"),
        3,
        List.of());
    return new VersionedArticleSeed(key, lifecycle, submission);
  }

  private static final class RecordingAuthoringPort implements ArticleAuthoringPort {
    private final List<StudioArticleCommand> saved = new ArrayList<>();
    private final List<LifecycleCall> reviewed = new ArrayList<>();
    private final List<PublishCall> published = new ArrayList<>();

    @Override
    public void saveDraft(StudioArticleCommand command) {
      saved.add(command);
    }

    @Override
    public void submitForReview(UUID commandId, Instant clientAt, UUID articleId) {
      reviewed.add(new LifecycleCall(commandId, articleId));
    }

    @Override
    public void publish(
        UUID commandId, Instant clientAt, UUID articleId, UUID revisionId) {
      published.add(new PublishCall(commandId, articleId, revisionId));
    }

    @Override
    public void archive(UUID commandId, Instant clientAt, UUID articleId) {}

    @Override
    public void withdraw(
        UUID commandId, Instant clientAt, UUID articleId, UUID draftRevisionId) {}

    @Override
    public void setFeaturedRank(
        UUID commandId, Instant clientAt, UUID articleId, Integer featuredRank) {}
  }

  private record LifecycleCall(UUID commandId, UUID articleId) {}

  private record PublishCall(UUID commandId, UUID articleId, UUID revisionId) {}
}
