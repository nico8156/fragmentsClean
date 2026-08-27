package com.nm.fragmentsclean.aticleContext.write.businesslogic.gateways;

public record ArticleReviewEmail(
		String idempotencyKey,
		String recipient,
		String subject,
		String textBody,
		String htmlBody) {
}
