package com.nm.fragmentsclean.adminImportContextTest.unit;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nm.fragmentsclean.adminImportContext.adapters.primary.bootstrap.ArticleSeedImportRunner;
import com.nm.fragmentsclean.adminImportContext.businessLogic.models.StudioArticleCommand;
import com.nm.fragmentsclean.adminImportContext.businessLogic.ports.ArticleAuthoringPort;
import com.nm.fragmentsclean.adminImportContext.businessLogic.usecases.ImportVersionedArticleSeeds;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ArticleSeedImportRunnerTest {
  @Test
  void reads_the_versioned_resource_and_routes_every_seed_through_article_authoring() throws Exception {
    var authoring = new RecordingAuthoringPort();
    var importer = new ImportVersionedArticleSeeds(
        authoring, () -> Instant.parse("2026-09-18T10:00:00Z"));
    var runner = new ArticleSeedImportRunner(
        importer,
        new ObjectMapper().findAndRegisterModules(),
        "seed/articles.seed.json",
        "legacy-v1");

    runner.run();

    assertThat(authoring.saved).hasSize(5);
    assertThat(authoring.reviewed).hasSize(5);
    assertThat(authoring.published).hasSize(5);
    assertThat(authoring.saved)
        .extracting(StudioArticleCommand::slug)
        .contains("quest-ce-que-le-cafe-de-specialite");
    assertThat(authoring.saved)
        .flatExtracting(StudioArticleCommand::tags)
        .allMatch(tag -> List.of(
                "culture cafe", "materiel", "diy", "tuto",
                "approfondir", "fun", "decouverte", "voyage")
            .contains(tag));
  }

  private static final class RecordingAuthoringPort implements ArticleAuthoringPort {
    private final List<StudioArticleCommand> saved = new ArrayList<>();
    private final List<UUID> reviewed = new ArrayList<>();
    private final List<UUID> published = new ArrayList<>();

    @Override
    public void saveDraft(StudioArticleCommand command) {
      saved.add(command);
    }

    @Override
    public void submitForReview(UUID commandId, Instant clientAt, UUID articleId) {
      reviewed.add(articleId);
    }

    @Override
    public void publish(UUID commandId, Instant clientAt, UUID articleId, UUID revisionId) {
      published.add(articleId);
    }

    @Override
    public void archive(UUID commandId, Instant clientAt, UUID articleId) {}

    @Override
    public void withdraw(UUID commandId, Instant clientAt, UUID articleId, UUID draftRevisionId) {}

    @Override
    public void setFeaturedRank(UUID commandId, Instant clientAt, UUID articleId, Integer rank) {}
  }
}
