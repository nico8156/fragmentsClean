package com.nm.fragmentsclean.articleContext.write.adapters.secondary.gateways.openai;

import com.nm.fragmentsclean.articleContext.write.businesslogic.gateways.ArticleGenerationProviderException;
import com.nm.fragmentsclean.articleContext.write.businesslogic.models.generation.*;

import java.util.List;

final class OpenAiArticleResponseV1Mapper {
    static final String SCHEMA_VERSION = "article-generation.v1";

    GeneratedArticleDraft map(OpenAiArticleResponseV1 dto) {
        if (dto == null || !SCHEMA_VERSION.equals(dto.schemaVersion) || dto.sections == null || dto.tags == null)
            throw new ArticleGenerationProviderException("OpenAI article schema is missing or incompatible", false);
        try {
            List<GeneratedArticleSection> sections = dto.sections.stream()
                    .map(s -> GeneratedArticleSection.from(s.heading, s.paragraph, s.visualBrief)).toList();
            List<ArticleEditorialTag> tags = dto.tags.stream().map(ArticleEditorialTag::fromProvider).toList();
            return GeneratedArticleDraft.from(dto.title, dto.introduction, dto.conclusion, dto.coverVisualBrief, sections, tags);
        } catch (RuntimeException error) {
            if (error instanceof ArticleGenerationProviderException providerError) throw providerError;
            throw new ArticleGenerationProviderException("OpenAI article response violates editorial rules", false, error);
        }
    }
}
