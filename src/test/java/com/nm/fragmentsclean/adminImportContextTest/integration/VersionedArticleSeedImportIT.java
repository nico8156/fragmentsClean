package com.nm.fragmentsclean.adminImportContextTest.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.nm.fragmentsclean.TestContainers;
import com.nm.fragmentsclean.adminImportContext.businessLogic.models.StudioArticleBlock;
import com.nm.fragmentsclean.adminImportContext.businessLogic.models.StudioArticleImageRef;
import com.nm.fragmentsclean.adminImportContext.businessLogic.models.StudioArticleSubmission;
import com.nm.fragmentsclean.adminImportContext.businessLogic.models.VersionedArticleSeed;
import com.nm.fragmentsclean.adminImportContext.businessLogic.usecases.ImportVersionedArticleSeeds;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("database")
class VersionedArticleSeedImportIT extends TestContainers {
  private static final String SEED_KEY = "integration-article-seed";
  private static final String VERSION = "integration-v1";
  private static final UUID ARTICLE_ID = stableUuid("article:" + SEED_KEY);

  @Autowired ImportVersionedArticleSeeds importer;
  @Autowired JdbcTemplate jdbc;

  @BeforeEach
  @AfterEach
  void cleanup() {
    jdbc.update("DELETE FROM outbox_events WHERE aggregate_id = ?", ARTICLE_ID.toString());
    jdbc.update("DELETE FROM command_status WHERE command_id IN (?, ?, ?)",
        commandId("save"), commandId("review"), commandId("publish"));
    jdbc.update("DELETE FROM articles_projection WHERE id = ?", ARTICLE_ID);
    jdbc.update("DELETE FROM articles WHERE article_id = ?", ARTICLE_ID);
  }

  @Test
  void explicit_import_creates_the_write_model_and_outbox_without_mutating_the_projection() {
    importer.execute(VERSION, List.of(seed()));

    assertThat(jdbc.queryForObject(
        "SELECT status FROM articles WHERE article_id = ?", String.class, ARTICLE_ID))
        .isEqualTo("PUBLISHED");
    assertThat(jdbc.queryForObject(
        "SELECT status FROM article_revisions WHERE article_id = ?", String.class, ARTICLE_ID))
        .isEqualTo("PUBLISHED");
    assertThat(jdbc.queryForObject(
        "SELECT count(*) FROM command_status WHERE command_id IN (?, ?, ?)",
        Integer.class, commandId("save"), commandId("review"), commandId("publish")))
        .isEqualTo(3);
    assertThat(jdbc.queryForObject(
        "SELECT count(*) FROM outbox_events WHERE aggregate_id = ?", Integer.class, ARTICLE_ID.toString()))
        .isEqualTo(3);
    assertThat(jdbc.queryForObject(
        "SELECT count(*) FROM articles_projection WHERE id = ?", Integer.class, ARTICLE_ID))
        .as("projection changes only after the normal integration-event consumer runs")
        .isZero();
  }

  private static VersionedArticleSeed seed() {
    var image = new StudioArticleImageRef("https://example.test/image.jpg", null, 800, 600, "Alt");
    return new VersionedArticleSeed(
        SEED_KEY,
        "published",
        new StudioArticleSubmission(
            null,
            null,
            "integration-article-seed",
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
            List.of("culture cafe"),
            3,
            List.of()));
  }

  private static UUID commandId(String action) {
    return stableUuid("article-seed:" + VERSION + ":" + SEED_KEY + ":" + action);
  }

  private static UUID stableUuid(String value) {
    return UUID.nameUUIDFromBytes(value.getBytes(StandardCharsets.UTF_8));
  }
}
