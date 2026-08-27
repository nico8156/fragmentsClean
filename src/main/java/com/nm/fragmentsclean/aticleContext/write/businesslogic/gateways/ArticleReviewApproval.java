package com.nm.fragmentsclean.aticleContext.write.businesslogic.gateways;

import java.time.Instant;
import java.util.UUID;

public record ArticleReviewApproval(
		UUID approvalId,
		UUID sagaId,
		UUID articleId,
		UUID revisionId,
		String tokenHash,
		Instant createdAt,
		Instant expiresAt,
		Instant consumedAt) {
	public boolean isActiveAt(Instant now) {
		return consumedAt == null && expiresAt.isAfter(now);
	}
}
