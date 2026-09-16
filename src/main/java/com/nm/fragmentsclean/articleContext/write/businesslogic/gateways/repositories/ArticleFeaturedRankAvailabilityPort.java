package com.nm.fragmentsclean.articleContext.write.businesslogic.gateways.repositories;

import java.util.UUID;

public interface ArticleFeaturedRankAvailabilityPort {
    /** Serializes competing claims until the caller's transaction finishes. */
    boolean occupiedByAnother(UUID articleId, int rank);
}
