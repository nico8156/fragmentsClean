package com.nm.fragmentsclean.aticleContext.read.adapters.secondary.gateways.repositories;

import com.nm.fragmentsclean.aticleContext.read.projections.ArticleProjectionRow;
import com.nm.fragmentsclean.aticleContext.write.businesslogic.models.ArticleCreatedEvent;
import com.nm.fragmentsclean.platform.eventing.contracts.ArticleRevisionPublishedIntegrationEvent;
import com.nm.fragmentsclean.platform.eventing.contracts.ArticleArchivedIntegrationEvent;

public interface ArticleProjectionRepository {
	void apply(ArticleCreatedEvent event);

	void apply(ArticleRevisionPublishedIntegrationEvent event);
	void apply(ArticleArchivedIntegrationEvent event);

	long count();

	void insertSeed(ArticleProjectionRow row);
}
