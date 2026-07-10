package com.nm.fragmentsclean.adminImportContext.businessLogic.ports;

import com.nm.fragmentsclean.aticleContext.write.businesslogic.usecases.article.CreateArticleCommand;

public interface ArticleAuthoringPort {
	void createArticle(CreateArticleCommand command);
}
