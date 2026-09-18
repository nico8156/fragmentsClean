package com.nm.fragmentsclean.adminImportContext.businessLogic.usecases;

import com.nm.fragmentsclean.adminImportContext.businessLogic.models.StudioArticleCommand;
import com.nm.fragmentsclean.adminImportContext.businessLogic.models.VersionedArticleSeed;
import com.nm.fragmentsclean.adminImportContext.businessLogic.ports.ArticleAuthoringPort;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DateTimeProvider;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/** Explicit, replay-safe import of versioned editorial fixtures through article commands. */
public final class ImportVersionedArticleSeeds {
  private final ArticleAuthoringPort authoring;
  private final DateTimeProvider clock;

  public ImportVersionedArticleSeeds(ArticleAuthoringPort authoring, DateTimeProvider clock) {
    this.authoring = authoring;
    this.clock = clock;
  }

  public ImportResult execute(String sourceVersion, List<VersionedArticleSeed> seeds) {
    String version = requireText(sourceVersion, "sourceVersion");
    List<ValidatedSeed> validated = validate(seeds);
    var now = clock.now();
    int published = 0;

    for (var seed : validated) {
      String namespace = "article-seed:" + version + ":" + seed.seedKey();
      var source = seed.submission();
      UUID articleId = stableUuid("article:" + seed.seedKey());
      UUID revisionId = stableUuid(namespace + ":revision");
      authoring.saveDraft(new StudioArticleCommand(
          stableUuid(namespace + ":save"),
          now,
          articleId,
          revisionId,
          source.slug(),
          source.locale(),
          source.authorId(),
          source.authorName(),
          source.title(),
          source.intro(),
          source.blocks(),
          source.conclusion(),
          source.cover() == null ? null : source.cover().url(),
          source.cover() == null ? null : source.cover().width(),
          source.cover() == null ? null : source.cover().height(),
          source.cover() == null ? null : source.cover().alt(),
          source.tags(),
          source.readingTimeMin(),
          source.coffeeIds()));
      if (seed.lifecycle() == Lifecycle.PUBLISHED) {
        authoring.submitForReview(stableUuid(namespace + ":review"), now, articleId);
        authoring.publish(stableUuid(namespace + ":publish"), now, articleId, revisionId);
        published++;
      }
    }
    return new ImportResult(validated.size(), published);
  }

  private static List<ValidatedSeed> validate(List<VersionedArticleSeed> seeds) {
    if (seeds == null) {
      throw new IllegalArgumentException("seeds are required");
    }
    var keys = new HashSet<String>();
    return seeds.stream()
        .map(seed -> {
          String key = requireText(seed.seedKey(), "seedKey");
          if (!keys.add(key)) {
            throw new IllegalArgumentException("Duplicate article seed key: " + key);
          }
          return new ValidatedSeed(key, lifecycle(seed.lifecycle()), seed.submission());
        })
        .toList();
  }

  private static Lifecycle lifecycle(String value) {
    try {
      return Lifecycle.valueOf(requireText(value, "lifecycle").toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException unsupported) {
      throw new IllegalArgumentException("Article seed lifecycle must be DRAFT or PUBLISHED", unsupported);
    }
  }

  private static UUID stableUuid(String value) {
    return UUID.nameUUIDFromBytes(value.getBytes(StandardCharsets.UTF_8));
  }

  private static String requireText(String value, String field) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(field + " is required");
    }
    return value.trim();
  }

  private enum Lifecycle {
    DRAFT,
    PUBLISHED
  }

  private record ValidatedSeed(
      String seedKey, Lifecycle lifecycle, com.nm.fragmentsclean.adminImportContext.businessLogic.models.StudioArticleSubmission submission) {}

  public record ImportResult(int imported, int published) {}
}
