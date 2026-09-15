package com.nm.fragmentsclean.articleContext.write.businesslogic.gateways.repositories;

import java.util.UUID;

public interface ArticleFeaturedRankAvailabilityPort {
    boolean occupiedByAnother(UUID articleId, int rank);
}
