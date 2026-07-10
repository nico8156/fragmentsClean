package com.nm.fragmentsclean.adminImportContext.businessLogic.ports;

import com.nm.fragmentsclean.adminImportContext.businessLogic.models.StudioArticleCommand;

public interface ArticleAuthoringPort {
	void createArticle(StudioArticleCommand command);
}
