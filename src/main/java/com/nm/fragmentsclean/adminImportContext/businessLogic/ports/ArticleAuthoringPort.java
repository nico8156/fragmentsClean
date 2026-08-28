package com.nm.fragmentsclean.adminImportContext.businessLogic.ports;

import com.nm.fragmentsclean.adminImportContext.businessLogic.models.StudioArticleCommand;

public interface ArticleAuthoringPort {
	void saveDraft(StudioArticleCommand command);
	void submitForReview(java.util.UUID commandId, java.time.Instant clientAt, java.util.UUID articleId);
	void publish(java.util.UUID commandId, java.time.Instant clientAt,
			java.util.UUID articleId, java.util.UUID revisionId);
	void archive(java.util.UUID commandId, java.time.Instant clientAt, java.util.UUID articleId);
}
