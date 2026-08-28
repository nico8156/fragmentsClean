package com.nm.fragmentsclean.aticleContext.write.businesslogic.gateways;

import java.time.Instant;
import java.util.UUID;

public interface ArticleReviewApprovalValidator {
    ArticleReviewApproval validate(String token, Instant now);
    boolean consume(UUID approvalId, Instant consumedAt);
}
