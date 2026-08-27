package com.nm.fragmentsclean.aticleContext.read.adapters.secondary.gateways.repositories;

import com.nm.fragmentsclean.aticleContext.read.projections.ArticleProjectionRow;
import com.nm.fragmentsclean.aticleContext.write.businesslogic.models.ArticleCreatedEvent;
import com.nm.fragmentsclean.platform.eventing.contracts.ArticleRevisionPublishedIntegrationEvent;

public interface ArticleProjectionRepository {
	void apply(ArticleCreatedEvent event);

	void apply(ArticleRevisionPublishedIntegrationEvent event);

	long count();

	void insertSeed(ArticleProjectionRow row);
}
