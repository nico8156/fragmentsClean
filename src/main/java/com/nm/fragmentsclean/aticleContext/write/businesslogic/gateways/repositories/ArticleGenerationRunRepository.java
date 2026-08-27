package com.nm.fragmentsclean.aticleContext.write.businesslogic.gateways.repositories;

import com.nm.fragmentsclean.aticleContext.write.businesslogic.processManagers.ArticleGenerationRun;
import java.util.Optional;
import java.util.UUID;

public interface ArticleGenerationRunRepository {
    Optional<ArticleGenerationRun> byId(UUID runId);
    void save(ArticleGenerationRun run);
}
