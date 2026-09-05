package com.nm.fragmentsclean.articleContext.write.businesslogic.gateways.repositories;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.nm.fragmentsclean.articleContext.write.businesslogic.models.ArticleContent;
import com.nm.fragmentsclean.articleContext.write.businesslogic.models.ArticleImageRef;
import com.nm.fragmentsclean.articleContext.write.businesslogic.models.generation.ArticleEditorialTag;

public interface GeneratedArticleRevisionRepository {
	void replace(
			UUID articleId,
			UUID revisionId,
			ArticleContent content,
			ArticleImageRef cover,
			List<ArticleEditorialTag> tags,
			Instant now);
}
