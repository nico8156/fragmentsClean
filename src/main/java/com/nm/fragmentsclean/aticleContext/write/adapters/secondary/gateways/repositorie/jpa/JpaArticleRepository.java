package com.nm.fragmentsclean.aticleContext.write.adapters.secondary.gateways.repositorie.jpa;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nm.fragmentsclean.aticleContext.write.adapters.secondary.gateways.repositorie.jpa.entities.ArticleJpaEntity;
import com.nm.fragmentsclean.aticleContext.write.businesslogic.gateways.repositories.ArticleRepository;
import com.nm.fragmentsclean.aticleContext.write.businesslogic.models.Article;
import com.nm.fragmentsclean.aticleContext.write.businesslogic.models.ArticleStatus;


import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class JpaArticleRepository implements ArticleRepository {

    private final SpringArticleRepository springArticleRepository;
    private final ObjectMapper objectMapper;

    public JpaArticleRepository(SpringArticleRepository springArticleRepository,
                                ObjectMapper objectMapper) {
        this.springArticleRepository = springArticleRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<Article> byId(UUID articleId) {
        return springArticleRepository.findById(articleId)
                .map(this::toDomain);
    }

    @Override
    public void save(Article article) {
        springArticleRepository.save(toJpa(article));
    }

    @Override
    public List<Article> findAllPublished() {
        return springArticleRepository.findByStatus(ArticleStatus.PUBLISHED)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    // ─────────────────────────────────────────────
    // mapping JPA <-> Domaine
    // ─────────────────────────────────────────────

    private Article toDomain(ArticleJpaEntity entity) {
        var tags = fromJsonListOfString(entity.getTagsJson());
        var coffeeIdStrings = fromJsonListOfString(entity.getCoffeeIdsJson());
        var coffeeIds = coffeeIdStrings.stream()
                .map(UUID::fromString)
                .toList();

        return Article.fromSnapshot(
                new Article.ArticleSnapshot(
                        entity.getArticleId(),
                        entity.getSlug(),
                        entity.getLocale(),
                        entity.getAuthorId(),
                        entity.getAuthorName(),
                        entity.getTitle(),
                        entity.getIntro(),
                        entity.getBlocksJson(),
                        entity.getConclusion(),
                        entity.getCoverUrl(),
                        entity.getCoverWidth(),
                        entity.getCoverHeight(),
                        entity.getCoverAlt(),
                        tags,
                        entity.getReadingTimeMin(),
                        coffeeIds,
                        entity.getCreatedAt(),
                        entity.getUpdatedAt(),
                        entity.getPublishedAt(),
                        entity.getStatus(),
                        entity.getVersion()
                )
        );
    }

    private ArticleJpaEntity toJpa(Article article) {
        var snap = article.toSnapshot();

        String tagsJson = toJson(snap.tags());
        // on stocke les UUID comme String[] en JSON
        List<String> coffeeIdStrings = snap.coffeeIds() != null
                ? snap.coffeeIds().stream().map(UUID::toString).toList()
                : List.of();
        String coffeeIdsJson = toJson(coffeeIdStrings);

        return new ArticleJpaEntity(
                snap.articleId(),
                snap.slug(),
                snap.locale(),
                snap.authorId(),
                snap.authorName(),
                snap.title(),
                snap.intro(),
                snap.blocksJson(), // déjà JSON côté domaine
                snap.conclusion(),
                snap.coverUrl(),
                snap.coverWidth(),
                snap.coverHeight(),
                snap.coverAlt(),
                tagsJson,
                snap.readingTimeMin(),
                coffeeIdsJson,
                snap.createdAt(),
                snap.updatedAt(),
                snap.publishedAt(),
                snap.status(),
                snap.version()
        );
    }

    // ─────────────────────────────────────────────
    // helpers JSON
    // ─────────────────────────────────────────────

    private String toJson(Object value) {
        if (value == null) {
            return "[]";
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize JSON for Article", e);
        }
    }

    private List<String> fromJsonListOfString(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize JSON for Article", e);
        }
    }
}
