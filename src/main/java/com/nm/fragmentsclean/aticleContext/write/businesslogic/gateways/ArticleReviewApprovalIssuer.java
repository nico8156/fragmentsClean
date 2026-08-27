package com.nm.fragmentsclean.aticleContext.write.businesslogic.gateways;

import java.time.Instant;
import java.util.UUID;

public interface ArticleReviewApprovalIssuer {
	String issue(UUID sagaId, UUID articleId, UUID revisionId, Instant now);
}
