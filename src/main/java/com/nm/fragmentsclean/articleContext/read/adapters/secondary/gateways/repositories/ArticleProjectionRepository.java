package com.nm.fragmentsclean.articleContext.read.adapters.secondary.gateways.repositories;

import com.nm.fragmentsclean.articleContext.read.projections.ArticleProjectionRow;
import com.nm.fragmentsclean.articleContext.write.businesslogic.models.ArticleCreatedEvent;
import com.nm.fragmentsclean.platform.eventing.contracts.ArticleRevisionPublishedIntegrationEvent;
import com.nm.fragmentsclean.platform.eventing.contracts.ArticleArchivedIntegrationEvent;
import com.nm.fragmentsclean.platform.eventing.contracts.ArticleWithdrawnIntegrationEvent;
import com.nm.fragmentsclean.platform.eventing.contracts.ArticleFeaturedRankChangedIntegrationEvent;

public interface ArticleProjectionRepository {
	void apply(ArticleCreatedEvent event);

	void apply(ArticleRevisionPublishedIntegrationEvent event);
	void apply(ArticleArchivedIntegrationEvent event);
	void apply(ArticleWithdrawnIntegrationEvent event);
	void apply(ArticleFeaturedRankChangedIntegrationEvent event);

	long count();

	void insertSeed(ArticleProjectionRow row);
}
