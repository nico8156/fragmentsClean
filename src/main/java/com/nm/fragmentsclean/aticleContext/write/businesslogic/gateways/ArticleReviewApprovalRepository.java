package com.nm.fragmentsclean.aticleContext.write.businesslogic.gateways;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface ArticleReviewApprovalRepository {
	Optional<ArticleReviewApproval> findBySagaAndRevision(UUID sagaId, UUID revisionId);
	Optional<ArticleReviewApproval> findByTokenHash(String tokenHash);
	void save(ArticleReviewApproval approval);
	boolean consume(UUID approvalId, Instant consumedAt);
}
