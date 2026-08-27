package com.nm.fragmentsclean.aticleContext.write.businesslogic.gateways;

import com.nm.fragmentsclean.aticleContext.write.businesslogic.usecases.article.RequestArticleGenerationCommand;

/** Entry point used by non-HTTP triggers to reuse the generation command. */
public interface ArticleGenerationRequestPort {
    void request(RequestArticleGenerationCommand command);
}
