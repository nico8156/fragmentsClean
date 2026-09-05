package com.nm.fragmentsclean.articleContext.write.businesslogic.gateways.repositories;

import java.util.UUID;

/**
 * Transactional read used by the publication use case. The adapter must lock
 * the published rows while counting them so two concurrent publications
 * cannot both pass the same capacity check.
 */
public interface ArticlePublicationCapacityPort {

    int countPublishedExcluding(UUID articleId);
}
