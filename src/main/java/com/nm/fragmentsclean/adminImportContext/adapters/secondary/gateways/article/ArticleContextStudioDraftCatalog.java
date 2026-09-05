package com.nm.fragmentsclean.adminImportContext.adapters.secondary.gateways.article;

import com.nm.fragmentsclean.adminImportContext.businessLogic.models.*;
import com.nm.fragmentsclean.adminImportContext.businessLogic.ports.StudioArticleDraftCatalog;
import com.nm.fragmentsclean.articleContext.read.ArticleStudioDraftReader;
import com.nm.fragmentsclean.articleContext.read.ArticleStudioDraftView;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public final class ArticleContextStudioDraftCatalog implements StudioArticleDraftCatalog {
    private final ArticleStudioDraftReader reader;

    public ArticleContextStudioDraftCatalog(ArticleStudioDraftReader reader) { this.reader = reader; }

    @Override
    public List<StudioArticleDraftDocument> list() { return reader.list().stream().map(this::map).toList(); }

    @Override
    public Optional<StudioArticleDraftDocument> byId(UUID articleId) { return reader.byId(articleId).map(this::map); }

    private StudioArticleDraftDocument map(ArticleStudioDraftView source) {
        var draft = new StudioArticleSubmission(source.articleId(), source.revisionId(), source.slug(),
                source.locale(), source.authorId(), source.authorName(), source.title(), source.introduction(),
                source.sections().stream().map(section -> new StudioArticleBlock(section.heading(),
                        section.paragraph(), image(section.image()))).toList(), source.conclusion(),
                image(source.cover()), source.tags(), source.readingTimeMin(), source.coffeeIds());
        return new StudioArticleDraftDocument(source.articleId(), source.revisionId(), source.status().toLowerCase(),
                draft, source.createdAt(), source.updatedAt(), source.publishedAt());
    }

    private static StudioArticleImageRef image(ArticleStudioDraftView.Image source) {
        return source == null ? null : new StudioArticleImageRef(
                source.storageReference(), source.url(), source.width(), source.height(), source.alt());
    }
}
