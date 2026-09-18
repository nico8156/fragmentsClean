package com.nm.fragmentsclean.adminImportContext.adapters.primary.bootstrap;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nm.fragmentsclean.adminImportContext.businessLogic.models.StudioArticleBlock;
import com.nm.fragmentsclean.adminImportContext.businessLogic.models.StudioArticleImageRef;
import com.nm.fragmentsclean.adminImportContext.businessLogic.models.StudioArticleSubmission;
import com.nm.fragmentsclean.adminImportContext.businessLogic.models.VersionedArticleSeed;
import com.nm.fragmentsclean.adminImportContext.businessLogic.usecases.ImportVersionedArticleSeeds;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/** Opt-in primary adapter. Disabled unless an operator explicitly requests a versioned import. */
@Component
@ConditionalOnProperty(name = "fragments.bootstrap.article-seeds.enabled", havingValue = "true")
public final class ArticleSeedImportRunner implements CommandLineRunner {
  private static final Logger log = LoggerFactory.getLogger(ArticleSeedImportRunner.class);

  private final ImportVersionedArticleSeeds importer;
  private final ObjectMapper objectMapper;
  private final String resourceLocation;
  private final String sourceVersion;

  public ArticleSeedImportRunner(
      ImportVersionedArticleSeeds importer,
      ObjectMapper objectMapper,
      @Value("${fragments.bootstrap.article-seeds.resource:seed/articles.seed.json}")
          String resourceLocation,
      @Value("${fragments.bootstrap.article-seeds.version:legacy-v1}") String sourceVersion) {
    this.importer = importer;
    this.objectMapper = objectMapper;
    this.resourceLocation = resourceLocation;
    this.sourceVersion = sourceVersion;
  }

  @Override
  public void run(String... args) throws Exception {
    var resource = new ClassPathResource(resourceLocation);
    if (!resource.exists()) {
      throw new IllegalStateException("Article seed resource does not exist: " + resourceLocation);
    }
    final List<ArticleSeedDocument> documents;
    try (InputStream input = resource.getInputStream()) {
      documents = objectMapper.readValue(input, new TypeReference<>() {});
    }
    var result = importer.execute(sourceVersion, documents.stream().map(this::toSeed).toList());
    log.info(
        "[ARTICLE_SEED_IMPORT] completed version={} imported={} published={}",
        sourceVersion,
        result.imported(),
        result.published());
  }

  private VersionedArticleSeed toSeed(ArticleSeedDocument document) {
    var submission =
        new StudioArticleSubmission(
            null,
            null,
            document.slug(),
            document.locale(),
            stableUuid("author:" + document.author().id()),
            document.author().name(),
            document.title(),
            document.intro(),
            document.blocks().stream()
                .map(
                    block ->
                        new StudioArticleBlock(
                            block.heading(), block.paragraph(), image(block.photo())))
                .toList(),
            document.conclusion(),
            image(document.cover()),
            document.tags().stream().map(ArticleSeedImportRunner::legacyTag).toList(),
            document.readingTimeMin(),
            document.coffeeIds());
    return new VersionedArticleSeed(document.id(), document.status(), submission);
  }

  private static StudioArticleImageRef image(ImageDocument image) {
    return image == null
        ? null
        : new StudioArticleImageRef(
            image.url(), image.url(), image.width(), image.height(), image.alt());
  }

  private static UUID stableUuid(String key) {
    return UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8));
  }

  private static String legacyTag(String value) {
    String normalized = Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD)
        .replaceAll("\\p{M}", "")
        .trim()
        .toLowerCase(java.util.Locale.ROOT);
    return switch (normalized) {
      case "bien-etre", "communaute", "fun" -> "fun";
      case "nutrition", "durabilite", "qualite", "approfondir" -> "approfondir";
      case "science", "decouverte" -> "decouverte";
      case "culture", "torrefacteurs", "terroir", "culture cafe" -> "culture cafe";
      case "producteurs", "origines", "voyage" -> "voyage";
      case "maison", "diy" -> "diy";
      case "recettes", "tuto" -> "tuto";
      case "equipement", "materiel" -> "materiel";
      default -> throw new IllegalArgumentException("Unsupported legacy article tag: " + value);
    };
  }

  record ArticleSeedDocument(
      String id,
      String slug,
      String locale,
      String title,
      String intro,
      List<BlockDocument> blocks,
      String conclusion,
      ImageDocument cover,
      List<String> tags,
      AuthorDocument author,
      int readingTimeMin,
      Instant publishedAt,
      Instant updatedAt,
      long version,
      String status,
      List<UUID> coffeeIds) {}

  record BlockDocument(String heading, String paragraph, ImageDocument photo) {}

  record ImageDocument(String url, Integer width, Integer height, String alt) {}

  record AuthorDocument(String id, String name) {}
}
