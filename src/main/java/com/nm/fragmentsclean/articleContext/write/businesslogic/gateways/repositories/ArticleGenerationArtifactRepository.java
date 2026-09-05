package com.nm.fragmentsclean.articleContext.write.businesslogic.gateways.repositories;
import com.nm.fragmentsclean.articleContext.write.businesslogic.models.generation.GeneratedArticleDraft;
import java.util.UUID;
public interface ArticleGenerationArtifactRepository {
    void save(UUID runId, UUID sagaId, UUID articleId, UUID revisionId, String schemaVersion, GeneratedArticleDraft draft);
}
